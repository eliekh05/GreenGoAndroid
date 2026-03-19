package com.greengo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - PreferencesScreen  (mirrors iOS PreferencesView)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PreferencesScreen(vm: AppStateViewModel) {
    val theme       by vm.theme.collectAsState()
    val triviaScore by vm.triviaScore.collectAsState()
    val memoryScore by vm.memoryScore.collectAsState()
    val oceanScore  by vm.oceanScore.collectAsState()
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NavBar(
                title  = "Preferences",
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
                .padding(ws.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            // Appearance
            PrefSection(title = "Appearance") {
                AppTheme.values().forEach { t ->
                    Surface(
                        onClick = { vm.setTheme(t) },
                        color   = theme.cardBackground,
                        shape   = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                        ) {
                            Box(modifier = Modifier.size(22.dp)
                                .background(t.appearanceDot, RoundedCornerShape(50)))
                            Text(t.displayName, color = theme.text, fontSize = ws.bodySp.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            if (theme == t) {
                                Text("✓", color = Color(red = 0.12f, green = 0.58f, blue = 0.08f),
                                    fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // High Scores
            PrefSection(title = "High Scores") {
                listOf(
                    "Eco Trivia"    to triviaScore,
                    "Wild Recall"   to memoryScore,
                    "Reef Rescuers" to oceanScore
                ).forEach { (label, value) ->
                    ScoreRow(label = label, value = value, theme = theme)
                }
            }

            // Reset
            PrefSection(title = "Reset") {
                Surface(
                    onClick = { showResetConfirm = true },
                    color   = theme.cardBackground,
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                    ) {
                        Text("↺", color = Color.Red, fontSize = 18.sp)
                        Text("Reset All App Data", color = Color.Red, fontSize = ws.bodySp.sp)
                    }
                }
            }

            // App Info
            PrefSection(title = "App Info") {
                listOf("App Name" to "GreenGo", "Version" to "1.0", "Platform" to "Android").forEach { (k, v) ->
                    Surface(color = theme.cardBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(k, color = theme.text, fontSize = ws.bodySp.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(v, color = theme.mutedText, fontSize = ws.bodySp.sp)
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all app data?") },
            text  = { Text("This resets high scores and restores the default theme.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAllAppData(); showResetConfirm = false }) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - SettingsScreen  (mirrors iOS SettingsView — combined scores + reset)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(vm: AppStateViewModel) {
    val theme       by vm.theme.collectAsState()
    val triviaScore by vm.triviaScore.collectAsState()
    val memoryScore by vm.memoryScore.collectAsState()
    val oceanScore  by vm.oceanScore.collectAsState()
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NavBar(
                title  = "Settings",
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
                .padding(ws.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            // Appearance
            PrefSection(title = "Appearance") {
                AppTheme.values().forEach { t ->
                    Surface(
                        onClick = { vm.setTheme(t) },
                        color   = theme.cardBackground,
                        shape   = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
                        ) {
                            Box(modifier = Modifier.size(22.dp)
                                .background(t.appearanceDot, RoundedCornerShape(50)))
                            Text(t.displayName, color = theme.text, fontSize = ws.bodySp.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            if (theme == t) Text("✓", color = Color.Green, fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // High Scores + Reset
            PrefSection(title = "High Scores") {
                listOf(
                    "Eco Trivia"    to triviaScore,
                    "Wild Recall"   to memoryScore,
                    "Reef Rescuers" to oceanScore
                ).forEach { (label, value) ->
                    ScoreRow(label = label, value = value, theme = theme)
                }
                Surface(
                    onClick = { showResetConfirm = true },
                    color   = theme.cardBackground,
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
                        Text("🗑", fontSize = 18.sp)
                        Text("Reset All App Data", color = Color.Red, fontSize = ws.bodySp.sp)
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all app data?") },
            text  = { Text("This resets high scores, shows all info screens again, and restores the default theme.") },
            confirmButton = {
                TextButton(onClick = { vm.resetAllAppData(); showResetConfirm = false }) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrefSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = ws.captionSp.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun ScoreRow(label: String, value: Int, theme: AppTheme) {
    Surface(color = theme.cardBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = ws.contentPadding, vertical = ws.buttonVPadding),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = theme.text, fontSize = ws.bodySp.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("$value", color = theme.mutedText, fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold)
        }
    }
}
