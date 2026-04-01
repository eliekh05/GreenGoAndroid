package com.greengo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {

    private val vm: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Let Compose draw under system bars (edge‑to‑edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hide both status and nav bars (immersive experience)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Back button logic: internal navigation or exit
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.canNavigateBack()) {
                    vm.navigateBack()
                } else {
                    finishAffinity()  // exit app
                }
            }
        })

        // Init your ViewModel state
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
}