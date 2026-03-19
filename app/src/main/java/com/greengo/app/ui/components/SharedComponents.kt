package com.greengo.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - GreenGo Colour constants
// ─────────────────────────────────────────────────────────────────────────────

val GreenPrimary = Color(red = 0.08f, green = 0.52f, blue = 0.10f)
val BluePrimary  = Color(red = 0.10f, green = 0.38f, blue = 0.82f)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - NavBar
// Mirrors iOS navBar() helper: back chevron left, centred title, home icon right.
// Use inside Scaffold(topBar = { NavBar(...) }) so Scaffold handles insets.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NavBar(
    title: String,
    onBack: () -> Unit,
    onHome: (() -> Unit)? = null,     // null → hide home icon (matches iOS showHome=false)
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val ws = rememberWindowSize()
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.92f))
                .statusBarsPadding()
                .padding(horizontal = ws.contentPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(ws.navIconSize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.text,
                    modifier = Modifier.size(ws.smallIconSize)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                fontSize = ws.navTitleSp.sp,
                fontWeight = FontWeight.Bold,
                color = theme.text
            )
            Spacer(modifier = Modifier.weight(1f))

            // Home icon (or transparent spacer to keep title centred)
            if (onHome != null) {
                IconButton(
                    onClick = onHome,
                    modifier = Modifier.size(ws.navIconSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = theme.mutedText,
                        modifier = Modifier.size(ws.smallIconSize)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(ws.navIconSize))
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.6f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - BackBtn
// Small "← Back" link used on info screens that don't have a full NavBar.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BackBtn(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ws = rememberWindowSize()
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = GreenPrimary,
            modifier = Modifier.size(ws.smallIconSize)
        )
        Text(
            text = "Back",
            fontSize = ws.bodySp.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary
        )
    }
}
