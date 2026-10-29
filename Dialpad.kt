package com.t9dialer.app.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.t9dialer.app.t9.T9Keys

private val ROWS = listOf("123", "456", "789", "*0#")

@Composable
fun DialpadPanel(
    query: String,
    display: String,
    cyrillic: Boolean,
    /** 1 = Normal; Small, Large and Huge scale the keys and their text. */
    scale: Float,
    onKey: (Char) -> Unit,
    onLongKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCall: () -> Unit,
    onCallLong: () -> Unit,
    onHide: () -> Unit,
) {
    val c = LocalDialerColors.current
    val letters = if (cyrillic) T9Keys.cyrillicLetters else T9Keys.latinLetters
    Surface(
        color = c.panel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalSwipe(onDown = onHide)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        ) {
            Handle(onClickLabel = "Hide keypad", onClick = onHide)
            NumberBar(query, display, onCall, onCallLong, onBackspace, onClear)
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                for (row in ROWS) {
                    Row(Modifier.fillMaxWidth()) {
                        for (key in row) DialKey(key, letters[key].orEmpty(), scale, onKey, onLongKey, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** The keypad tucked away: a slim bar you tap or swipe up to bring it back. */
@Composable
fun CollapsedDialpad(query: String, display: String, onShow: () -> Unit) {
    val c = LocalDialerColors.current
    Surface(
        color = c.panel,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalSwipe(onUp = onShow)
                .clickable(onClickLabel = "Show keypad", onClick = onShow)
                .navigationBarsPadding()
        ) {
            Handle(onClickLabel = null, onClick = null)
            Row(
                Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Dialpad, contentDescription = null, tint = c.accent)
                Spacer(Modifier.width(14.dp))
                Text(
                    if (query.isEmpty()) "Show keypad" else display,
                    color = if (query.isEmpty()) c.inkMuted else c.ink,
                    fontSize = if (query.isEmpty()) 16.sp else 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Handle(onClickLabel: String?, onClick: (() -> Unit)?) {
    val c = LocalDialerColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(CircleShape).background(c.inkMuted.copy(alpha = 0.35f)))
    }
}

/** Swipe down (or up) anywhere on the keypad sheet, keys included. */
private fun Modifier.verticalSwipe(onUp: (() -> Unit)? = null, onDown: (() -> Unit)? = null): Modifier =
    pointerInput(onUp, onDown) {
        val threshold = 48.dp.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                if (total > threshold) onDown?.invoke()
                else if (total < -threshold) onUp?.invoke()
            },
        ) { change, amount ->
            total += amount
            change.consume()
        }
    }

/**
 * Full-width strip: call button flush at the left edge, the number in the middle,
 * delete flush at the right edge, with thin dividers between them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberBar(
    query: String,
    display: String,
    onCall: () -> Unit,
    onCallLong: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    val c = LocalDialerColors.current
    Row(
        Modifier.fillMaxWidth().height(64.dp).background(c.bg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Greyed out and inert until a number is typed
        val canCall = query.isNotEmpty()
        Box(
            Modifier
                .fillMaxHeight()
                .width(76.dp)
                .combinedClickable(
                    enabled = canCall,
                    onClickLabel = "Call",
                    onLongClickLabel = "Call with long-press prefix",
                    onLongClick = onCallLong,
                    onClick = onCall,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Call,
                contentDescription = "Call",
                tint = if (canCall) c.call else c.inkMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(30.dp),
            )
        }
        BarDivider()
        if (query.isEmpty()) {
            Text(
                "Type a name or number",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = c.inkMuted,
                fontSize = 16.sp,
            )
        } else {
            val size = when {
                display.length <= 11 -> 34.sp
                display.length <= 14 -> 28.sp
                display.length <= 18 -> 23.sp
                else -> 19.sp
            }
            Text(
                display,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                textAlign = TextAlign.Center,
                color = c.ink,
                fontSize = size,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        BarDivider()
        Box(
            Modifier
                .fillMaxHeight()
                .width(76.dp)
                .then(
                    if (query.isNotEmpty())
                        Modifier
                            .combinedClickable(onClick = onBackspace, onLongClick = onClear, onLongClickLabel = "Clear all")
                            .semantics { contentDescription = "Delete" }
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Backspace,
                contentDescription = null,
                tint = if (query.isNotEmpty()) c.ink else c.inkMuted.copy(alpha = 0.35f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun BarDivider() {
    Box(
        Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .width(1.dp)
            .background(LocalDialerColors.current.inkMuted.copy(alpha = 0.25f))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(key: Char, letters: String, scale: Float, onKey: (Char) -> Unit, onLongKey: (Char) -> Unit, modifier: Modifier) {
    val c = LocalDialerColors.current
    val press by rememberUpdatedState(onKey)
    val longPress by rememberUpdatedState(onLongKey)
    Box(
        modifier
            .height(58.dp * scale)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 40.dp * scale),
                onLongClick = { longPress(key) },
                onClick = { press(key) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(key.toString(), color = c.ink, fontSize = 30.sp * scale, lineHeight = 32.sp * scale, textAlign = TextAlign.End, modifier = Modifier.width(30.dp * scale))
            Spacer(Modifier.width(8.dp * scale))
            Box(Modifier.width(46.dp * scale)) {
                if (key == '1') {
                    Icon(Icons.Filled.Voicemail, contentDescription = "Long-press for voicemail", tint = c.inkMuted, modifier = Modifier.size(16.dp * scale))
                } else {
                    Text(letters, color = c.inkMuted, fontSize = 12.sp * scale, letterSpacing = 1.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/**
 * Haptic tick (only if Vibrate on tap is ticked) plus a DTMF tone,
 * following the phone's dial-pad-tone setting and silent mode.
 */
@Composable
fun rememberKeyFeedback(vibrate: Boolean, durationMs: Int): (Char) -> Unit {
    val context = LocalContext.current
    val view = LocalView.current
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 60) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    val tonesOn = remember {
        runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1
        }.getOrDefault(false)
    }
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    return remember(tone, tonesOn, vibrate, durationMs) {
        { key: Char ->
            if (vibrate) {
                // System default is the phone's own light tick; otherwise vibrate for the chosen length
                if (durationMs <= 0) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                else vibrateFor(context, durationMs)
            }
            if (tonesOn && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                dtmfTone(key)?.let { tone?.startTone(it, 120) }
            }
        }
    }
}

/** Stands in for the system's haptics when Vibrate on tap is off, so long-presses don't buzz either. */
object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

private fun dtmfTone(key: Char): Int? = when (key) {
    in '0'..'9' -> ToneGenerator.TONE_DTMF_0 + (key - '0')
    '*' -> ToneGenerator.TONE_DTMF_S
    '#' -> ToneGenerator.TONE_DTMF_P
    else -> null
}
