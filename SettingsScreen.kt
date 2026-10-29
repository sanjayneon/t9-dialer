package com.t9dialer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t9dialer.app.data.Prefs
import com.t9dialer.app.data.ThemeMode
import com.t9dialer.app.data.HomeView
import com.t9dialer.app.data.DialpadSize
import com.t9dialer.app.data.DEFAULT_FONT_SIZE
import com.t9dialer.app.data.FONT_SIZE_RANGE
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import com.t9dialer.app.data.VIBRATE_DURATIONS
import com.t9dialer.app.data.vibrateLabel
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

private enum class TextSetting(val title: String, val help: String) {
    DialPrefix("Prefix for every call", "Added in front of each number you call. Left off numbers that start with + and codes like *123#."),
    LongPressPrefix("Prefix when you long-press call", "For example +91. If it starts with +, a leading 0 in the number is dropped."),
    Lookup("Look up unknown numbers", "Web address to open from an unknown number's menu. {number} is replaced with the number."),
}

private val SHORTCUTS = listOf(
    "Swipe the keypad down to hide it, up to show it.",
    "Swipe a row right to call, left to send a message.",
    "Tap a search result to open the contact's details.",
    "Long-press a row for more: speed dial, copy, edit before calling.",
    "Long-press 1 for voicemail, 0 for +.",
    "Long-press 2 to 9 to call a speed dial. For 10 to 99, tap the first digit, then long-press the second.",
    "Long-press * to switch keypad letters between English and Russian.",
    "Long-press # to open settings.",
    "Type * in a search to stand for any letters: 5*3 finds J... D...",
    "Long-press call to dial with your long-press prefix.",
)

@Composable
fun SettingsScreen(vm: DialerViewModel, onBack: () -> Unit) {
    val c = LocalDialerColors.current
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<TextSetting?>(null) }
    var choosingTheme by rememberSaveable { mutableStateOf(false) }
    var choosingView by rememberSaveable { mutableStateOf(false) }
    var choosingSize by rememberSaveable { mutableStateOf(false) }
    var choosingFont by rememberSaveable { mutableStateOf(false) }
    var choosingDuration by rememberSaveable { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val context = LocalContext.current
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "Unknown"
    }

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.ink) }
            Text("Settings", color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = navBottom + 24.dp)) {
            item { SectionHeader("General") }
            item { ValueRow("Default view", prefs.defaultView.label) { choosingView = true } }

            item { SectionHeader("Appearance") }
            item { ValueRow("Theme", prefs.theme.label) { choosingTheme = true } }
            item { ValueRow("Dialpad size", prefs.dialpadSize.label) { choosingSize = true } }
            item {
                ValueRow(
                    "Font size",
                    if (prefs.listFontSize == DEFAULT_FONT_SIZE) "$DEFAULT_FONT_SIZE (default)" else "${prefs.listFontSize} (default $DEFAULT_FONT_SIZE)",
                ) { choosingFont = true }
            }
            item {
                CheckboxRow(
                    "Multiline mode",
                    if (prefs.multilineNames) "Long names wrap onto a second line" else "Long names are cut off with \u2026",
                    prefs.multilineNames,
                ) { on -> vm.updatePrefs { it.copy(multilineNames = on) } }
            }
            item {
                CheckboxRow(
                    "Show company and job",
                    if (prefs.showOrg) "Job title and company show under each name" else "Only names and numbers are shown",
                    prefs.showOrg,
                ) { on -> vm.updatePrefs { it.copy(showOrg = on) } }
            }

            item { SectionHeader("Calling") }
            item { ValueRow(TextSetting.DialPrefix.title, prefs.dialPrefix.ifBlank { "None" }) { editing = TextSetting.DialPrefix } }
            item { ValueRow(TextSetting.LongPressPrefix.title, prefs.longPressPrefix.ifBlank { "None" }) { editing = TextSetting.LongPressPrefix } }
            item { ValueRow(TextSetting.Lookup.title, prefs.lookupUrl) { editing = TextSetting.Lookup } }

            item { SectionHeader("Search and keypad") }
            item {
                CheckboxRow(
                    "Vibrate on tap",
                    if (prefs.vibrateOnTap) "Keys vibrate when you press them" else "Silent: no vibration",
                    prefs.vibrateOnTap,
                ) { on -> vm.updatePrefs { it.copy(vibrateOnTap = on) } }
            }
            item {
                ValueRow(
                    "Vibrate duration",
                    if (prefs.vibrateOnTap) vibrateLabel(prefs.vibrateMs) else "Tick Vibrate on tap to choose",
                    enabled = prefs.vibrateOnTap,
                ) { choosingDuration = true }
            }
            item {
                SwitchRow(
                    "Tap a search result to open the contact",
                    if (prefs.tapOpensContact) "Swipe right or use the long-press menu to call" else "Off: tapping a result calls straight away",
                    prefs.tapOpensContact,
                ) { on -> vm.updatePrefs { it.copy(tapOpensContact = on) } }
            }
            item {
                SwitchRow("Swipe rows to call or message", "Right to call, left to message", prefs.swipeActions) { on ->
                    vm.updatePrefs { it.copy(swipeActions = on) }
                }
            }
            item {
                SwitchRow("Russian letters on the keys", "Search finds English and Russian names either way", prefs.cyrillicKeys) { on ->
                    vm.updatePrefs { it.copy(cyrillicKeys = on) }
                }
            }

            item { SectionHeader("Speed dial") }
            val dials = prefs.speedDials.values.sortedBy { it.slot }
            if (dials.isEmpty()) {
                item { Note("Nothing set yet. Long-press a contact and choose Set speed dial.") }
            }
            items(dials, key = { "sd${it.slot}" }) { d ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${d.slot}", color = c.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, color = c.ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(d.number, color = c.inkMuted, fontSize = 14.sp, maxLines = 1)
                    }
                    IconButton(onClick = { vm.removeSpeedDial(d.slot) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${d.name} from speed dial", tint = c.inkMuted)
                    }
                }
            }

            item { SectionHeader("About") }
            item { ValueRow("Version", appVersion) {} }

            item { SectionHeader("Shortcuts") }
            items(SHORTCUTS) { Note(it) }
        }
    }

    if (choosingDuration) {
        DurationDialog(
            current = prefs.vibrateMs,
            onPick = { ms -> vm.updatePrefs { it.copy(vibrateMs = ms) } },
            onDismiss = { choosingDuration = false },
        )
    }

    if (choosingFont) {
        FontSizeDialog(
            current = prefs.listFontSize,
            onSave = { size -> vm.updatePrefs { it.copy(listFontSize = size) }; choosingFont = false },
            onDismiss = { choosingFont = false },
        )
    }

    if (choosingSize) {
        SizeDialog(
            current = prefs.dialpadSize,
            onPick = { size -> vm.updatePrefs { it.copy(dialpadSize = size) }; choosingSize = false },
            onDismiss = { choosingSize = false },
        )
    }

    if (choosingView) {
        ViewDialog(
            current = prefs.defaultView,
            onPick = { v -> vm.updatePrefs { it.copy(defaultView = v) }; choosingView = false },
            onDismiss = { choosingView = false },
        )
    }

    if (choosingTheme) {
        ThemeDialog(
            current = prefs.theme,
            onPick = { mode -> vm.updatePrefs { it.copy(theme = mode) }; choosingTheme = false },
            onDismiss = { choosingTheme = false },
        )
    }

    editing?.let { setting ->
        TextSettingDialog(setting, prefs, onSave = { value ->
            vm.updatePrefs {
                when (setting) {
                    TextSetting.DialPrefix -> it.copy(dialPrefix = value.trim())
                    TextSetting.LongPressPrefix -> it.copy(longPressPrefix = value.trim())
                    TextSetting.Lookup -> it.copy(lookupUrl = value.trim().ifBlank { Prefs.DEFAULT_LOOKUP_URL })
                }
            }
            editing = null
        }, onDismiss = { editing = null })
    }
}

/** Type a size or drag the slider; the default is always shown, with a button to go back to it. */
@Composable
private fun FontSizeDialog(current: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    var size by remember { mutableIntStateOf(current) }
    var input by remember { mutableStateOf(current.toString()) }
    val typed = input.toIntOrNull()
    val valid = typed != null && typed in FONT_SIZE_RANGE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font size") },
        text = {
            Column {
                Text("Default size: $DEFAULT_FONT_SIZE", color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Text size for names in Call log, Contacts and search results. Numbers and details grow or shrink with it.",
                    color = c.inkMuted, fontSize = 13.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                // Live preview at the chosen size
                Surface(color = c.bg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("Shubham Mittal", color = c.ink, fontSize = size.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("+91 99 58 959820", color = c.inkMuted, fontSize = (size * 14f / DEFAULT_FONT_SIZE).sp, maxLines = 1)
                    }
                }
                Slider(
                    value = size.toFloat(),
                    onValueChange = { v -> size = v.roundToInt(); input = size.toString() },
                    valueRange = FONT_SIZE_RANGE.first.toFloat()..FONT_SIZE_RANGE.last.toFloat(),
                    steps = FONT_SIZE_RANGE.last - FONT_SIZE_RANGE.first - 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { v ->
                            input = v.filter { it.isDigit() }.take(2)
                            input.toIntOrNull()?.takeIf { it in FONT_SIZE_RANGE }?.let { size = it }
                        },
                        label = { Text("Size") },
                        singleLine = true,
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(110.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(
                        enabled = size != DEFAULT_FONT_SIZE,
                        onClick = { size = DEFAULT_FONT_SIZE; input = DEFAULT_FONT_SIZE.toString() },
                    ) { Text("Use default ($DEFAULT_FONT_SIZE)") }
                }
                if (!valid) {
                    Text(
                        "Enter a size from ${FONT_SIZE_RANGE.first} to ${FONT_SIZE_RANGE.last}.",
                        color = c.missed, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(size) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SizeDialog(current: DialpadSize, onPick: (DialpadSize) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dialpad size") },
        text = {
            Column(Modifier.selectableGroup()) {
                for (size in DialpadSize.entries) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(selected = size == current, role = Role.RadioButton, onClick = { onPick(size) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = size == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        // Each option is written at roughly the size its digits will be
                        Text("5", color = c.ink, fontSize = 22.sp * size.scale, modifier = Modifier.width(30.dp))
                        Text(size.label, color = c.ink, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ViewDialog(current: HomeView, onPick: (HomeView) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default view") },
        text = {
            Column(Modifier.selectableGroup()) {
                Text("What the app shows when it opens.", color = c.inkMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                for (v in HomeView.entries) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(selected = v == current, role = Role.RadioButton, onClick = { onPick(v) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = v == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(v.label, color = c.ink, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ThemeDialog(current: ThemeMode, onPick: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column(Modifier.selectableGroup()) {
                for (mode in ThemeMode.entries) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(selected = mode == current, role = Role.RadioButton, onClick = { onPick(mode) }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(mode.label, color = c.ink, fontSize = 16.sp)
                            if (mode == ThemeMode.SYSTEM) {
                                Text("Follows your phone's dark mode", color = c.inkMuted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** Picking an option vibrates once at that length so you can feel it; the dialog stays open to compare. */
@Composable
private fun DurationDialog(current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    val context = LocalContext.current
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vibrate duration") },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                Text("Tap an option to feel it.", color = c.inkMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                for (ms in VIBRATE_DURATIONS) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(selected = ms == current, role = Role.RadioButton, onClick = {
                                onPick(ms)
                                if (ms <= 0) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                else vibrateFor(context, ms)
                            }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = ms == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(vibrateLabel(ms), color = c.ink, fontSize = 16.sp)
                        if (ms <= 0) Text("  phone's own light tick", color = c.inkMuted, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun ValueRow(title: String, value: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalDialerColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(title, color = if (enabled) c.ink else c.inkMuted.copy(alpha = 0.6f), fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = c.inkMuted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CheckboxRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(summary, color = c.inkMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(summary, color = c.inkMuted, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        color = LocalDialerColors.current.inkMuted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun TextSettingDialog(setting: TextSetting, prefs: Prefs, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    val initial = when (setting) {
        TextSetting.DialPrefix -> prefs.dialPrefix
        TextSetting.LongPressPrefix -> prefs.longPressPrefix
        TextSetting.Lookup -> prefs.lookupUrl
    }
    var value by remember(setting) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(setting.title) },
        text = {
            Column {
                OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                Text(setting.help, color = c.inkMuted, fontSize = 14.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
