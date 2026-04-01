package com.greengo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize

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
                .fillMaxWidth()
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
                icon      = Icons.AutoMirrored.Filled.DirectionsWalk,
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
