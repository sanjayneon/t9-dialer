package com.t9dialer.app.ui

import android.Manifest
import android.provider.CallLog
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.t9dialer.app.data.HomeView
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.t9dialer.app.data.DEFAULT_FONT_SIZE
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.t9dialer.app.Actions
import com.t9dialer.app.data.PhoneEntry
import com.t9dialer.app.data.Prefs
import com.t9dialer.app.data.RecentGroup
import com.t9dialer.app.data.numberKey
import com.t9dialer.app.data.withDialPrefix
import com.t9dialer.app.data.withLongPressPrefix

/** What the long-press sheet and speed-dial dialog act on. */
private class SheetTarget(val title: String, val number: String, val contactId: Long?, val lookupKey: String?)

private val PERMISSIONS = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE,
)

@Composable
fun DialerApp(vm: DialerViewModel) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    // With Vibrate on tap unticked, nothing in the app vibrates: key taps, long-presses, swipes
    val haptics = if (prefs.vibrateOnTap) LocalHapticFeedback.current else NoHaptics
    CompositionLocalProvider(
        LocalHapticFeedback provides haptics,
        LocalMultilineNames provides prefs.multilineNames,
        LocalShowOrg provides prefs.showOrg,
    ) {
        if (showSettings) SettingsScreen(vm, onBack = { showSettings = false })
        else MainScreen(vm, openSettings = { showSettings = true })
    }
}

@Composable
private fun MainScreen(vm: DialerViewModel, openSettings: () -> Unit) {
    val context = LocalContext.current
    val c = LocalDialerColors.current
    val query by vm.query.collectAsStateWithLifecycle()
    val text by vm.text.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val feedback = rememberKeyFeedback(prefs.vibrateOnTap, prefs.vibrateMs)

    var dialpadOpen by rememberSaveable { mutableStateOf(true) }
    // Every launch starts on the view chosen in Settings > General
    var view by rememberSaveable { mutableStateOf(prefs.defaultView) }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    var speedDialFor by remember { mutableStateOf<SheetTarget?>(null) }
    var askedForAccess by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val textMode = text != null
    val searching = if (textMode) !text.isNullOrBlank() else query.isNotEmpty()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refresh() }

    val requestAccess: () -> Unit = {
        if (askedForAccess) Actions.openAppSettings(context)
        else { askedForAccess = true; permissionLauncher.launch(PERMISSIONS) }
    }

    // Ask once on first launch; after that the card offers a way to settings.
    LaunchedEffect(state.loaded) {
        if (state.loaded && !state.hasContacts && !askedForAccess) requestAccess()
    }

    LaunchedEffect(query, text) {
        listState.scrollToItem(0)
        if (query.isNotEmpty() && !textMode) dialpadOpen = true
    }

    BackHandler(enabled = !textMode && !dialpadOpen) { dialpadOpen = true }
    BackHandler(enabled = !textMode && dialpadOpen && query.isNotEmpty()) { vm.clear() }
    BackHandler(enabled = textMode) { vm.closeTextSearch() }

    // Dragging the results list up tucks the keypad away so you can see more.
    val collapseOnDrag = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < -6f) dialpadOpen = false
                return Offset.Zero
            }
        }
    }

    val call: (String) -> Unit = { number -> Actions.call(context, withDialPrefix(number, prefs.dialPrefix)) }
    val message: (String) -> Unit = { number -> Actions.message(context, number) }
    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    fun speedDial(slot: Int) {
        val d = prefs.speedDials[slot]
        if (d == null) {
            toast("Nothing on speed dial $slot. Long-press a contact to set it.")
        } else {
            vm.clear()
            call(d.number)
        }
    }

    val onLongKey: (Char) -> Unit = { k ->
        when {
            k == '0' -> { feedback(k); vm.press('+') }
            k == '*' -> {
                val next = !prefs.cyrillicKeys
                vm.updatePrefs { it.copy(cyrillicKeys = next) }
                toast(if (next) "Keypad letters: Русский" else "Keypad letters: English")
            }
            k == '#' -> openSettings()
            k == '1' && query.isEmpty() -> Actions.voicemail(context)
            k in '2'..'9' && query.isEmpty() -> speedDial(k - '0')
            k.isDigit() && query.length == 1 && query[0] in '1'..'9' -> speedDial((query[0] - '0') * 10 + (k - '0'))
            else -> { feedback(k); vm.press(k) }
        }
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(Modifier.fillMaxSize().background(c.bg).imePadding()) {
        TopBar(
            textMode = textMode,
            text = text.orEmpty(),
            onText = vm::setText,
            onOpenSearch = { vm.openTextSearch() },
            onCloseSearch = { vm.closeTextSearch() },
            onAddContact = { Actions.addContact(context, query) },
            onSettings = openSettings,
        )
        if (!searching) ViewChips(view, onSelect = { view = it })

        // Settings > Appearance > Font size scales all text in the list (names, numbers, details)
        val baseDensity = LocalDensity.current
        val listDensity = Density(baseDensity.density, baseDensity.fontScale * prefs.listFontSize / DEFAULT_FONT_SIZE.toFloat())
        Box(Modifier.weight(1f).fillMaxWidth()) {
        CompositionLocalProvider(LocalDensity provides listDensity) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(collapseOnDrag),
                contentPadding = PaddingValues(bottom = if (textMode) navBottom + 12.dp else 12.dp),
            ) {
                if (state.loaded && !state.hasContacts) {
                    item("access") { AccessCard(opensSettings = askedForAccess, onAllow = requestAccess) }
                }

                if (!searching) {
                    when (view) {
                        HomeView.CALL_LOG -> {
                            if (state.recents.isEmpty()) {
                                item("e-log") { EmptyNote(if (state.hasCallLog) "No calls yet." else "Allow call history access to see your calls here.") }
                            }
                            itemsIndexed(state.recents, key = { i, r -> "r$i-${r.key}" }) { _, r ->
                                SwipeActions(prefs.swipeActions && r.number.isNotBlank(), { call(r.number) }, { message(r.number) }) {
                                    RecentRow(
                                        r, AnnotatedString(r.title()), AnnotatedString(r.number),
                                        onClick = { call(r.number) },
                                        onLongClick = { sheet = r.toTarget() },
                                    )
                                }
                            }
                        }
                        HomeView.CONTACTS, HomeView.FAVOURITES -> {
                            val list = if (view == HomeView.FAVOURITES) state.favourites else state.contacts
                            if (list.isEmpty() && state.hasContacts) {
                                item("e-contacts") {
                                    EmptyNote(
                                        if (view == HomeView.FAVOURITES) "No favourites yet. Star someone in your Contacts app and they'll show here."
                                        else "No contacts with phone numbers yet."
                                    )
                                }
                            }
                            items(list, key = { "a${it.contactId}" }) { e ->
                                SwipeActions(prefs.swipeActions, { call(e.number) }, { message(e.number) }) {
                                    ContactRow(
                                        e, AnnotatedString(e.name), AnnotatedString(e.number),
                                        onClick = { call(e.number) },
                                        onLongClick = { sheet = e.toTarget() },
                                    )
                                }
                            }
                        }
                        HomeView.FREQUENT -> {
                            if (state.frequent.isEmpty()) {
                                item("e-freq") {
                                    EmptyNote(
                                        if (state.hasCallLog) "People you call often will show here once you've called them a couple of times."
                                        else "Allow call history access to see who you call most."
                                    )
                                }
                            }
                            items(state.frequent, key = { "f${it.recent.key}" }) { f ->
                                val r = f.recent
                                SwipeActions(prefs.swipeActions, { call(r.number) }, { message(r.number) }) {
                                    PersonRow(
                                        AnnotatedString(r.title()), r.name, r.contact?.photoUri,
                                        onClick = { call(r.number) },
                                        onLongClick = { sheet = r.toTarget() },
                                        org = r.contact?.org,
                                        trailing = {
                                            Text("${f.count} calls", color = c.inkMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                                        },
                                    ) {
                                        Text(if (r.name != null) r.number else "Not in contacts", color = c.inkMuted, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (results.calls.isNotEmpty()) {
                        item("h-calls") { SectionHeader("Call log") }
                        items(results.calls, key = { "l${it.recent.key}" }) { h ->
                            SwipeActions(prefs.swipeActions, { call(h.recent.number) }, { message(h.recent.number) }) {
                                val numberText = highlight(h.recent.number, h.numberHighlights, c.accent)
                                RecentRow(
                                    h.recent,
                                    // Unsaved numbers show the number as the title, so highlight it there
                                    if (h.recent.name != null) highlight(h.recent.title(), h.nameHighlights, c.accent) else numberText,
                                    numberText,
                                    onClick = {
                                        val contact = h.recent.contact
                                        when {
                                            !prefs.tapOpensContact -> call(h.recent.number)
                                            contact != null -> Actions.openContact(context, contact.contactId, contact.lookupKey)
                                            else -> { sheet = h.recent.toTarget() }   // unsaved number: show its options
                                        }
                                    },
                                    onLongClick = { sheet = h.recent.toTarget() },
                                )
                            }
                        }
                    }
                    if (results.contacts.isNotEmpty()) {
                        item("h-phones") { SectionHeader("Contacts") }
                        items(results.contacts, key = { "c${it.entry.contactId}:${it.entry.number}" }) { h ->
                            SwipeActions(prefs.swipeActions, { call(h.entry.number) }, { message(h.entry.number) }) {
                                ContactRow(
                                    h.entry,
                                    highlight(h.entry.name, h.nameHighlights, c.accent),
                                    highlight(h.entry.number, h.numberHighlights, c.accent),
                                    onClick = {
                                        if (prefs.tapOpensContact) Actions.openContact(context, h.entry.contactId, h.entry.lookupKey)
                                        else call(h.entry.number)
                                    },
                                    onLongClick = { sheet = h.entry.toTarget() },
                                )
                            }
                        }
                    }
                    if (results.isEmpty && !textMode) {
                        item("no-match") {
                            Column(Modifier.padding(top = 8.dp)) {
                                ActionRow(Icons.Filled.PersonAdd, "Add ${vm.format(query).ifEmpty { query }} to contacts") {
                                    Actions.addContact(context, query)
                                }
                                ActionRow(Icons.AutoMirrored.Filled.Message, "Send a message") { message(query) }
                            }
                        }
                    }
                    if (results.isEmpty && textMode) {
                        item("no-text-match") {
                            Text(
                                "No one matches \u201c${text.orEmpty().trim()}\u201d.",
                                color = c.inkMuted, fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
        }

        if (!textMode) {
            val display = vm.format(query).ifEmpty { query }
            AnimatedVisibility(
                visible = dialpadOpen,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                DialpadPanel(
                    query = query,
                    display = display,
                    cyrillic = prefs.cyrillicKeys,
                    scale = prefs.dialpadSize.scale,
                    onKey = { k -> feedback(k); vm.press(k) },
                    onLongKey = onLongKey,
                    onBackspace = vm::backspace,
                    onClear = vm::clear,
                    // With nothing typed, the call button does nothing
                    onCall = { if (query.isNotEmpty()) call(query) },
                    onCallLong = {
                        if (query.isNotEmpty()) Actions.call(context, withLongPressPrefix(query, prefs.longPressPrefix))
                    },
                    onHide = { dialpadOpen = false },
                )
            }
            AnimatedVisibility(visible = !dialpadOpen, enter = fadeIn(), exit = fadeOut()) {
                CollapsedDialpad(query, display, onShow = { dialpadOpen = true })
            }
        }
    }

    sheet?.let { target ->
        ActionsSheet(
            target,
            prefs = prefs,
            onEdit = { vm.closeTextSearch(); vm.setQuery(target.number); dialpadOpen = true },
            onSpeedDial = { speedDialFor = target },
            onRemoveSpeedDial = { slot -> vm.removeSpeedDial(slot) },
            onDismiss = { sheet = null },
        )
    }

    speedDialFor?.let { target ->
        SpeedDialDialog(
            target, prefs,
            onSave = { slot ->
                vm.setSpeedDial(slot, target.title, target.number)
                toast(if (slot < 10) "Long-press $slot to call ${target.title}" else "Tap ${slot / 10}, then long-press ${slot % 10} to call ${target.title}")
                speedDialFor = null
            },
            onDismiss = { speedDialFor = null },
        )
    }
}

// ---------- top bar ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewChips(current: HomeView, onSelect: (HomeView) -> Unit) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (v in HomeView.entries) {
            FilterChip(
                selected = v == current,
                onClick = { onSelect(v) },
                label = { Text(v.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = c.accent,
                    selectedLabelColor = c.onAccent,
                    labelColor = c.inkMuted,
                ),
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        color = LocalDialerColors.current.inkMuted,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

@Composable
private fun TopBar(
    textMode: Boolean,
    text: String,
    onText: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onAddContact: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (textMode) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            IconButton(onClick = onCloseSearch) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = c.ink) }
            TextField(
                value = text,
                onValueChange = onText,
                placeholder = { Text("Search by name or number", color = c.inkMuted) },
                singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focus),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = c.accent,
                ),
            )
            if (text.isNotEmpty()) {
                IconButton(onClick = { onText("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear", tint = c.inkMuted) }
            }
        } else {
            Text(
                "Dialer",
                color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 16.dp),
            )
            IconButton(onClick = onAddContact) { Icon(Icons.Filled.PersonAdd, contentDescription = "Add contact", tint = c.inkMuted) }
            IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, contentDescription = "Search with keyboard", tint = c.inkMuted) }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = c.inkMuted) }
        }
    }
}

// ---------- rows ----------

/** Swipe right to call, left to message. The row springs back after the action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActions(enabled: Boolean, onCall: () -> Unit, onMessage: () -> Unit, content: @Composable () -> Unit) {
    if (!enabled) { content(); return }
    val c = LocalDialerColors.current
    val callNow by rememberUpdatedState(onCall)
    val messageNow by rememberUpdatedState(onMessage)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> callNow()
                SwipeToDismissBoxValue.EndToStart -> messageNow()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val (bg, icon, align) = when (state.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(c.call, Icons.Filled.Call, Alignment.CenterStart)
                SwipeToDismissBoxValue.EndToStart -> Triple(c.accent, Icons.AutoMirrored.Filled.Message, Alignment.CenterEnd)
                SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.CenterStart)
            }
            Box(Modifier.fillMaxSize().background(bg).padding(horizontal = 28.dp), contentAlignment = align) {
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.White)
            }
        },
    ) {
        Box(Modifier.background(c.bg)) { content() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonRow(
    title: AnnotatedString,
    avatarName: String?,
    photoUri: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    titleSuffix: String? = null,
    org: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    subtitle: @Composable () -> Unit,
) {
    val c = LocalDialerColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "More options")
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(avatarName, photoUri)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    maxLines = if (LocalMultilineNames.current) 2 else 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (titleSuffix != null) Text("  $titleSuffix", color = c.inkMuted, fontSize = 14.sp, maxLines = 1)
            }
            if (org != null && LocalShowOrg.current) {
                Text(
                    org, color = c.inkMuted, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                    maxLines = if (LocalMultilineNames.current) 2 else 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            subtitle()
        }
        trailing?.invoke()
    }
}

@Composable
private fun ContactRow(
    entry: PhoneEntry,
    name: AnnotatedString,
    number: AnnotatedString,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val c = LocalDialerColors.current
    PersonRow(name, entry.name, entry.photoUri, onClick, onLongClick, org = entry.org) {
        // Just the number, in full: no "Mobile"/"Home" label, wraps rather than cutting off
        Text(number, color = c.inkMuted, fontSize = 14.sp)
    }
}

@Composable
private fun RecentRow(r: RecentGroup, title: AnnotatedString, number: AnnotatedString, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = LocalDialerColors.current
    PersonRow(
        title, r.name, r.contact?.photoUri, onClick, onLongClick,
        titleSuffix = if (r.count > 1) "(${r.count})" else null,
        org = r.contact?.org,
        trailing = {
            Text(relativeTime(r.date), color = c.inkMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CallTypeIcon(r.type)
            Spacer(Modifier.width(6.dp))
            if (r.name != null) {
                // Just the full number, no "Mobile"/"Home" label
                Text(number, color = c.inkMuted, fontSize = 14.sp)
            } else {
                Text(if (r.number.isBlank()) callTypeName(r.type) else "Not in contacts", color = c.inkMuted, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Avatar(name: String?, photoUri: String?, size: Dp = 44.dp) {
    val c = LocalDialerColors.current
    val initial = name?.firstOrNull { it.isLetter() }?.uppercaseChar()
    val (bg, fg) = if (initial == null) c.panel to c.inkMuted
                   else c.avatars[Math.floorMod(name.hashCode(), c.avatars.size)]
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (initial != null) Text(initial.toString(), color = fg, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        else Icon(Icons.Filled.Person, contentDescription = null, tint = fg)
        if (photoUri != null) {
            AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun CallTypeIcon(type: Int) {
    val c = LocalDialerColors.current
    val (icon, tint) = when (type) {
        CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade to c.inkMuted
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Filled.CallMissed to c.missed
        CallLog.Calls.BLOCKED_TYPE -> Icons.Filled.Block to c.missed
        else -> Icons.AutoMirrored.Filled.CallReceived to c.inkMuted
    }
    Icon(icon, contentDescription = callTypeName(type), tint = tint, modifier = Modifier.size(16.dp))
}

private fun callTypeName(type: Int) = when (type) {
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    CallLog.Calls.MISSED_TYPE -> "Missed"
    CallLog.Calls.REJECTED_TYPE -> "Declined"
    CallLog.Calls.BLOCKED_TYPE -> "Blocked"
    else -> "Incoming"
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        color = LocalDialerColors.current.inkMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = c.accent) }
        Spacer(Modifier.width(14.dp))
        Text(text, color = c.ink, fontSize = 16.sp)
    }
}

@Composable
private fun AccessCard(opensSettings: Boolean, onAllow: () -> Unit) {
    val c = LocalDialerColors.current
    Surface(
        color = c.panel,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Search contacts with the keypad", color = c.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (opensSettings)
                    "Turn on Contacts, Call logs and Phone in this app's permissions, then come back."
                else
                    "Allow access to your contacts and call history to find people by name and see recent calls.",
                color = c.inkMuted, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = onAllow) { Text(if (opensSettings) "Open settings" else "Allow access") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionsSheet(
    target: SheetTarget,
    prefs: Prefs,
    onEdit: () -> Unit,
    onSpeedDial: () -> Unit,
    onRemoveSpeedDial: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = LocalDialerColors.current
    val dial = prefs.speedDialFor(target.number)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.panel) {
        Column(Modifier.padding(bottom = 12.dp)) {
            Text(target.title, color = c.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 24.dp))
            if (target.title != target.number && target.number.isNotBlank()) {
                Text(target.number, color = c.inkMuted, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp))
            }
            Spacer(Modifier.height(8.dp))
            if (target.number.isBlank()) {
                Text("This caller hid their number.", color = c.inkMuted, fontSize = 15.sp, modifier = Modifier.padding(24.dp))
                return@Column
            }
            ActionRow(Icons.Filled.Call, "Call") { Actions.call(context, withDialPrefix(target.number, prefs.dialPrefix)); onDismiss() }
            ActionRow(Icons.AutoMirrored.Filled.Message, "Send a message") { Actions.message(context, target.number); onDismiss() }
            ActionRow(Icons.Filled.Dialpad, "Edit number before calling") { onEdit(); onDismiss() }
            if (dial == null) {
                ActionRow(Icons.Filled.Speed, "Set speed dial") { onSpeedDial(); onDismiss() }
            } else {
                ActionRow(Icons.Filled.Speed, "Remove from speed dial ${dial.slot}") { onRemoveSpeedDial(dial.slot); onDismiss() }
            }
            ActionRow(Icons.Filled.ContentCopy, "Copy number") { Actions.copy(context, target.number); onDismiss() }
            if (target.contactId != null) {
                ActionRow(Icons.Filled.Person, "Open contact") { Actions.openContact(context, target.contactId, target.lookupKey); onDismiss() }
            } else {
                ActionRow(Icons.Filled.PersonAdd, "Add to contacts") { Actions.addContact(context, target.number); onDismiss() }
                ActionRow(Icons.Filled.Public, "Look up number online") { Actions.lookUp(context, target.number, prefs.lookupUrl); onDismiss() }
            }
        }
    }
}

@Composable
private fun SpeedDialDialog(target: SheetTarget, prefs: Prefs, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = LocalDialerColors.current
    var input by rememberSaveable { mutableStateOf((2..99).firstOrNull { it !in prefs.speedDials }?.toString().orEmpty()) }
    val slot = input.toIntOrNull()
    val valid = slot != null && slot in 2..99
    val taken = slot?.let { prefs.speedDials[it] }?.takeIf { numberKey(it.number) != numberKey(target.number) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speed dial for ${target.title}") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { v -> input = v.filter { it.isDigit() }.take(2) },
                    label = { Text("Number from 2 to 99") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        !valid -> "Choose a number from 2 to 99. 1 is kept for voicemail."
                        taken != null -> "This replaces ${taken.name} on $slot."
                        slot!! < 10 -> "To call, long-press $slot on the keypad."
                        else -> "To call, tap ${slot / 10}, then long-press ${slot % 10}."
                    },
                    color = if (taken != null) c.missed else c.inkMuted,
                    fontSize = 14.sp,
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { slot?.let(onSave) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---------- helpers ----------

private fun RecentGroup.title() = name ?: number.ifBlank { "Private number" }

private fun PhoneEntry.toTarget() = SheetTarget(name, number, contactId, lookupKey)

private fun RecentGroup.toTarget() = SheetTarget(title(), number, contact?.contactId, contact?.lookupKey)

private fun highlight(text: String, indices: IntArray, color: Color): AnnotatedString {
    if (indices.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        val style = SpanStyle(color = color, fontWeight = FontWeight.Bold)
        for (i in indices) if (i in text.indices) addStyle(style, i, i + 1)
    }
}

private fun relativeTime(time: Long): String =
    DateUtils.getRelativeTimeSpanString(
        time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
