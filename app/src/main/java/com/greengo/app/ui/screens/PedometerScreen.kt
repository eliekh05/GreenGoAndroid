package com.greengo.app.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.text.font.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.AppTheme
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicInteger
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.automirrored.filled.*

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - PedometerManager (mirrors iOS PedometerManager)
// Uses Android TYPE_STEP_COUNTER sensor (hardware step counter).
// Requires ACTIVITY_RECOGNITION permission on API 29+.
// ─────────────────────────────────────────────────────────────────────────────

class PedometerManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var steps      by mutableStateOf(0)
    var distanceKm by mutableStateOf(0.0)
    var elapsed    by mutableStateOf(0L)   // milliseconds
    var isRunning  by mutableStateOf(false)

    private var baselineSteps  = -1L
    private var accSteps       = 0
    private var accDistanceM   = 0.0
    private var accTime        = 0L
    private var sessionStart   = 0L

    val timeString: String get() {
        val s = elapsed / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val rawTotal = event.values[0].toLong()
            if (baselineSteps < 0) { baselineSteps = rawTotal; return }
            val sessionSteps = (rawTotal - baselineSteps).toInt().coerceAtLeast(0)
            steps      = accSteps + sessionSteps
            distanceKm = (accDistanceM + sessionSteps * 0.762) / 1000.0
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun start() {
        if (isRunning) return
        baselineSteps = -1
        sessionStart  = System.currentTimeMillis()
        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        isRunning = true
    }

    fun pause() {
        if (!isRunning) return
        sensorManager.unregisterListener(listener)
        accTime   += System.currentTimeMillis() - sessionStart
        accSteps   = steps
        accDistanceM = distanceKm * 1000.0
        isRunning  = false
    }

    fun reset() {
        pause()
        steps = 0; distanceKm = 0.0; elapsed = 0L
        accSteps = 0; accDistanceM = 0.0; accTime = 0L
    }

    fun tick() {
        if (isRunning) elapsed = accTime + (System.currentTimeMillis() - sessionStart)
    }

    fun release() { sensorManager.unregisterListener(listener) }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - PedometerScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PedometerScreen(vm: AppStateViewModel) {
        val ws = rememberWindowSize()
        val theme = vm.theme.collectAsState().value
    val context = LocalContext.current

    val pedo = remember { PedometerManager(context) }
    DisposableEffect(Unit) { onDispose { pedo.pause(); pedo.release() } }

    // Timer tick every second
    LaunchedEffect(pedo.isRunning) {
        while (isActive && pedo.isRunning) {
            delay(500)
            pedo.tick()
        }
    }

    // Walk animation
    val infiniteTransition = rememberInfiniteTransition(label = "walk")
    val walkOffset by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "walkX"
    )
    val walkRotation by infiniteTransition.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "walkRot"
    )

    // Permission launcher
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) pedo.start() }

    val green = Color(red = 0.08f, green = 0.52f, blue = 0.12f)

    Scaffold(
        topBar = {
            NavBar(
                title  = "Pedometer",
                onBack = { vm.navigate(Screen.Functionality) },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // Walking figure (animated when running)
            val pedResId = remember {
                context.resources.getIdentifier("stickman", "drawable", context.packageName)
            }
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer {
                        if (pedo.isRunning) {
                            translationX = walkOffset; rotationZ = walkRotation
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = green.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxSize()
                ) {}
                if (pedResId != 0) {
                    Image(
                        painter = painterResource(id = pedResId),
                        contentDescription = null,
                        modifier = Modifier.size(ws.tileIconSize),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(ws.tileIconSize)
                    )
                }
            }

            // Stats grid — 3 columns
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ws.contentPadding)
                    .heightIn(max = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing),
                verticalArrangement   = Arrangement.spacedBy(ws.cardSpacing),
                userScrollEnabled     = false
            ) {
                item {
                    StatCard("${pedo.steps}", "Steps",
                        Icons.AutoMirrored.Filled.DirectionsWalk, green, theme)
                }
                item {
                    StatCard("${"%.2f".format(pedo.distanceKm)} km", "Distance",
                        Icons.Default.Map, Color.Blue, theme)
                }
                item {
                    StatCard(pedo.timeString, "Time",
                        Icons.Default.Timer, Color(1f, 0.65f, 0f), theme)
                }
            }

            // Control buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ws.contentPadding)
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)
            ) {
                CtrlBtn(
                    label  = if (pedo.isRunning) "Pause" else "Start",
                    icon   = if (pedo.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    color  = if (pedo.isRunning) Color(1f, 0.65f, 0f) else green,
                    modifier = Modifier.weight(1f)
                ) {
                    if (pedo.isRunning) pedo.pause()
                    else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            permLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            pedo.start()
                        }
                    }
                }
                CtrlBtn(
                    label  = "Reset",
                    icon   = Icons.Default.Refresh,
                    color  = Color.Red.copy(alpha = 0.80f),
                    modifier = Modifier.weight(1f)
                ) { pedo.reset() }
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, icon: ImageVector, color: Color, theme: AppTheme) {
    val ws = rememberWindowSize()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                Modifier.let {
                    it  // shadow handled by Surface elevation
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(ws.cardRadius),
            color = theme.cardBackground,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = ws.buttonVPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Text(
                    text = value,
                    fontSize = ws.bodySp.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1
                )
                Text(label, fontSize = ws.captionSp.sp, color = theme.mutedText)
            }
        }
    }
}

@Composable
private fun CtrlBtn(label: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val ws = rememberWindowSize()
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(ws.cardRadius),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(vertical = ws.buttonVPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(ws.smallIconSize))
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
