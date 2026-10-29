package com.t9dialer.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.t9dialer.app.ui.DialerApp
import com.t9dialer.app.ui.DialerTheme
import com.t9dialer.app.ui.DialerViewModel
import com.t9dialer.app.ui.isDark

class MainActivity : ComponentActivity() {

    private val vm: DialerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // required on Android 16; the UI handles system bar insets itself
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            val dark = isDark(prefs.theme)
            // Keep status and navigation bar icons readable when the app's theme differs from the phone's
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                                     else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                                         else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            }
            DialerTheme(dark) { DialerApp(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Pre-fills the keypad when another app or a tel: link asks to dial a number. */
    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "tel") vm.setQuery(data.schemeSpecificPart.orEmpty())
    }
}
