package com.greengo.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.screens.*

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - ContentView
// Root composable: reads screen from ViewModel, renders matching screen.
// AnimatedContent provides the .easeInOut(duration: 0.22) crossfade equivalent.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentView(vm: AppStateViewModel) {
    val screen by vm.screen.collectAsState()

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
            is Screen.Splash        -> SplashScreen(vm)
            is Screen.Home          -> HomeScreen(vm)
            is Screen.Map           -> MapScreen(vm)
            is Screen.MapInfo       -> MapInfoScreen(vm)
            is Screen.Functionality -> FunctionalityScreen(vm)
            is Screen.Translator    -> TranslatorScreen(vm)
            is Screen.Pedometer     -> PedometerScreen(vm)
            is Screen.Games         -> GamesScreen(vm)
            is Screen.MemoryInfo    -> MemoryInfoScreen(vm)
            is Screen.WildRecall    -> WildRecallScreen(vm)
            is Screen.OceanInfo     -> OceanInfoScreen(vm)
            is Screen.Ocean         -> OceanGameScreen(vm)
            is Screen.Trivia        -> TriviaScreen(vm)
            is Screen.TriviaScore   -> TriviaScoreScreen(vm, targetScreen.score)
            is Screen.Settings      -> SettingsScreen(vm)
            is Screen.About         -> AboutScreen(vm)
            is Screen.Contact       -> ContactScreen(vm)
            is Screen.Preferences   -> PreferencesScreen(vm)
        }
    }
}
