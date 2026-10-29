package com.t9dialer.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Choices offered under Vibrate duration. 0 means the phone's own tick. */
val VIBRATE_DURATIONS = listOf(0, 10, 20, 30, 40, 50, 75, 100, 150, 200)

fun vibrateLabel(ms: Int) = if (ms <= 0) "System default" else "$ms ms"

class SpeedDial(val slot: Int, val name: String, val number: String)

/** What the app shows when it opens, before you type anything. */
enum class HomeView(val label: String) {
    CALL_LOG("Call log"),
    CONTACTS("Contacts"),
    FAVOURITES("Favourites"),
    FREQUENT("Frequently called"),
}

const val DEFAULT_FONT_SIZE = 16
val FONT_SIZE_RANGE = 12..28

/** How big the keypad's keys are drawn. */
enum class DialpadSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    NORMAL("Normal", 1f),
    LARGE("Large", 1.15f),
    HUGE("Huge", 1.3f),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

data class Prefs(
    /** Added in front of every number you call (left off numbers starting with + and codes like *123#). */
    val dialPrefix: String = "",
    /** Added in front of the number when you long-press the call button, e.g. +91. */
    val longPressPrefix: String = "",
    /** Web page for looking up an unknown number; {number} is replaced with it. */
    val lookupUrl: String = DEFAULT_LOOKUP_URL,
    val swipeActions: Boolean = true,
    /** Vibrate when keys are pressed. Off means no vibration anywhere in the app. */
    val vibrateOnTap: Boolean = true,
    /** Key vibration length in milliseconds; 0 uses the phone's own short tick. */
    val vibrateMs: Int = 0,
    /** Tapping a search result opens the contact's details; off means it calls straight away. */
    val tapOpensContact: Boolean = true,
    val cyrillicKeys: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Let long names wrap onto a second line instead of being cut off with "…". */
    val multilineNames: Boolean = false,
    /** Show job title and company under names in lists. */
    val showOrg: Boolean = false,
    val dialpadSize: DialpadSize = DialpadSize.NORMAL,
    /** Size of names in lists and search results, in sp; numbers and details scale with it. */
    val listFontSize: Int = DEFAULT_FONT_SIZE,
    val defaultView: HomeView = HomeView.CALL_LOG,
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
            vibrateOnTap = sp.getBoolean("vibrate_on_tap", true),
            vibrateMs = sp.getInt("vibrate_ms", 0),
            tapOpensContact = sp.getBoolean("tap_opens_contact", true),
            cyrillicKeys = sp.getBoolean("cyrillic_keys", false),
            theme = ThemeMode.entries.firstOrNull { it.name == sp.getString("theme", null) } ?: ThemeMode.SYSTEM,
            multilineNames = sp.getBoolean("multiline_names", false),
            showOrg = sp.getBoolean("show_org", false),
            listFontSize = sp.getInt("list_font_size", DEFAULT_FONT_SIZE).coerceIn(FONT_SIZE_RANGE),
            dialpadSize = DialpadSize.entries.firstOrNull { it.name == sp.getString("dialpad_size", null) } ?: DialpadSize.NORMAL,
            defaultView = HomeView.entries.firstOrNull { it.name == sp.getString("default_view", null) } ?: HomeView.CALL_LOG,
            speedDials = dials,
        )
    }

    private fun write(p: Prefs) {
        sp.edit {
            putString("dial_prefix", p.dialPrefix)
            putString("long_press_prefix", p.longPressPrefix)
            putString("lookup_url", p.lookupUrl)
            putBoolean("swipe_actions", p.swipeActions)
            putBoolean("vibrate_on_tap", p.vibrateOnTap)
            putInt("vibrate_ms", p.vibrateMs)
            putBoolean("tap_opens_contact", p.tapOpensContact)
            putBoolean("cyrillic_keys", p.cyrillicKeys)
            putString("theme", p.theme.name)
            putString("default_view", p.defaultView.name)
            putBoolean("multiline_names", p.multilineNames)
            putBoolean("show_org", p.showOrg)
            putString("dialpad_size", p.dialpadSize.name)
            putInt("list_font_size", p.listFontSize)
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
