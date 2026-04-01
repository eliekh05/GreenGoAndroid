package com.greengo.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.rememberWindowSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
// MARK: - WildRecallGame  (plain class held in remember{} — mirrors iOS @StateObject)
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

    // Use mutableStateListOf so Compose observes changes — plain mutableListOf causes stale reads
    private val flippedIDs = androidx.compose.runtime.mutableStateListOf<UUID>()
    @Volatile private var busy = false

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
                evaluatePair()
            }
        }
    }

    private fun evaluatePair() {
        val ids = flippedIDs.toList()
        if (ids.size < 2) { flippedIDs.clear(); busy = false; return }
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
        } else {
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
// MARK: - Wild Recall custom nav bar
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
