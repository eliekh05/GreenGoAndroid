package com.greengo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import androidx.compose.ui.unit.IntOffset
import com.greengo.app.ui.components.BackBtn
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize
import kotlinx.coroutines.isActive

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Sprite model
// ─────────────────────────────────────────────────────────────────────────────

data class OceanSprite(
    val id: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val size: Float = 50f,
    val visible: Boolean = true
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - ReefRescuersGame  (mirrors iOS ReefRescuersGame)
// Uses simple setter lambdas so Compose state observes each sprite field.
// ─────────────────────────────────────────────────────────────────────────────

class ReefRescuersGame {
    var score    by mutableStateOf(0)
    var running  by mutableStateOf(false)
    var gameOver by mutableStateOf(false)

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
    private val binSize   = 60f
    private val dropSpeed = 180f  // pixels per second (180 px/s = 3 px/frame @ 60 fps)

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

    /** Called by drag gesture: moves bin horizontally to touch point. */
    fun dragBin(toX: Float) {
        if (!running) return
        recyclingBin = recyclingBin.copy(
            x = (toX - binSize / 2f).coerceIn(0f, canvasWidth - binSize),
            y = binY
        )
    }

    /** One physics tick — called every Vsync frame; deltaSeconds is time since last frame. */
    fun tick(deltaSeconds: Float) {
        if (!running) return

        // Unlock sprite types progressively (mirrors iOS score thresholds)
        if (score > 1  && !chipsbag.visible)     chipsbag     = chipsbag.copy(visible = true)
        if (score > 8  && !yellowbottle.visible) yellowbottle = yellowbottle.copy(visible = true)
        if (score > 20 && !oldshoes.visible)     oldshoes     = oldshoes.copy(visible = true)

        // Drop all visible trash + fish
        if (greenplasticbag.visible) greenplasticbag = greenplasticbag.copy(y = greenplasticbag.y + dropSpeed * deltaSeconds)
        if (chipsbag.visible)        chipsbag        = chipsbag.copy(y = chipsbag.y + dropSpeed * deltaSeconds)
        if (oldshoes.visible)        oldshoes        = oldshoes.copy(y = oldshoes.y + dropSpeed * deltaSeconds)
        if (yellowbottle.visible)    yellowbottle    = yellowbottle.copy(y = yellowbottle.y + dropSpeed * deltaSeconds)
        if (tire.visible)            tire            = tire.copy(y = tire.y + dropSpeed * deltaSeconds)
        if (fish.visible)            fish            = fish.copy(y = fish.y + dropSpeed * deltaSeconds)

        // Pin platform and bin
        platform     = platform.copy(y = platformY)
        recyclingBin = recyclingBin.copy(y = binY)

        // Collision
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
// MARK: - OceanGameScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OceanGameScreen(vm: AppStateViewModel) {
        val ws = rememberWindowSize()
        val theme = vm.theme.collectAsState().value
    val game  = remember { ReefRescuersGame() }
    val density = LocalDensity.current

    // Display-sync game loop via withFrameNanos — runs at the device's native
    // refresh rate (60 / 90 / 120 / 144 Hz) with zero busy-waiting or delay().
    LaunchedEffect(game.running) {
        if (!game.running) return@LaunchedEffect
        var lastNanos = 0L
        while (isActive && game.running) {
            withFrameNanos { frameNanos ->
                val deltaSeconds = if (lastNanos == 0L) 0f
                                   else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastNanos = frameNanos
                game.tick(deltaSeconds)
            }
        }
    }

    // Persist best score when game ends
    LaunchedEffect(game.gameOver) {
        if (game.gameOver) vm.saveOceanScore(game.score)
    }

    DisposableEffect(Unit) { onDispose { game.stop() } }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── HUD bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(red = 0.03f, green = 0.10f, blue = 0.28f))
                .statusBarsPadding()
                .padding(horizontal = ws.contentPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (game.running) game.reset() else game.start() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.85f)),
                contentPadding = PaddingValues(horizontal = ws.contentPadding, vertical = ws.buttonVPadding)
            ) {
                Text(
                    text = if (game.running) "Reset" else "Start",
                    fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold, color = Color.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Score", fontSize = ws.captionSp.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                Text("${game.score}", fontSize = ws.navTitleSp.sp, fontWeight = FontWeight.Bold,
                    color = Color(red = 1f, green = 0.85f, blue = 0f))
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = { game.stop(); vm.navigate(Screen.Games) }) {
                Icon(Icons.Default.Home, contentDescription = "Home",
                    tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        // ── Canvas area ───────────────────────────────────────────────────────
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size != canvasSize) {
                        canvasSize = size
                        with(density) {
                            game.initCanvas(size.width.toFloat(), size.height.toFloat())
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> game.dragBin(change.position.x) }
                }
        ) {
            val context = LocalContext.current

            // Ocean background
            val bgResId = remember {
                context.resources.getIdentifier("ocean", "drawable", context.packageName)
            }
            if (bgResId != 0) {
                Image(painter = painterResource(id = bgResId), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxSize()
                    .background(Color(red = 0.03f, green = 0.25f, blue = 0.60f)))
            }

            // Falling sprites + platform + bin
            SpriteView(game.tire,            context)
            SpriteView(game.chipsbag,         context)
            SpriteView(game.greenplasticbag,  context)
            SpriteView(game.oldshoes,         context)
            SpriteView(game.yellowbottle,     context)
            SpriteView(game.fish,             context)
            PlatformSpriteView(game.platform, context)
            BinSpriteView(game.recyclingBin,  context)

            // Game Over overlay
            if (game.gameOver) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                    ) {
                        Text("Game Over", fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Score: ${game.score}", fontSize = ws.statSp.sp, fontWeight = FontWeight.Bold,
                            color = Color(red = 1f, green = 0.85f, blue = 0f))
                        Row(horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
                            Button(onClick = { game.reset() }, shape = RoundedCornerShape(ws.cardRadius),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(red = 0.08f, green = 0.52f, blue = 0.12f))
                            ) {
                                Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                            }
                            Button(onClick = { vm.navigate(Screen.Games) }, shape = RoundedCornerShape(ws.cardRadius),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.7f))
                            ) {
                                Text("Exit", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White, modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                    }
                }
            }

            // Start prompt (before first tap)
            if (!game.running && !game.gameOver) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { game.start() },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                        contentPadding = PaddingValues(horizontal = ws.contentPadding * 2.75f, vertical = ws.buttonVPadding)
                    ) {
                        Text("Start", fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Sprite composables (pixel-positioned via offset)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpriteView(sprite: OceanSprite, context: android.content.Context) {
    if (!sprite.visible) return
    val resId = remember(sprite.id) {
        context.resources.getIdentifier(sprite.id, "drawable", context.packageName)
    }
    val density = LocalDensity.current
    with(density) {
        Box(modifier = Modifier
            .offset(x = sprite.x.toDp(), y = sprite.y.toDp())
            .size(sprite.size.toDp())
        ) {
            if (resId != 0) {
                Image(painter = painterResource(id = resId), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                    .background(Color(1f, 0.65f, 0f, 0.7f)))   // orange fallback
            }
        }
    }
}

@Composable
private fun PlatformSpriteView(sprite: OceanSprite, context: android.content.Context) {
    val resId = remember { context.resources.getIdentifier(sprite.id, "drawable", context.packageName) }
    if (resId == 0) return
    val density = LocalDensity.current
    Box(modifier = Modifier
        .offset(x = 0.dp, y = with(density) { sprite.y.toDp() })
        .fillMaxWidth()
        .height(20.dp)
    ) {
        Image(painter = painterResource(id = resId), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
}

@Composable
private fun BinSpriteView(sprite: OceanSprite, context: android.content.Context) {
    val resId = remember { context.resources.getIdentifier(sprite.id, "drawable", context.packageName) }
    val density = LocalDensity.current
    Box(modifier = Modifier
        .offset(x = with(density) { sprite.x.toDp() }, y = with(density) { sprite.y.toDp() })
        .size(60.dp)
    ) {
        if (resId != 0) {
            Image(painter = painterResource(id = resId), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                .background(Color.Green.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center) {
                Text("BIN", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - OceanInfoScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OceanInfoScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            BackBtn(onClick = { vm.navigate(Screen.Games) })

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
            ) {
                if (iconResId != 0) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(ws.tileIconSize).clip(RoundedCornerShape(50)),
                        contentScale = ContentScale.Crop
                    )
                }
                Text("Reef Rescuers", fontSize = ws.headingSp.sp, fontWeight = FontWeight.Bold, color = theme.accent)
            }

            Text(
                "The oceans are polluted. Your goal is to clean out as much trash as possible without catching the fish.",
                fontSize = ws.bodySp.sp,
                color = theme.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(red = 0.04f, green = 0.32f, blue = 0.70f).copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(ws.cardInnerPadding)
            )

            Text("How to Play", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = theme.text)

            listOf(
                "Drag the recycling bin left and right to catch falling trash.",
                "Catch chips bags, plastic bags, bottles, tyres, and shoes to score.",
                "More trash types unlock as your score grows.",
                "DON'T catch the fish — it ends the game immediately!"
            ).forEach { rule ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    Text("✅", fontSize = ws.bodySp.sp)
                    Text(rule, fontSize = ws.bodySp.sp, color = theme.text)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Don't show this again", fontSize = ws.bodySp.sp, color = theme.text)
                Switch(checked = skip, onCheckedChange = { skip = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.accent))
            }

            Button(
                onClick = { vm.setSkipOceanInfo(skip); vm.navigate(Screen.Ocean) },
                modifier = Modifier.fillMaxWidth().height(ws.buttonHeight),
                shape = RoundedCornerShape(ws.cardRadius),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("Play Now!", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
