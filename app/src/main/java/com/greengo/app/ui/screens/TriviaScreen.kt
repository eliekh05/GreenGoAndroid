package com.greengo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.data.TriviaAnswer
import com.greengo.app.data.TriviaQuestion
import com.greengo.app.data.allTriviaQuestions

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TriviaScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TriviaScreen(vm: AppStateViewModel) {
    val theme by vm.theme.collectAsState()

    // Game state — rebuilt on each new game (matching iOS buildGame())
    var questions    by remember { mutableStateOf<List<TriviaQuestion>>(emptyList()) }
    var answerOrders by remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var qIndex       by remember { mutableStateOf(0) }
    var score        by remember { mutableStateOf(0) }
    var chosen       by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }

    // Build game on first composition
    LaunchedEffect(Unit) {
        val shuffled = allTriviaQuestions.shuffled()
        val orders = shuffled.map { q -> (0 until q.answers.size).toList().shuffled() }
        questions    = shuffled
        answerOrders = orders
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(theme.background),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }

    val q            = questions[qIndex]
    val currentOrder = answerOrders[qIndex]
    val shuffledCorrectIdx = currentOrder.indexOf(q.correctIndex)

    fun advance() {
        if (qIndex < questions.size - 1) {
            qIndex += 1; chosen = null; showFeedback = false
        } else {
            vm.saveTriviaScore(score)
            vm.navigate(Screen.TriviaScore(score))
        }
    }

    Scaffold(
        topBar = {
            TriviaNavBar(
                qIndex   = qIndex,
                total    = questions.size,
                score    = score,
                theme    = theme,
                onBack   = { vm.navigate(Screen.Games) }
            )
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(ws.cardInnerPadding)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            // Question card
            Text(
                text = q.question,
                fontSize = ws.bodySp.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = theme.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.background.copy(alpha = 0.6f), RoundedCornerShape(ws.cardRadius))
                    .padding(ws.cardInnerPadding)
            )

            // Shuffled answer cards
            currentOrder.forEachIndexed { shuffledIdx, originalIdx ->
                AnswerCard(
                    ans             = q.answers[originalIdx],
                    shuffledIdx     = shuffledIdx,
                    shuffledCorrect = shuffledCorrectIdx,
                    chosen          = chosen,
                    showFeedback    = showFeedback,
                    theme           = theme
                ) {
                    if (!showFeedback) {
                        chosen = shuffledIdx
                        showFeedback = true
                        if (shuffledIdx == shuffledCorrectIdx) score += 10
                    }
                }
            }

            // Feedback + Next button
            if (showFeedback) {
                val isCorrect = chosen == shuffledCorrectIdx
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.background.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(ws.cardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isCorrect) "✅" else "❌", fontSize = ws.bodySp.sp)
                        Text(
                            if (isCorrect) "Correct!" else "Incorrect",
                            fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold,
                            color = if (isCorrect) Color.Green else Color.Red
                        )
                    }
                    // Explanation for whichever answer was tapped
                    val tappedOriginalIdx = currentOrder[chosen!!]
                    Text(
                        q.answers[tappedOriginalIdx].explanation,
                        fontSize = ws.bodySp.sp,
                        color = theme.text.copy(alpha = 0.9f)
                    )
                }

                Button(
                    onClick = { advance() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ws.cardRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) {
                    Text(
                        if (qIndex < questions.size - 1) "Next Question" else "See Results",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - AnswerCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnswerCard(
    ans: TriviaAnswer,
    shuffledIdx: Int,
    shuffledCorrect: Int,
    chosen: Int?,
    showFeedback: Boolean,
    theme: AppTheme,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val resId = remember(ans.imageName) {
        context.resources.getIdentifier(ans.imageName, "drawable", context.packageName)
    }

    val isChosen  = chosen == shuffledIdx
    val isCorrect = shuffledIdx == shuffledCorrect

    val tint = when {
        !showFeedback -> theme.text
        isChosen && isCorrect -> Color.Green
        isChosen -> Color.Red
        isCorrect -> Color.Green
        else -> theme.text.copy(alpha = 0.5f)
    }

    Surface(
        onClick = { if (!showFeedback) onClick() },
        enabled = !showFeedback,
        shape = RoundedCornerShape(12.dp),
        color = if (isChosen) tint.copy(alpha = 0.35f) else theme.background.copy(alpha = 0.12f),
        border = if (isChosen) ButtonDefaults.outlinedButtonBorder else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    modifier = Modifier.size(ws.tileIconSize * 0.88f).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(ws.tileIconSize).background(tint.copy(alpha = 0.3f), RoundedCornerShape(8.dp)))
            }
            Text(
                text = ans.text,
                fontSize = ws.bodySp.sp,
                fontWeight = FontWeight.Bold,
                color = theme.text,
                modifier = Modifier.weight(1f)
            )
            if (showFeedback) {
                Text(
                    text = when {
                        isCorrect -> "✅"
                        isChosen  -> "❌"
                        else      -> ""
                    },
                    fontSize = ws.cardTitleSp.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TriviaNavBar (custom — includes score display + progress dots)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TriviaNavBar(
    qIndex: Int,
    total: Int,
    score: Int,
    theme: AppTheme,
    onBack: () -> Unit
) {
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
            Text("Eco Trivia", fontSize = ws.navTitleSp.sp, fontWeight = FontWeight.Bold, color = theme.text)
            Spacer(modifier = Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Score", fontSize = 10.sp, color = theme.text.copy(alpha = 0.8f))
                Text("$score", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = theme.accent)
            }
        }

        // Progress dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background.copy(alpha = 0.9f))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(total) { i ->
                val size = if (i == qIndex) 10.dp else 7.dp
                val color = when {
                    i < qIndex  -> theme.accent
                    i == qIndex -> theme.text
                    else        -> theme.text.copy(alpha = 0.35f)
                }
                Box(modifier = Modifier.padding(horizontal = 3.dp).size(size)
                    .clip(RoundedCornerShape(50)).background(color))
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.6f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TriviaScoreScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TriviaScoreScreen(vm: AppStateViewModel, score: Int) {
    val green = Color(red = 0.08f, green = 0.42f, blue = 0.14f)
    val maxScore = allTriviaQuestions.size * 10
    val context = LocalContext.current
    val logoResId = remember {
        context.resources.getIdentifier("greengologocopy", "drawable", context.packageName)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(green),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                if (logoResId != 0) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = null,
                        modifier = Modifier.size(ws.logoSize).clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Text("Quiz Complete!", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Column(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(ws.cardRadius))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Your Score", fontSize = ws.bodySp.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("$score / $maxScore", fontSize = 52.sp, fontWeight = FontWeight.Bold,
                        color = Color(red = 0.97f, green = 0.91f, blue = 0.35f))
                }

                Text(
                    text = when {
                        score == maxScore    -> "Perfect score! 🌍"
                        score >= maxScore / 2 -> "Great job! Keep learning! 🌿"
                        else                 -> "Nice try! Play again to improve 💪"
                    },
                    fontSize = ws.bodySp.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
            ) {
                Button(
                    onClick = { vm.navigate(Screen.Trivia) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ws.cardRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(red = 0.97f, green = 0.91f, blue = 0.35f))
                ) {
                    Text("Play Again", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                Button(
                    onClick = { vm.navigate(Screen.Games) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ws.cardRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f))
                ) {
                    Text("Games", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                TextButton(onClick = { vm.navigate(Screen.Home) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Home", fontSize = ws.bodySp.sp, color = Color.White.copy(alpha = 0.75f))
                }
            }
        }
    }
}
