package com.t9dialer.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeedDial(val slot: Int, val name: String, val number: String)

data class Prefs(
    /** Added in front of every number you call (left off numbers starting with + and codes like *123#). */
    val dialPrefix: String = "",
    /** Added in front of the number when you long-press the call button, e.g. +91. */
    val longPressPrefix: String = "",
    /** Web page for looking up an unknown number; {number} is replaced with it. */
    val lookupUrl: String = DEFAULT_LOOKUP_URL,
    val swipeActions: Boolean = true,
    val cyrillicKeys: Boolean = false,
    val speedDials: Map<Int, SpeedDial> = emptyMap(),
) {
    fun speedDialFor(number: String): SpeedDial? {
        val key = numberKey(number)
        return speedDials.values.firstOrNull { numberKey(it.number) == key }
    }

    companion object {
        const val DEFAULT_LOOKUP_URL = "https://www.google.com/search?q={number}"
    }
}

class AppPrefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _prefs = MutableStateFlow(read())
    val prefs: StateFlow<Prefs> = _prefs.asStateFlow()

    fun update(change: (Prefs) -> Prefs) {
        val next = change(_prefs.value)
        write(next)
        _prefs.value = next
    }

    private fun read(): Prefs {
        val dials = HashMap<Int, SpeedDial>()
        for ((k, v) in sp.all) {
            if (!k.startsWith(SPEED_PREFIX) || v !is String) continue
            val slot = k.removePrefix(SPEED_PREFIX).toIntOrNull() ?: continue
            val parts = v.split('\n', limit = 2)
            if (parts.size == 2) dials[slot] = SpeedDial(slot, parts[0], parts[1])
        }
        return Prefs(
            dialPrefix = sp.getString("dial_prefix", "").orEmpty(),
            longPressPrefix = sp.getString("long_press_prefix", "").orEmpty(),
            lookupUrl = sp.getString("lookup_url", null) ?: Prefs.DEFAULT_LOOKUP_URL,
            swipeActions = sp.getBoolean("swipe_actions", true),
            cyrillicKeys = sp.getBoolean("cyrillic_keys", false),
            speedDials = dials,
        )
    }

    private fun write(p: Prefs) {
        sp.edit {
            putString("dial_prefix", p.dialPrefix)
            putString("long_press_prefix", p.longPressPrefix)
            putString("lookup_url", p.lookupUrl)
            putBoolean("swipe_actions", p.swipeActions)
            putBoolean("cyrillic_keys", p.cyrillicKeys)
            for (k in sp.all.keys) if (k.startsWith(SPEED_PREFIX)) remove(k)
            for (d in p.speedDials.values) putString("$SPEED_PREFIX${d.slot}", "${d.name}\n${d.number}")
        }
    }

    private companion object {
        const val SPEED_PREFIX = "speed_"
    }
}

/** The number actually dialled for a normal tap. */
fun withDialPrefix(number: String, prefix: String): String {
    val n = number.trim()
    if (prefix.isBlank() || n.startsWith("+") || n.startsWith(prefix) || n.any { it == '*' || it == '#' }) return n
    return prefix + n
}

/** The number dialled when the call button is long-pressed: +91 and 098100 83013 give +9198100 83013. */
fun withLongPressPrefix(number: String, prefix: String): String {
    val n = number.trim()
    if (prefix.isBlank() || n.startsWith(prefix)) return n
    return if (prefix.startsWith("+") && n.startsWith("0")) prefix + n.drop(1) else prefix + n
}
