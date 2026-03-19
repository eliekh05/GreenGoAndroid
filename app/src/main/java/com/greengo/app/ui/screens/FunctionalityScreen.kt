package com.greengo.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.layout.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize

@Composable
fun FunctionalityScreen(vm: AppStateViewModel) {
    val theme by vm.theme.collectAsState()
    val ws = rememberWindowSize()

    Scaffold(
        topBar = {
            NavBar(
                title = "Functionality",
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
                .padding(ws.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            ToolCard(
                title     = "Translator",
                sub       = "Speak or type text, translate into 20+ languages",
                icon      = Icons.Default.Language,
                imageName = "languagesphoto",
                color     = Color(red = 0.05f, green = 0.38f, blue = 0.82f),
                theme     = theme
            ) { vm.navigate(Screen.Translator) }

            ToolCard(
                title     = "Pedometer",
                sub       = "Count steps, track distance and time on any walk",
                icon      = Icons.Default.DirectionsWalk,
                imageName = "pedometer",
                color     = Color(red = 0.08f, green = 0.52f, blue = 0.12f),
                theme     = theme
            ) { vm.navigate(Screen.Pedometer) }
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    sub: String,
    icon: ImageVector,
    imageName: String,
    color: Color,
    theme: AppTheme,
    onClick: () -> Unit
) {
    val ws = rememberWindowSize()
        val context = LocalContext.current
    val imageResId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ws.cardRadius),
        color = theme.cardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(ws.cardRadius),
                ambientColor = color.copy(alpha = 0.12f),
                spotColor   = color.copy(alpha = 0.12f)
            )
    ) {
        Row(
            modifier = Modifier.padding(ws.cardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Box(
                modifier = Modifier
                    .size(ws.tileIconSize)
                    .clip(RoundedCornerShape(ws.cardRadius)),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(color = color, shape = RoundedCornerShape(ws.cardRadius), modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(ws.navIconSize))
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = color)
                Text(sub, fontSize = ws.captionSp.sp, color = theme.mutedText)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(ws.smallIconSize)
            )
        }
    }
}
