package com.greengo.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.automirrored.filled.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.BackBtn
import com.greengo.app.ui.components.rememberWindowSize
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Sprite model  (mirrors iOS OceanSprite)
// ─────────────────────────────────────────────────────────────────────────────

data class OceanSprite(
    val id: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val size: Float = 80f,        // 80dp — matches iOS .frame(width:80,height:80)
    val visible: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - ReefRescuersGame  (faithful to iOS / AIA logic)
// Uses mutableStateOf per sprite — Compose observes each independently.
// Timer-based tick at 60fps mirrors iOS Timer.scheduledTimer(1/60).
// ─────────────────────────────────────────────────────────────────────────────

class ReefRescuersGame {
    var score    by mutableStateOf(0)
    var running  by mutableStateOf(false)
    var gameOver by mutableStateOf(false)

    // Sprites — initial positions match iOS AIA SCM
    var chipsbag        by mutableStateOf(OceanSprite("chips",           visible = false))
    var greenplasticbag by mutableStateOf(OceanSprite("greenplasticbag", visible = true))
    var oldshoes        by mutableStateOf(OceanSprite("oldshoes",        visible = false))
    var yellowbottle    by mutableStateOf(OceanSprite("yellowbottle",    visible = false))
    var fish            by mutableStateOf(OceanSprite("fish",            visible = true))
    var tire            by mutableStateOf(OceanSprite("cartire",         visible = true))
    var platform        by mutableStateOf(OceanSprite("oceanplatform",   visible = true))
    var recyclingBin    by mutableStateOf(OceanSprite("recyclebin",      visible = true))

    var canvasWidth:  Float = 390f
    var canvasHeight: Float = 700f

    private val platformH = 20f
    private val binSize   = 110f   // matches iOS 110pt bin width

    private val platformY get() = canvasHeight - platformH
    private val binY      get() = platformY - binSize

    fun initCanvas(w: Float, h: Float) {
        canvasWidth  = w
        canvasHeight = h
        platform     = platform.copy(x = 0f, y = platformY)
        recyclingBin = recyclingBin.copy(x = w / 2f - binSize / 2f, y = binY)
    }

    fun start() {
        if (running) return
        score = 0; gameOver = false; running = true
        spawnAll()
    }

    fun reset() {
        score = 0; gameOver = false
        spawnAll()
        running = true
    }

    fun stop() { running = false }

    fun dragBin(toX: Float) {
        if (!running) return
        recyclingBin = recyclingBin.copy(
            x = (toX - binSize / 2f).coerceIn(0f, canvasWidth - binSize),
            y = binY
        )
    }

    // Called every vsync frame — deltaSeconds keeps speed frame-rate independent
    // iOS uses 3px per frame @ 60fps = 180px/s
    fun tick(deltaSeconds: Float) {
        if (!running) return

        // Unlock sprites progressively (mirrors iOS score thresholds)
        if (score > 1  && !chipsbag.visible)     chipsbag     = chipsbag.copy(visible = true)
        if (score > 8  && !yellowbottle.visible) yellowbottle = yellowbottle.copy(visible = true)
        if (score > 20 && !oldshoes.visible)     oldshoes     = oldshoes.copy(visible = true)

        val speed = 180f * deltaSeconds   // 3px/frame @ 60fps

        if (greenplasticbag.visible) greenplasticbag = greenplasticbag.copy(y = greenplasticbag.y + speed)
        if (chipsbag.visible)        chipsbag        = chipsbag.copy(y = chipsbag.y + speed)
        if (oldshoes.visible)        oldshoes        = oldshoes.copy(y = oldshoes.y + speed)
        if (yellowbottle.visible)    yellowbottle    = yellowbottle.copy(y = yellowbottle.y + speed)
        if (tire.visible)            tire            = tire.copy(y = tire.y + speed)
        if (fish.visible)            fish            = fish.copy(y = fish.y + speed)

        platform     = platform.copy(y = platformY)
        recyclingBin = recyclingBin.copy(y = binY)

        hitTest(chipsbag,        isFish = false) { chipsbag        = it }
        hitTest(greenplasticbag, isFish = false) { greenplasticbag = it }
        hitTest(oldshoes,        isFish = false) { oldshoes        = it }
        hitTest(yellowbottle,    isFish = false) { yellowbottle    = it }
        hitTest(tire,            isFish = false) { tire            = it }
        hitTest(fish,            isFish = true)  { fish            = it }
    }

    private fun hitTest(s: OceanSprite, isFish: Boolean, setter: (OceanSprite) -> Unit) {
        if (!s.visible) return
        if (s.y + s.size >= platformY) {
            setter(respawn(s)); return
        }
        if (overlaps(s, recyclingBin)) {
            if (isFish) {
                running = false; gameOver = true
                setter(s.copy(y = -200f))
            } else {
                score += 1
                setter(respawn(s))
            }
        }
    }

    private fun respawn(s: OceanSprite): OceanSprite {
        val newX = (Math.random() * maxOf(1.0, (canvasWidth - s.size).toDouble())).toFloat()
        return s.copy(x = newX, y = -s.size)
    }

    private fun overlaps(a: OceanSprite, b: OceanSprite): Boolean {
        if (!a.visible || !b.visible) return false
        return a.x < b.x + b.size && a.x + a.size > b.x &&
               a.y < b.y + b.size && a.y + a.size > b.y
    }

    private fun spawnAll() {
        greenplasticbag = respawn(greenplasticbag).copy(visible = true)
        tire            = respawn(tire).copy(visible = true)
        fish            = respawn(fish).copy(visible = true)
        chipsbag        = respawn(chipsbag).copy(visible = false)
        yellowbottle    = respawn(yellowbottle).copy(visible = false)
        oldshoes        = respawn(oldshoes).copy(visible = false)
        platform        = platform.copy(x = 0f, y = platformY)
        recyclingBin    = recyclingBin.copy(x = canvasWidth / 2f - binSize / 2f, y = binY)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - OceanInfoScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OceanInfoScreen(vm: AppStateViewModel) {
    val theme by vm.theme.collectAsState()
    val context = LocalContext.current
    var skip by remember { mutableStateOf(false) }

    val iconResId = remember {
        context.resources.getIdentifier("oceangameicon", "drawable", context.packageName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BackBtn(onClick = { vm.navigate(Screen.Games) })

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (iconResId != 0) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
                Text("Reef Rescuers", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = theme.accent)
            }

            Text(
                "The oceans are polluted. Your goal is to clean out as much trash as possible without killing the fish.",
                fontSize = 16.sp, color = theme.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(red = 0.04f, green = 0.32f, blue = 0.70f, alpha = 0.12f),
                        RoundedCornerShape(12.dp))
                    .padding(14.dp)
            )

            Text("How to Play", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.text)

            listOf(
                "Drag the recycling bin left and right to catch falling trash.",
                "Catch chips bags, plastic bags, bottles, tyres, and shoes to score.",
                "More trash types unlock as your score grows.",
                "DON'T catch the fish — it ends the game immediately!"
            ).forEach { rule ->
                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = theme.accent,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp))
                    Text(rule, fontSize = 15.sp, color = theme.text)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Don't show this again", fontSize = 15.sp, color = theme.text)
                Switch(checked = skip, onCheckedChange = { skip = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.accent))
            }

            Button(
                onClick = { vm.setSkipOceanInfo(skip); vm.navigate(Screen.Ocean) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Play Now!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - OceanGameScreen  (mirrors iOS OceanGameView exactly)
// Key fixes vs previous version:
//   • ignoresSafeArea on the whole screen — no nav bar insets eating canvas height
//   • Canvas height = total height minus HUD height only
//   • Platform + bin Y computed from canvas height, not screen height
//   • navigationBarsPadding on HUD bar so buttons aren't behind Samsung nav bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OceanGameScreen(vm: AppStateViewModel) {
    val game = remember { ReefRescuersGame() }

    // Game loop — mirrors iOS Timer.scheduledTimer(1/60)
    LaunchedEffect(game.running) {
        if (!game.running) return@LaunchedEffect
        var lastTime = System.nanoTime()
        while (isActive && game.running) {
            awaitFrame()
            val now   = System.nanoTime()
            val delta = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime  = now
            game.tick(delta)
        }
    }

    DisposableEffect(Unit) { onDispose { game.stop() } }

    LaunchedEffect(game.gameOver) {
        if (game.gameOver) vm.saveOceanScore(game.score)
    }

    val hudBg = Color(red = 0.03f, green = 0.10f, blue = 0.28f)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalW = constraints.maxWidth.toFloat()
        val totalH = constraints.maxHeight.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {

            // ── HUD bar ── mirrors iOS HStack at top
            // navigationBarsPadding NOT here (it's at bottom) — statusBarsPadding only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hudBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start / Reset button
                Button(
                    onClick = { if (game.running) game.reset() else game.start() },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.85f)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.width(80.dp).height(36.dp)
                ) {
                    Text(
                        if (game.running) "Reset" else "Start",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Score", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f))
                    Text("${game.score}", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = Color(red = 1f, green = 0.85f, blue = 0f))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Home button
                IconButton(onClick = { game.stop(); vm.navigate(Screen.Games) }) {
                    Icon(Icons.Default.Home, contentDescription = "Home",
                        tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }

            // ── Canvas ── fills all remaining space above nav bar
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Add bottom padding equal to nav bar height so bin is never behind Samsung nav
                    .navigationBarsPadding()
            ) {
                val canvasW = constraints.maxWidth.toFloat()
                val canvasH = constraints.maxHeight.toFloat()

                // Init canvas once dimensions are known
                LaunchedEffect(canvasW, canvasH) {
                    if (canvasW > 0f && canvasH > 0f) {
                        game.initCanvas(canvasW, canvasH)
                    }
                }

                // Ocean background
                val context = LocalContext.current
                val oceanResId = remember {
                    context.resources.getIdentifier("ocean", "drawable", context.packageName)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(red = 0.03f, green = 0.25f, blue = 0.60f))
                ) {
                    if (oceanResId != 0) {
                        Image(
                            painter = painterResource(id = oceanResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Drag surface — full canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Track absolute finger position across whole screen
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                game.dragBin(down.position.x)
                                var currentX = down.position.x
                                var pointerDown = true
                                while (pointerDown) {
                                    val event = awaitPointerEvent()
                                    val drag = event.changes.firstOrNull()
                                    if (drag != null) {
                                        currentX = drag.position.x
                                        game.dragBin(currentX)
                                        drag.consume()
                                    }
                                    pointerDown = event.changes.any { it.pressed }
                                }
                            }
                        }
                )

                // Platform — full width, 20dp tall, pinned to bottom
                val platResId = remember {
                    context.resources.getIdentifier("oceanplatform", "drawable", context.packageName)
                }
                if (platResId != 0) {
                    Image(
                        painter = painterResource(id = platResId),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .align(Alignment.BottomCenter),
                        contentScale = ContentScale.FillBounds
                    )
                }

                // Falling sprites
                SpriteView(game.tire,            context)
                SpriteView(game.chipsbag,        context)
                SpriteView(game.greenplasticbag, context)
                SpriteView(game.oldshoes,        context)
                SpriteView(game.yellowbottle,    context)
                SpriteView(game.fish,            context)

                // Recycling bin
                BinView(game.recyclingBin, context)

                // Game Over overlay
                if (game.gameOver) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text("Game Over", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                                color = Color.White)
                            Text("Score: ${game.score}", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                color = Color(red = 1f, green = 0.85f, blue = 0f))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = { game.reset() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(red = 0.08f, green = 0.52f, blue = 0.12f)),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                        color = Color.White)
                                }
                                Button(
                                    onClick = { vm.navigate(Screen.Games) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray.copy(alpha = 0.7f)),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text("Exit", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                        color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Start prompt
                if (!game.running && !game.gameOver) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { game.start() },
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 44.dp, vertical = 18.dp)
                        ) {
                            Text("Start", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Sprite composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.SpriteView(s: OceanSprite, context: android.content.Context) {
    if (!s.visible) return
    val resId = remember(s.id) {
        context.resources.getIdentifier(s.id, "drawable", context.packageName)
    }
    val sizeDp = with(LocalDensity.current) { s.size.toDp() }
    Box(
        modifier = Modifier
            .offset { IntOffset(s.x.toInt(), s.y.toInt()) }
            .size(sizeDp)
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color(1f, 0.65f, 0f), RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun BoxScope.BinView(s: OceanSprite, context: android.content.Context) {
    val resId = remember(s.id) {
        context.resources.getIdentifier(s.id, "drawable", context.packageName)
    }
    val sizeDp = with(LocalDensity.current) { s.size.toDp() }
    Box(
        modifier = Modifier
            .offset { IntOffset(s.x.toInt(), s.y.toInt()) }
            .size(sizeDp)
    ) {
        if (resId != 0) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()
                .background(Color.Green.copy(alpha = 0.8f), RoundedCornerShape(6.dp))) {
                Text("BIN", color = Color.White, fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
