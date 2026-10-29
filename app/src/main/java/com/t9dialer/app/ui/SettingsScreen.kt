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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t9dialer.app.data.Prefs

private enum class TextSetting(val title: String, val help: String) {
    DialPrefix("Prefix for every call", "Added in front of each number you call. Left off numbers that start with + and codes like *123#."),
    LongPressPrefix("Prefix when you long-press call", "For example +91. If it starts with +, a leading 0 in the number is dropped."),
    Lookup("Look up unknown numbers", "Web address to open from an unknown number's menu. {number} is replaced with the number."),
}

private val SHORTCUTS = listOf(
    "Swipe the keypad down to hide it, up to show it.",
    "Swipe a row right to call, left to send a message.",
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
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.ink) }
            Text("Settings", color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(contentPadding = PaddingValues(bottom = navBottom + 24.dp)) {
            item { SectionHeader("Calling") }
            item { ValueRow(TextSetting.DialPrefix.title, prefs.dialPrefix.ifBlank { "None" }) { editing = TextSetting.DialPrefix } }
            item { ValueRow(TextSetting.LongPressPrefix.title, prefs.longPressPrefix.ifBlank { "None" }) { editing = TextSetting.LongPressPrefix } }
            item { ValueRow(TextSetting.Lookup.title, prefs.lookupUrl) { editing = TextSetting.Lookup } }

            item { SectionHeader("Search and keypad") }
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

            item { SectionHeader("Shortcuts") }
            items(SHORTCUTS) { Note(it) }
        }
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

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    val c = LocalDialerColors.current
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, color = c.ink, fontSize = 16.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = c.inkMuted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
