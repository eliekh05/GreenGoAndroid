package com.greengo.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.greengo.app.ui.components.BluePrimary
import com.greengo.app.ui.components.GreenPrimary
import com.greengo.app.ui.components.WindowSize
import com.greengo.app.ui.components.rememberWindowSize
import java.util.UUID

private data class NavTile(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val sub: String,
    val icon: ImageVector,
    val color: Color,
    val imageName: String,
    val dest: Screen
)

@Composable
fun HomeScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
    val theme       by vm.theme.collectAsState()
    val skipMapInfo by vm.skipMapInfo.collectAsState()
    val ws           = rememberWindowSize()

    val tiles = remember {
        listOf(
            NavTile(title="Maps",          sub="Eco-friendly locations",   icon=Icons.Default.Map,      color=GreenPrimary,                         imageName="pin",            dest=Screen.MapInfo),
            NavTile(title="Functionality", sub="Translator & Pedometer",   icon=Icons.Default.Settings, color=BluePrimary,                          imageName="languagesphoto", dest=Screen.Functionality),
            NavTile(title="Games",         sub="Learn while you play",     icon=Icons.Default.Games,    color=Color(0.78f, 0.22f, 0.08f, 1f),       imageName="game_controller", dest=Screen.Games)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Space for pinned header
            Spacer(modifier = Modifier.statusBarsPadding().height(ws.buttonHeight + ws.cardSpacing * 2))

            Column(
                modifier = Modifier
                    .padding(horizontal = ws.contentPadding)
                    .padding(top = ws.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
            ) {
                // On tablets: 2-column tile grid
                if (ws.isTablet) {
                    tiles.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                        ) {
                            row.forEach { tile ->
                                Box(modifier = Modifier.weight(1f)) {
                                    TileCard(tile, theme, ws) {
                                        if (tile.dest == Screen.MapInfo)
                                            vm.navigate(if (skipMapInfo) Screen.Map else Screen.MapInfo)
                                        else vm.navigate(tile.dest)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    tiles.forEach { tile ->
                        TileCard(tile, theme, ws) {
                            if (tile.dest == Screen.MapInfo)
                                vm.navigate(if (skipMapInfo) Screen.Map else Screen.MapInfo)
                            else vm.navigate(tile.dest)
                        }
                    }
                }

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                ) {
                    SmallBtn("About",      Icons.Default.Info,
                        Color(0.35f,0.18f,0.62f,1f), Modifier.weight(1f)) { vm.navigate(Screen.About) }
                    SmallBtn("Contact Us", Icons.Default.Email,
                        Color(0.05f,0.52f,0.44f,1f), Modifier.weight(1f)) { vm.navigate(Screen.Contact) }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
                    SmallBtn("Preferences", Icons.Default.Tune,
                        Color(0.40f,0.40f,0.40f,1f), Modifier.fillMaxWidth()) { vm.navigate(Screen.Preferences) }
                }
            }
        }

        HomeHeader(ws)
    }
}

@SuppressLint("LocalContextResourcesRead", "DiscouragedApi")
@Composable
private fun HomeHeader(ws: WindowSize) {
    val ws = rememberWindowSize()
        val context    = LocalContext.current
    val logoResId  = remember {
        context.resources.getIdentifier("greengologocopy", "drawable", context.packageName)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .statusBarsPadding()
                .padding(horizontal = ws.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "GreenGo logo",
                    modifier = Modifier.size(ws.tileIconSize * 0.6f).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row {
                    Text("Green", color = GreenPrimary, fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Go",    color = BluePrimary,  fontSize = ws.headingSp.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("Sustainable Travel Companion", fontSize = (ws.bodySp - 2f).sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

@SuppressLint("LocalContextResourcesRead", "DiscouragedApi")
@Composable
private fun TileCard(tile: NavTile, theme: AppTheme, ws: WindowSize, onClick: () -> Unit) {
    val ws = rememberWindowSize()
        val context     = LocalContext.current
    val imageResId  = remember(tile.imageName) {
        context.resources.getIdentifier(tile.imageName, "drawable", context.packageName)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(ws.cardRadius),
        color = theme.cardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(ws.cardRadius),
                ambientColor = tile.color.copy(alpha = 0.12f),
                spotColor    = tile.color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(ws.cardInnerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Box(
                modifier = Modifier
                    .size(ws.tileIconSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (imageResId == 0) tile.color else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0)
                    Image(painterResource(imageResId), null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else
                    Icon(tile.icon, null, tint = Color.White, modifier = Modifier.size(ws.tileIconSize * 0.5f))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tile.title, color = tile.color, fontSize = (ws.bodySp + 6f).sp, fontWeight = FontWeight.Bold)
                Text(tile.sub,   fontSize = ws.bodySp.sp, color = theme.mutedText)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = tile.color, modifier = Modifier.size(ws.smallIconSize))
        }
    }
}

@Composable
private fun SmallBtn(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val ws = rememberWindowSize()
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(ws.cardRadius),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(vertical = ws.buttonVPadding, horizontal = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(ws.smallIconSize))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold)
        }
    }
}
