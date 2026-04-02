package com.greengo.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.screens.AboutScreen
import com.greengo.app.ui.screens.ContactScreen
import com.greengo.app.ui.screens.FunctionalityScreen
import com.greengo.app.ui.screens.GamesScreen
import com.greengo.app.ui.screens.HomeScreen
import com.greengo.app.ui.screens.MapInfoScreen
import com.greengo.app.ui.screens.MapScreen
import com.greengo.app.ui.screens.MemoryInfoScreen
import com.greengo.app.ui.screens.OceanGameScreen
import com.greengo.app.ui.screens.OceanInfoScreen
import com.greengo.app.ui.screens.PedometerScreen
import com.greengo.app.ui.screens.PreferencesScreen
import com.greengo.app.ui.screens.SettingsScreen
import com.greengo.app.ui.screens.SplashScreen
import com.greengo.app.ui.screens.TranslatorScreen
import com.greengo.app.ui.screens.TriviaScoreScreen
import com.greengo.app.ui.screens.TriviaScreen
import com.greengo.app.ui.screens.WildRecallScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentView(vm: AppStateViewModel) {
    val screen by vm.screen.collectAsState()

    // Global back handler:
    // - On Splash: do nothing (can't go back, splash is one-way)
    // - On Home with empty stack: do nothing (let Android handle — exits app)
    // - Everywhere else: pop the back stack
    BackHandler(enabled = screen !is Screen.Splash && !vm.isAtRoot()) {
        vm.navigateBack()
    }

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
