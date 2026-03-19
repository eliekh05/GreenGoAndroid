package com.greengo.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.layout.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.GreenPrimary
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MemCard model
// ─────────────────────────────────────────────────────────────────────────────

data class MemCard(
    val id: UUID = UUID.randomUUID(),
    val pairID: Int,
    val imgName: String,
    var flipped: Boolean = false,
    var matched: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WildRecallGame ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class WildRecallGame {
    companion object {
        val allPairs   = listOf("a1","a2","a3","a4","a5","a6","a7","a8")
        const val pairsPerGame = 6
    }

    var cards      by mutableStateOf<List<MemCard>>(emptyList())
    var score      by mutableStateOf(0)
    var matched    by mutableStateOf(0)
    var gameOver   by mutableStateOf(false)
    var totalPairs by mutableStateOf(pairsPerGame)

    private var flippedIDs = mutableListOf<UUID>()
    private var busy = false

    init { deal() }

    fun deal() {
        score = 0; matched = 0; gameOver = false
        flippedIDs.clear(); busy = false
        val chosen = allPairs.shuffled().take(pairsPerGame)
        totalPairs = chosen.size
        val deck = mutableListOf<MemCard>()
        chosen.forEachIndexed { i, name ->
            deck.add(MemCard(pairID = i, imgName = name))
            deck.add(MemCard(pairID = i, imgName = name))
        }
        cards = deck.shuffled()
    }

    fun tap(card: MemCard, scope: kotlinx.coroutines.CoroutineScope) {
        if (busy) return
        val idx = cards.indexOfFirst { it.id == card.id }
        if (idx < 0 || cards[idx].flipped || cards[idx].matched) return
        if (flippedIDs.size >= 2) return

        cards = cards.toMutableList().also { it[idx] = it[idx].copy(flipped = true) }
        flippedIDs.add(card.id)

        if (flippedIDs.size == 2) {
            busy = true
            scope.launch {
                delay(750)
                check()
            }
        }
    }

    private fun check() {
        val ids = flippedIDs.toList()
        val i0 = cards.indexOfFirst { it.id == ids[0] }
        val i1 = cards.indexOfFirst { it.id == ids[1] }
        if (i0 < 0 || i1 < 0) { flippedIDs.clear(); busy = false; return }

        val updated = cards.toMutableList()
        if (updated[i0].pairID == updated[i1].pairID) {
            updated[i0] = updated[i0].copy(matched = true)
            updated[i1] = updated[i1].copy(matched = true)
            score += 10
            matched += 1
            if (matched == totalPairs) gameOver = true
        } else {
            updated[i0] = updated[i0].copy(flipped = false)
            updated[i1] = updated[i1].copy(flipped = false)
            score = maxOf(0, score - 1)
        }
        cards = updated
        flippedIDs.clear()
        busy = false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WildRecallScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WildRecallScreen(vm: AppStateViewModel) {
    val theme by vm.theme.collectAsState()
    val ws = rememberWindowSize()
    val game  = remember { WildRecallGame() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(game.gameOver) {
        if (game.gameOver) vm.saveMemoryScore(game.score)
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // NavBar
            WildRecallNavBar(theme = theme,
                onBack = { vm.navigate(Screen.Games) },
                onHome = { vm.navigate(Screen.Home) })

            // Score bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ws.contentPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Score: ", fontSize = ws.bodySp.sp, color = theme.text)
                Text("${game.score}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.accent)
                Spacer(modifier = Modifier.weight(1f))
                Text("Pairs: ${game.matched}/${game.totalPairs}", fontSize = ws.captionSp.sp,
                    color = theme.text.copy(alpha = 0.6f))
            }

            // Card grid — 4 columns for 12 cards (6 pairs)
            LazyVerticalGrid(
                columns = GridCells.Fixed(ws.memoryCardColumns),
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(game.cards, key = { it.id }) { card ->
                    MemCardView(card = card, theme = theme) { game.tap(card, scope) }
                }
            }

            // New Game button
            Button(
                onClick = { game.deal() },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(ws.cardRadius),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("New Game", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = 4.dp))
            }
        }

        // Win overlay
        if (game.gameOver) {
            WinOverlay(
                score = game.score,
                theme = theme,
                onPlayAgain = { game.deal() },
                onGames     = { vm.navigate(Screen.Games) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MemCardView (3-D flip animation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MemCardView(card: MemCard, theme: AppTheme, onClick: () -> Unit) {
        val ws = rememberWindowSize()
        val context = LocalContext.current
    val faceResId = remember(card.imgName) {
        context.resources.getIdentifier(card.imgName, "drawable", context.packageName)
    }
    val backResId = remember {
        context.resources.getIdentifier("cardxxx", "drawable", context.packageName)
    }

    val rotation by animateFloatAsState(
        targetValue = if (card.flipped || card.matched) 0f else 180f,
        animationSpec = tween(durationMillis = 300),
        label = "card_flip"
    )
    val isFront = rotation < 90f

    Box(
        modifier = Modifier
            .size(ws.memoryCardSize)
            .graphicsLayer { rotationY = rotation }
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !card.flipped && !card.matched) { onClick() }
    ) {
        if (isFront) {
            // Face (animal image)
            if (faceResId != 0) {
                Image(
                    painter = painterResource(id = faceResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (card.matched) 0.65f else 1f },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(theme.accent.copy(alpha = 0.3f)))
            }
            // Matched border
            if (card.matched) {
                Box(modifier = Modifier.fillMaxSize()
                    .then(Modifier.clip(RoundedCornerShape(10.dp)))
                    .background(Color.Transparent))
                // Compose doesn't have a direct overlay border in Box — use Surface stroke workaround
            }
        } else {
            // Back
            if (backResId != 0) {
                Image(
                    painter = painterResource(id = backResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .background(Color(red = 0.70f, green = 0.18f, blue = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("?", fontSize = ws.navTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - WinOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WinOverlay(
    score: Int,
    theme: AppTheme,
    onPlayAgain: () -> Unit,
    onGames: () -> Unit
) {
    val ws = rememberWindowSize()
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(ws.cardInnerPadding * 2)
                .background(
                    if (theme == AppTheme.DARK) Color(0xFF1E1E1E).copy(alpha = 0.95f)
                    else theme.background.copy(alpha = 0.95f),
                    RoundedCornerShape(22.dp)
                )
                .padding(ws.cardInnerPadding * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Text("🎉 Congratulations!", fontSize = ws.headingSp.sp, fontWeight = FontWeight.Bold, color = theme.text)
            Text("Score: $score", fontSize = ws.statSp.sp, fontWeight = FontWeight.Bold, color = theme.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) { Text("Play Again", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)) }
                Button(
                    onClick = onGames,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) { Text("Games", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Wild Recall custom nav bar (uses "GameOfSquids" style title)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WildRecallNavBar(theme: AppTheme, onBack: () -> Unit, onHome: () -> Unit) {
    val ws = rememberWindowSize()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .statusBarsPadding()
                .padding(horizontal = ws.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(ws.navIconSize)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = theme.text, modifier = Modifier.size(ws.smallIconSize))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Wild Recall", fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onHome, modifier = Modifier.size(ws.navIconSize)) {
                Icon(Icons.Default.Home, contentDescription = "Home",
                    tint = theme.mutedText, modifier = Modifier.size(ws.smallIconSize))
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.6f))
    }
}
