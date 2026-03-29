package com.greengo.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.layout.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.BackBtn
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
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
// MARK: - GamesScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GamesScreen(vm: AppStateViewModel) {
        val ws = rememberWindowSize()
        val theme       by vm.theme.collectAsState()
    val memoryScore by vm.memoryScore.collectAsState()
    val oceanScore  by vm.oceanScore.collectAsState()
    val triviaScore by vm.triviaScore.collectAsState()
    val skipMemory  by vm.skipMemoryInfo.collectAsState()
    val skipOcean   by vm.skipOceanInfo.collectAsState()

    Scaffold(
        topBar = {
            NavBar(
                title  = "Games",
                onBack = { vm.navigate(Screen.Home) },
                onHome = { vm.navigate(Screen.Home) },
                theme  = theme
            )
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ws.contentPadding)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Learn eco-facts while having fun!",
                fontSize = ws.bodySp.sp,
                color = theme.text.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
            )

            GameCard(
                title       = "Wild Recall",
                sub         = "Match animal pairs — eco memory game",
                bannerImage = "a1",
                color       = Color(red = 0.70f, green = 0.18f, blue = 0.18f),
                score       = memoryScore,
                theme       = theme
            ) { vm.navigate(if (skipMemory) Screen.WildRecall else Screen.MemoryInfo) }

            GameCard(
                title       = "Reef Rescuers",
                sub         = "Catch falling trash before it hits the ocean",
                bannerImage = "oceangameicon",
                color       = Color(red = 0.04f, green = 0.32f, blue = 0.70f),
                score       = oceanScore,
                theme       = theme
            ) { vm.navigate(if (skipOcean) Screen.Ocean else Screen.OceanInfo) }

            GameCard(
                title       = "Eco Trivia",
                sub         = "5-round quiz on sustainable travel",
                bannerImage = "europetrafic",
                color       = Color(red = 0.08f, green = 0.42f, blue = 0.14f),
                score       = triviaScore,
                theme       = theme
            ) { vm.navigate(Screen.Trivia) }
        }
    }
}

@Composable
private fun GameCard(
    title: String,
    sub: String,
    bannerImage: String,
    color: Color,
    score: Int,
    theme: AppTheme,
    onClick: () -> Unit
) {
    val ws = rememberWindowSize()
        val context = LocalContext.current
    val imageResId = remember(bannerImage) {
        context.resources.getIdentifier(bannerImage, "drawable", context.packageName)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ws.cardRadius),
        color = theme.cardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(ws.cardRadius), ambientColor = color.copy(alpha = 0.14f), spotColor = color.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ws.gameBannerHeight)
                    .clip(RoundedCornerShape(topStart = ws.cardRadius, topEnd = ws.cardRadius))
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(color.copy(alpha = 0.25f)))
                }
            }

            Row(
                modifier = Modifier.padding(ws.cardInnerPadding).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(sub, fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.7f))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Best", fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.6f))
                    Text("$score", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MemoryInfoScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MemoryInfoScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
    val theme by vm.theme.collectAsState()
        val context = LocalContext.current

    val facts = listOf(
        "a1" to "Cheetahs are harmed by habitat fragmentation and illegal wildlife tourism.",
        "a2" to "Elephants face stress and conflict when tourism damages natural habitats.",
        "a3" to "Giraffes are affected when travel activity disrupts feeding and breeding patterns.",
        "a4" to "Hippos are threatened by polluted waterways and habitat disturbance.",
        "a5" to "Gorillas are vulnerable to habitat pressure from irresponsible tours.",
        "a6" to "Lions are impacted by habitat loss, poaching pressure, and disruption.",
        "a7" to "Rhinos face rising poaching risk where tourism is unmanaged.",
        "a8" to "Sea turtles suffer from litter, light pollution, and nesting disruption."
    )

    var skip by remember { mutableStateOf(false) }

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
                .padding(ws.cardInnerPadding)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            BackBtn(onClick = { vm.navigate(Screen.Games) })

            Text("Wild Recall", fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold, color = theme.accent)

            Text(
                "While traveling unsustainably across the world, many animals face severe consequences. Learn and then play.",
                fontSize = 18.sp,
                color = theme.text
            )

            facts.forEach { (imgName, factText) ->
                val resId = context.resources.getIdentifier(imgName, "drawable", context.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.cardBackground, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                ) {
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(factText, fontSize = ws.bodySp.sp, color = theme.text)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Don't show this again", fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold, color = theme.text)
                Switch(checked = skip, onCheckedChange = { skip = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = theme.accent))
            }

            Button(
                onClick = {
                    vm.setSkipMemoryInfo(skip)
                    vm.navigate(Screen.WildRecall)
                },
                modifier = Modifier.fillMaxWidth().height(ws.buttonHeight),
                shape = RoundedCornerShape(ws.cardRadius),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("Continue", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
