package com.greengo.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - PedometerManager (mirrors iOS PedometerManager)
// Uses Android TYPE_STEP_COUNTER sensor (hardware step counter).
// Requires ACTIVITY_RECOGNITION permission on API 29+.
// ─────────────────────────────────────────────────────────────────────────────

class PedometerManager(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var steps      by mutableIntStateOf(0)
    var distanceKm by mutableDoubleStateOf(0.0)
    var elapsed    by mutableLongStateOf(0L)   // milliseconds
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

@SuppressLint("LocalContextResourcesRead", "DiscouragedApi")
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
            Modifier.NavBar(
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
                Modifier
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
