package com.greengo.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WindowSize
//
// Fully automatic. Zero hardcoded device names or lookup tables.
//
// Android gives us screenWidthDp + screenHeightDp at runtime via
// LocalConfiguration. These are density-independent, rotation-aware,
// and correct on every device: phones, tablets, foldables, Chromebooks, TVs.
// They also update live on window resize (split-screen, foldable hinge change).
//
// Material 3 canonical width breakpoints:
//   < 600 dp  → COMPACT   phones portrait, foldable cover screens
//   600–839dp → MEDIUM    foldable main open, large phone landscape, small tablet
//   ≥ 840 dp  → EXPANDED  tablets, Chromebooks, large foldables open
//
// Every dp value below is computed from a single linear scale anchored at
// 412 dp (the most common flagship width — Pixel 6-10, S24+, S25 Ultra etc).
// Nothing is looked up. Everything just scales.
// ─────────────────────────────────────────────────────────────────────────────

enum class WindowWidthClass  { COMPACT, MEDIUM, EXPANDED }
enum class WindowHeightClass { COMPACT, MEDIUM, EXPANDED }

@Stable
class WindowSize(
    val widthClass:  WindowWidthClass,
    val heightClass: WindowHeightClass,
    val widthDp:     Dp,
    val heightDp:    Dp
) {
    // ── Form factor ───────────────────────────────────────────────────────────

    val isPhone:         Boolean get() = widthClass == WindowWidthClass.COMPACT
    val isMedium:        Boolean get() = widthClass == WindowWidthClass.MEDIUM
    val isTablet:        Boolean get() = widthClass == WindowWidthClass.EXPANDED
    val aspectRatio:     Float   get() = heightDp.value / widthDp.value

    // Foldable cover screen — Z Fold6 cover = 323 dp, very constrained
    val isFoldableCover: Boolean get() = isPhone && widthDp < 340.dp

    // Flip phone or extreme 21:9 screen (Z Flip6 = 2.44, Xperia ≈ 2.6)
    // Standard flagships (S25 Ultra = 2.16, Pixel 10 = 2.24) are NOT ultra-tall
    val isUltraTall:     Boolean get() = isPhone && aspectRatio > 2.35f

    // ── Scale factor ──────────────────────────────────────────────────────────
    // Anchored at 412 dp (most common flagship width).
    // 360 dp phones → 0.87,  984 dp Tab S9 → 2.39
    // Clamped so absurd values (300 dp / 1400 dp) stay sane.
    private val scale: Float get() =
        (widthDp.value / 412f).coerceIn(0.78f, 2.5f)

    // Milder scale for text — readability doesn't need to grow as fast as layout
    private val textScale: Float get() =
        (1f + (scale - 1f) * 0.55f).coerceIn(0.82f, 1.8f)

    // ── Spacing ───────────────────────────────────────────────────────────────

    val contentPadding: Dp get() {
        val raw = (16f * scale).roundToInt().dp
        return if (isFoldableCover) raw.coerceIn(10.dp, 14.dp)
               else raw.coerceIn(12.dp, 40.dp)
    }

    val cardSpacing:       Dp get() = (14f * scale).roundToInt().dp.coerceIn(10.dp, 26.dp)
    val cardInnerPadding:  Dp get() = (16f * scale).roundToInt().dp.coerceIn(12.dp, 28.dp)
    val cardRadius:        Dp get() = (16f * scale).roundToInt().dp.coerceIn(10.dp, 28.dp)
    val buttonHeight:      Dp get() = (52f * scale).roundToInt().dp.coerceIn(44.dp, 72.dp)
    val buttonVPadding:    Dp get() = (14f * scale).roundToInt().dp.coerceIn(10.dp, 20.dp)

    // ── Sizes ─────────────────────────────────────────────────────────────────

    val tileIconSize:     Dp get() = (64f  * scale).roundToInt().dp.coerceIn(52.dp, 100.dp)
    val smallIconSize:    Dp get() = (18f  * scale).roundToInt().dp.coerceIn(16.dp,  28.dp)
    val navIconSize:      Dp get() = (32f  * scale).roundToInt().dp.coerceIn(28.dp,  48.dp)
    val logoSize:         Dp get() = (90f  * scale).roundToInt().dp.coerceIn(70.dp, 140.dp)
    val memoryCardSize:   Dp get() = (76f  * scale).roundToInt().dp.coerceIn(60.dp, 110.dp)

    val gameBannerHeight: Dp get() {
        val base = if (isUltraTall) 130f else 110f
        return (base * scale).roundToInt().dp.coerceIn(80.dp, 200.dp)
    }

    // ── Typography ────────────────────────────────────────────────────────────

    val navTitleSp:   Float get() = (22f * textScale).coerceIn(17f, 32f)
    val headingSp:    Float get() = (28f * textScale).coerceIn(20f, 48f)
    val cardTitleSp:  Float get() = (20f * textScale).coerceIn(15f, 32f)
    val bodySp:       Float get() = (14f * textScale).coerceIn(12f, 18f)
    val captionSp:    Float get() = (12f * textScale).coerceIn(10f, 16f)
    val statSp:       Float get() = (22f * textScale).coerceIn(16f, 36f)

    // ── Grid ─────────────────────────────────────────────────────────────────

    // Memory cards — targets ~85 dp per card
    val memoryCardColumns: Int get() = (widthDp.value / 85f).toInt().coerceIn(3, 8)

    // General grid (game cards, trivia) — targets ~180 dp per column
    val gridColumns: Int get() = (widthDp.value / 180f).toInt().coerceIn(1, 6)

    val useTwoColumnLayout: Boolean get() = !isPhone
}

@Composable
fun rememberWindowSize(): WindowSize {
    val config = LocalConfiguration.current
    return remember(config.screenWidthDp, config.screenHeightDp) {
        WindowSize(
            widthClass = when {
                config.screenWidthDp < 600  -> WindowWidthClass.COMPACT
                config.screenWidthDp < 840  -> WindowWidthClass.MEDIUM
                else                        -> WindowWidthClass.EXPANDED
            },
            heightClass = when {
                config.screenHeightDp < 480 -> WindowHeightClass.COMPACT
                config.screenHeightDp < 900 -> WindowHeightClass.MEDIUM
                else                        -> WindowHeightClass.EXPANDED
            },
            widthDp  = config.screenWidthDp.dp,
            heightDp = config.screenHeightDp.dp
        )
    }
}
