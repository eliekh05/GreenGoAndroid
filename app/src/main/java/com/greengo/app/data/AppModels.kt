package com.greengo.app.data

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Navigation
// ─────────────────────────────────────────────────────────────────────────────

sealed class Screen {
    object Splash       : Screen()
    object Home         : Screen()
    object Map          : Screen()
    object MapInfo      : Screen()
    object Functionality: Screen()
    object Translator   : Screen()
    object Pedometer    : Screen()
    object Games        : Screen()
    object MemoryInfo   : Screen()
    object WildRecall   : Screen()
    object OceanInfo    : Screen()
    object Ocean        : Screen()
    object Trivia       : Screen()
    data class TriviaScore(val score: Int) : Screen()
    object Settings     : Screen()
    object About        : Screen()
    object Contact      : Screen()
    object Preferences  : Screen()
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - App Theme
// ─────────────────────────────────────────────────────────────────────────────

enum class AppTheme(val rawValue: String) {
    MINT("mint"),
    LIGHT("light"),
    DARK("dark"),
    NATURE("nature");

    val displayName: String get() = when (this) {
        MINT   -> "Mint"
        LIGHT  -> "Light"
        DARK   -> "Dark"
        NATURE -> "Nature"
    }

    val background: Color get() = when (this) {
        MINT   -> Color(red = 0.82f, green = 1.00f, blue = 0.97f)
        LIGHT  -> Color.White
        DARK   -> Color(red = 0.10f, green = 0.12f, blue = 0.15f)
        NATURE -> Color(red = 0.85f, green = 0.93f, blue = 0.83f)
    }

    val text: Color get() =
        if (this == DARK) Color.White else Color(red = 0.20f, green = 0.20f, blue = 0.20f)

    val accent: Color get() = when (this) {
        DARK   -> Color(red = 0.40f, green = 0.90f, blue = 0.50f)
        NATURE -> Color(red = 0.15f, green = 0.55f, blue = 0.25f)
        else   -> Color(red = 0.12f, green = 0.58f, blue = 0.08f)
    }

    val appearanceDot: Color get() = when (this) {
        MINT   -> Color(red = 0.10f, green = 0.74f, blue = 0.58f)
        LIGHT  -> Color(red = 0.75f, green = 0.75f, blue = 0.75f)
        DARK   -> Color(red = 0.24f, green = 0.30f, blue = 0.40f)
        NATURE -> Color(red = 0.34f, green = 0.55f, blue = 0.20f)
    }

    val cardBackground: Color get() =
        if (this == DARK) Color(red = 0.14f, green = 0.14f, blue = 0.14f) else Color.White

    val inputBackground: Color get() =
        if (this == DARK) Color(red = 0.12f, green = 0.12f, blue = 0.12f) else Color.White

    val mutedText: Color get() = text.copy(alpha = 0.70f)

    companion object {
        fun fromRawValue(raw: String) = values().firstOrNull { it.rawValue == raw } ?: MINT
    }
}
