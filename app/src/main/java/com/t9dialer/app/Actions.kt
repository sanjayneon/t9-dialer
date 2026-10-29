package com.t9dialer.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat

object Actions {

    /** Places the call directly if phone permission is granted, otherwise opens the system dialer pre-filled. */
    fun call(context: Context, number: String) {
        if (number.isBlank()) return
        val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val action = if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
        start(context, Intent(action, Uri.fromParts("tel", number, null)))
    }

    fun voicemail(context: Context) {
        val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        if (!canCall) {
            Toast.makeText(context, "Allow Phone permission to call voicemail", Toast.LENGTH_SHORT).show()
            return
        }
        start(context, Intent(Intent.ACTION_CALL, Uri.fromParts("voicemail", "", null)))
    }

    /** Opens the lookup page from settings with the number filled in. */
    fun lookUp(context: Context, number: String, urlTemplate: String) {
        val template = urlTemplate.ifBlank { com.t9dialer.app.data.Prefs.DEFAULT_LOOKUP_URL }
        val url = if ("{number}" in template) template.replace("{number}", Uri.encode(number))
                  else template + Uri.encode(number)
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun message(context: Context, number: String) =
        start(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))

    fun addContact(context: Context, number: String) =
        start(context, Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
        })

    fun openContact(context: Context, contactId: Long, lookupKey: String?) {
        val uri = if (lookupKey != null) ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
                  else ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    fun copy(context: Context, number: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Phone number", number))
        // Android 13+ shows its own confirmation
        if (Build.VERSION.SDK_INT < 33) Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
    }

    fun openAppSettings(context: Context) = start(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app on this phone can do that", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "Permission needed to do that", Toast.LENGTH_SHORT).show()
        }
    }
}
