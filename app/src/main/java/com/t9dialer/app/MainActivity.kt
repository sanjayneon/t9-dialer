package com.t9dialer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.t9dialer.app.ui.DialerApp
import com.t9dialer.app.ui.DialerTheme
import com.t9dialer.app.ui.DialerViewModel

class MainActivity : ComponentActivity() {

    private val vm: DialerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()   // required on Android 16; the UI handles system bar insets itself
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            DialerTheme { DialerApp(vm) }
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
