package com.t9dialer.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.t9dialer.app.t9.T9Name
import com.t9dialer.app.t9.T9Number
import com.t9dialer.app.t9.fold
import java.util.Locale

/** One phone number belonging to a contact. A contact with two numbers gives two entries. */
class PhoneEntry(
    val contactId: Long,
    val lookupKey: String?,
    val name: String,
    val number: String,
    val label: String,
    val photoUri: String?,
    val starred: Boolean,
    /** Job title and company from the contact, e.g. "Sales Manager, Simplo Tiles". */
    val org: String? = null,
) {
    val key: String = numberKey(number)
}

class CallEntry(val number: String, val cachedName: String?, val type: Int, val date: Long)

/** Consecutive calls with the same number, shown as one row: "Mum (3)". */
class RecentGroup(
    val key: String,
    val number: String,
    val name: String?,
    val contact: PhoneEntry?,
    val type: Int,
    val date: Long,
    val count: Int,
)

class CallStat(val count: Int, val last: Long)

class FrequentItem(val recent: RecentGroup, val count: Int)

class IndexedContact(val entry: PhoneEntry, val name: T9Name, val number: T9Number) {
    val folded: String = fold(entry.name)
}

/** The latest call log row for each number, searchable by name (if known) and number. */
class IndexedRecent(val recent: RecentGroup, val name: T9Name?, val number: T9Number) {
    val folded: String? = recent.name?.let { fold(it) }
}

class PhoneState(
    val loaded: Boolean = false,
    val hasContacts: Boolean = false,
    val hasCallLog: Boolean = false,
    /** One entry per contact, A to Z. */
    val contacts: List<PhoneEntry> = emptyList(),
    val recents: List<RecentGroup> = emptyList(),
    val contactIndex: List<IndexedContact> = emptyList(),
    val recentIndex: List<IndexedRecent> = emptyList(),
    val stats: Map<String, CallStat> = emptyMap(),
    /** Numbers you've called or been called by at least twice, most calls first. */
    val frequent: List<FrequentItem> = emptyList(),
) {
    /** Contacts starred as favourites in the phone's Contacts app, A to Z. */
    val favourites: List<PhoneEntry> get() = contacts.filter { it.starred }

    val lastDialed: String?
        get() = recents.firstOrNull { it.type == CallLog.Calls.OUTGOING_TYPE && it.number.isNotBlank() }?.number
}

/**
 * Matches numbers regardless of formatting or country code by comparing the
 * last nine digits: "+44 7700 900123" and "07700900123" give the same key.
 */
fun numberKey(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length > 9) digits.takeLast(9) else digits
}

class PhoneRepository(private val context: Context) {

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private val countryIso: String by lazy {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country
        iso.uppercase(Locale.ROOT)
    }

    fun format(number: String): String =
        if (number.isBlank()) "" else PhoneNumberUtils.formatNumber(number, countryIso) ?: number

    fun load(): PhoneState {
        val hasContacts = granted(Manifest.permission.READ_CONTACTS)
        val hasCallLog = granted(Manifest.permission.READ_CALL_LOG)
        val phones = if (hasContacts) queryPhones(queryOrganizations()) else emptyList()
        val calls = if (hasCallLog) queryCalls() else emptyList()

        val byKey = HashMap<String, PhoneEntry>()
        for (p in phones) if (p.key.isNotEmpty()) byKey.putIfAbsent(p.key, p)

        // Calls arrive newest first, so the first one seen per number is the latest.
        val stats = HashMap<String, CallStat>()
        for (call in calls) {
            val k = numberKey(call.number)
            if (k.isEmpty()) continue
            val s = stats[k]
            stats[k] = if (s == null) CallStat(1, call.date) else CallStat(s.count + 1, s.last)
        }

        val recents = groupRecents(calls, byKey)

        val nameCache = HashMap<String, T9Name>()
        fun t9(name: String) = nameCache.getOrPut(name) { T9Name(name) }
        val index = phones.map { IndexedContact(it, t9(it.name), T9Number(it.number)) }
        val recentIndex = recents
            .filter { it.key.isNotEmpty() }
            .distinctBy { it.key }
            .map { IndexedRecent(it, it.name?.let(::t9), T9Number(it.number)) }

        return PhoneState(
            loaded = true,
            hasContacts = hasContacts,
            hasCallLog = hasCallLog,
            contacts = phones.distinctBy { it.contactId },
            recents = recents,
            contactIndex = index,
            recentIndex = recentIndex,
            stats = stats,
            frequent = recentIndex
                .map { FrequentItem(it.recent, stats[it.recent.key]?.count ?: 0) }
                .filter { it.count >= 2 }
                .sortedWith(compareByDescending<FrequentItem> { it.count }.thenByDescending { it.recent.date })
                .take(MAX_FREQUENT),
        )
    }

    private fun groupRecents(calls: List<CallEntry>, byKey: Map<String, PhoneEntry>): List<RecentGroup> {
        val out = ArrayList<RecentGroup>()
        var i = 0
        while (i < calls.size && out.size < MAX_RECENTS) {
            val first = calls[i]
            val key = numberKey(first.number)
            var j = i + 1
            while (j < calls.size && key.isNotEmpty() && numberKey(calls[j].number) == key) j++
            val contact = byKey[key]
            out += RecentGroup(
                key = key,
                number = format(first.number),
                name = contact?.name ?: first.cachedName?.takeIf { it.isNotBlank() },
                contact = contact,
                type = first.type,
                date = first.date,
                count = j - i,
            )
            i = j
        }
        return out
    }

    /** Contact id to "Job title, Company" (either part may be missing). */
    private fun queryOrganizations(): Map<Long, String> {
        val out = HashMap<Long, String>()
        val projection = arrayOf(Organization.CONTACT_ID, Organization.COMPANY, Organization.TITLE)
        try {
            context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI, projection,
                "${ContactsContract.Data.MIMETYPE} = ?", arrayOf(Organization.CONTENT_ITEM_TYPE), null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    if (out.containsKey(id)) continue
                    val text = listOf(c.getString(2), c.getString(1))
                        .mapNotNull { it?.trim()?.takeIf { t -> t.isNotEmpty() } }
                        .joinToString(", ")
                    if (text.isNotEmpty()) out[id] = text
                }
            }
        } catch (_: SecurityException) {
        }
        return out
    }

    private fun queryPhones(orgs: Map<Long, String>): List<PhoneEntry> {
        val projection = arrayOf(
            Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER,
            Phone.TYPE, Phone.LABEL, Phone.PHOTO_THUMBNAIL_URI, Phone.STARRED,
        )
        val out = ArrayList<PhoneEntry>()
        val seen = HashSet<String>()
        try {
            context.contentResolver.query(
                Phone.CONTENT_URI, projection, null, null,
                "${Phone.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val number = c.getString(3)?.trim().orEmpty()
                    if (number.isEmpty()) continue
                    if (!seen.add("$id:${numberKey(number)}")) continue
                    val name = c.getString(2)?.takeIf { it.isNotBlank() } ?: number
                    val label = Phone.getTypeLabel(context.resources, c.getInt(4), c.getString(5)).toString()
                    out += PhoneEntry(id, c.getString(1), name, number, label, c.getString(6), c.getInt(7) == 1, orgs[id])
                }
            }
        } catch (_: SecurityException) {
        }
        return out
    }

    private fun queryCalls(): List<CallEntry> {
        val projection = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE,
        )
        val out = ArrayList<CallEntry>()
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                while (c.moveToNext() && out.size < MAX_CALLS) {
                    out += CallEntry(c.getString(0).orEmpty(), c.getString(1), c.getInt(2), c.getLong(3))
                }
            }
        } catch (_: SecurityException) {
        }
        return out
    }

    private companion object {
        const val MAX_CALLS = 1000
        const val MAX_RECENTS = 150
        const val MAX_FREQUENT = 30
    }
}
