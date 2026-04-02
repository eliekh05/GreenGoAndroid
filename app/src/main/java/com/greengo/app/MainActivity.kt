package com.greengo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.ui.ContentView

// ─────────────────────────────────────────────────────────────────────────────
// Immersive mode for the whole app — exactly like YouTube / Netflix / games:
//
//   • Status bar + navigation bar are hidden at all times.
//   • Swipe from edge → bars appear briefly (transient overlay), then auto-hide.
//   • No toggle button. No per-screen logic. One place, whole app.
//   • onWindowFocusChanged re-applies it after dialogs / notifications restore bars.
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    private val vm: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge() must be before setContent — makes system bars transparent
        // and tells the system we'll handle insets ourselves (SDK 35+ enforces this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Draw our content behind system bars (full screen canvas)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide bars for the whole app
        hideSystemBars()

        vm.init(applicationContext)

        setContent {
            val theme by vm.theme.collectAsState()

            MaterialTheme(
                colorScheme = if (theme == AppTheme.DARK) darkColorScheme() else lightColorScheme()
            ) {
                ContentView(vm = vm)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // System can restore bars after notifications, dialogs, or app switcher.
        // Re-hide whenever we get focus back — same as how games handle this.
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            // Hide both status bar and navigation bar
            hide(WindowInsetsCompat.Type.systemBars())
            // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE:
            // Swipe from edge → bars appear as a transparent overlay briefly → auto-hide.
            // This is exactly what YouTube, Netflix, and games use.
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
