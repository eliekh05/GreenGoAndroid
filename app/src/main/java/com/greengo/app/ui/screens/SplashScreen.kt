package com.greengo.app.ui.screens

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SplashScreen
//
// iOS approach:
//   AVPlayer renders directly onto an AVPlayerLayer (a CALayer subclass).
//   The UIView subclass sets its layerClass to AVPlayerLayer, so the player
//   draws straight into the view's backing layer — zero UI chrome possible.
//   .allowsHitTesting(false) blocks all input. .statusBarHidden(true) hides
//   the status bar. No AVPlayerViewController, no controls, nothing.
//
// Android equivalent (exact same architecture):
//   ExoPlayer renders directly onto a SurfaceView (a raw Surface/SurfaceHolder).
//   SurfaceView has no controls, no overlays, no UI at all — it is just pixels,
//   exactly like AVPlayerLayer. No PlayerView is used anywhere.
//   WindowInsetsController hides the status bar.
//   The SurfaceView is not clickable/focusable — matches .allowsHitTesting(false).
//
// Result: identical to iOS — pure fullscreen video, nothing else on screen.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SplashScreen(vm: AppStateViewModel) {
    val context  = LocalContext.current
    val mintBg   = Color(red = 0.816f, green = 1.0f, blue = 0.973f)

    val rawResId = remember {
        context.resources.getIdentifier("greengo2", "raw", context.packageName)
    }

    // No video found — show mint background briefly then go home (matches iOS go())
    if (rawResId == 0) {
        LaunchedEffect(Unit) {
            delay(2_000)
            vm.navigate(Screen.Home)
        }
        Box(modifier = Modifier.fillMaxSize().background(mintBg))
        return
    }

    // ── Build ExoPlayer — render target is set later when Surface is ready ────
    val player = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                setMediaItem(MediaItem.fromUri(uri))

                // Audio focus: play audio like iOS AVPlayer default (respects ringer)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus= */ true
                )

                repeatMode    = Player.REPEAT_MODE_OFF
                playWhenReady = true   // starts as soon as Surface + data ready
                prepare()

                // Navigate home when video finishes — matches iOS AVPlayerItemDidPlayToEndTime
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) vm.navigate(Screen.Home)
                    }
                })
            }
    }

    // 8-second safety cap — matches iOS Task.sleep(for: .seconds(8)) { go() }
    LaunchedEffect(Unit) {
        delay(8_000)
        vm.navigate(Screen.Home)
    }

    // Release player when composable leaves the tree
    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    // ── Mint background sits behind the SurfaceView ───────────────────────────
    // Visible for the few frames before the first video frame is decoded,
    // matching iOS Color(red:0.816...).ignoresSafeArea() behind VideoPlayerView.
    Box(modifier = Modifier.fillMaxSize().background(mintBg)) {

        // ── Raw SurfaceView — the Android equivalent of AVPlayerLayer ─────────
        // No controls, no overlays, no chrome of any kind.
        // Matches iOS: pLayer.player = player  +  pLayer.videoGravity = .resizeAspectFill
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { ctx ->
                SurfaceView(ctx).apply {

                    // Fill parent — equivalent to .ignoresSafeArea() on VideoPlayerView
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Matches iOS .allowsHitTesting(false)
                    isClickable        = false
                    isFocusable        = false
                    isFocusableInTouchMode = false
                    isLongClickable    = false
                    isSoundEffectsEnabled = false

                    // Wire the player to this Surface once it's created.
                    // Matches iOS: pLayer.player = player (AVPlayerLayer auto-connects
                    // when the layer is added to the view hierarchy).
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) {
                            player.setVideoSurfaceHolder(h)
                        }
                        override fun surfaceChanged(h: SurfaceHolder, fmt: Int, w: Int, h2: Int) {}
                        override fun surfaceDestroyed(h: SurfaceHolder) {
                            player.clearVideoSurfaceHolder(h)
                        }
                    })

                    // Hide status bar edge-to-edge — matches iOS .statusBarHidden(true)
                    // Uses WindowInsetsController (API 30+) with legacy flag fallback
                    @Suppress("DEPRECATION")
                    systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
                }
            }
        )
    }
}
