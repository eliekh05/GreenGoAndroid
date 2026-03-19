package com.greengo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.ui.ContentView

class MainActivity : ComponentActivity() {

    private val vm: AppStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialise SharedPreferences-backed state (equivalent to iOS UserDefaults reads in AppState.init)
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
