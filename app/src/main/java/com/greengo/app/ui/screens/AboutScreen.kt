package com.greengo.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.layout.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.BluePrimary
import com.greengo.app.ui.components.GreenPrimary
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize

@Composable
fun AboutScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
    val theme by vm.theme.collectAsState()
        val context = LocalContext.current

    val sections = listOf(
        "What is Green Go?" to listOf(
            "Green Go is an eco-friendly travel companion designed to help tourists make sustainable choices wherever they are in the world.",
            "The app provides interactive maps of eco-friendly accommodations, cycling routes, and nature sites across every continent."
        ),
        "Our Mission" to listOf(
            "We believe travel should leave the world better than we found it.",
            "Green Go empowers travellers with tools, knowledge, and games that make sustainability fun and accessible."
        ),
        "Features" to listOf(
            "🗺️ Eco Map — 150+ vetted eco-friendly locations worldwide",
            "🌐 Translator — Speak or type in 20+ languages",
            "🚶 Pedometer — Track steps, distance and time",
            "🎮 Games — Wild Recall, Reef Rescuers, Eco Trivia",
            "📖 Eco Tips — Learn while exploring"
        ),
        "Responsible Tourism" to listOf(
            "Support local businesses over large chains.",
            "Use public transportation or cycling whenever possible.",
            "Observe wildlife from a safe distance — never feed animals.",
            "Carry reusable items and avoid single-use plastics.",
            "Leave every place cleaner than you found it."
        )
    )

    val logoResId = remember {
        context.resources.getIdentifier("greengologocopy", "drawable", context.packageName)
    }

    Scaffold(
        topBar = {
            NavBar(
                title = "About",
                onBack = { vm.navigate(Screen.Home) },
                onHome = { vm.navigate(Screen.Home) },
                theme = theme
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
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Header: logo + GreenGo wordmark + version
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (logoResId != 0) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = "GreenGo logo",
                        modifier = Modifier
                            .size(ws.logoSize)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    Text("Green", color = GreenPrimary, fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Go",    color = BluePrimary,  fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(text = "Version 1.0", fontSize = ws.captionSp.sp, color = theme.mutedText)
            }

            Spacer(modifier = Modifier.height(20.dp))

            sections.forEach { (heading, lines) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(ws.cardRadius), clip = false)
                        .background(theme.cardBackground, RoundedCornerShape(ws.cardRadius))
                        .padding(ws.cardInnerPadding),
                    verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                ) {
                    Text(
                        text = heading,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                    lines.forEach { line ->
                        Text(text = line, fontSize = ws.bodySp.sp, color = theme.text)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
