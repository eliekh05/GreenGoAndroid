package com.greengo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.BackBtn
import com.greengo.app.ui.components.WindowSize
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.GreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Marker.OnMarkerClickListener
import java.net.URL
import java.net.URLEncoder
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Domain models  (mirrors iOS MarkerCategory / EcoMarker / Continent)
// ─────────────────────────────────────────────────────────────────────────────

enum class MarkerCategory(
    val label: String,
    val emoji: String,
    val pinColor: Int      // android.graphics.Color int for OSMDroid marker tint
) {
    ACCOMMODATION("Stay",    "🏨", android.graphics.Color.rgb(13, 107, 209)),   // blue
    CYCLING      ("Cycling", "🚲", android.graphics.Color.rgb(25, 140,  38)),   // green
    NATURE       ("Nature",  "🌿", android.graphics.Color.rgb(165, 102,  13))   // amber
}

data class EcoMarker(
    val name: String,
    val lat: Double,
    val lon: Double,
    val category: MarkerCategory,
    val website: String?      = null,
    val phone: String?        = null,
    val address: String?      = null,
    val openingHours: String? = null
)

enum class Continent(
    val label: String,
    val flag: String,
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val radiusKm: Double
) {
    LEBANON      ("Lebanon",    "🇱🇧",  33.8547,   35.8623, 10.0,  80.0),
    EUROPE       ("Europe",     "🌍",   50.0,      10.0,     4.0, 2500.0),
    ASIA         ("Asia",       "🌏",   35.0,     100.0,     4.0, 2500.0),
    AFRICA       ("Africa",     "🌍",    5.0,      20.0,     4.0, 2500.0),
    NORTH_AMERICA("N. America", "🌎",   45.0,    -100.0,     4.0, 2500.0),
    SOUTH_AMERICA("S. America", "🌎",  -15.0,     -60.0,     4.0, 2500.0),
    AUSTRALIA    ("Australia",  "🌏",  -25.0,     133.0,     4.0, 2500.0),
    ANTARCTICA   ("Antarctica", "🧊",  -80.0,       0.0,     3.0, 1500.0)
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Overpass + Nominatim  (identical logic to iOS — same APIs)
// ─────────────────────────────────────────────────────────────────────────────

private val overpassMirrors = listOf(
    "https://overpass-api.de/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
    "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    "https://overpass.nchc.org.tw/api/interpreter"
)

private val overpassFilters: Map<MarkerCategory, List<String>> = mapOf(
    MarkerCategory.ACCOMMODATION to listOf(
        "[\"tourism\"~\"hotel|hostel|guest_house|motel|camp_site|alpine_hut|chalet|wilderness_hut|caravan_site|apartment|resort\"]"
    ),
    MarkerCategory.CYCLING to listOf(
        "[\"amenity\"=\"bicycle_rental\"]",
        "[\"amenity\"=\"bicycle_repair_station\"]",
        "[\"shop\"=\"bicycle\"]"
    ),
    MarkerCategory.NATURE to listOf(
        "[\"leisure\"~\"nature_reserve|park|garden\"]",
        "[\"boundary\"=\"national_park\"]",
        "[\"natural\"~\"wood|forest|beach|cliff|peak|waterfall|spring|wetland\"]",
        "[\"tourism\"~\"viewpoint|picnic_site|wilderness_hut\"]"
    )
)

private suspend fun fetchMarkers(lat: Double, lon: Double, radiusKm: Double): List<EcoMarker> =
    withContext(Dispatchers.IO) {
        val rad = (minOf(radiusKm, 80.0) * 1000).toInt()
        val parts = mutableListOf("[out:json][timeout:25];(")
        for ((_, filters) in overpassFilters) {
            for (f in filters) {
                parts += "  node$f(around:$rad,$lat,$lon);"
                parts += "  way$f(around:$rad,$lat,$lon);"
            }
        }
        parts += "); out 300 center tags;"
        val query = parts.joinToString("\n")

        for (mirror in overpassMirrors) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val con = URL("$mirror?data=$encoded").openConnection()
                con.setRequestProperty("User-Agent", "GreenGo/1.0 (Android; contact: greengo.customerfeedback@gmail.com)")
                con.connectTimeout = 20_000; con.readTimeout = 30_000
                val json  = JSONObject(con.getInputStream().bufferedReader().readText())
                val elems = json.getJSONArray("elements")
                val seen  = mutableSetOf<String>()
                val out   = mutableListOf<EcoMarker>()
                for (i in 0 until elems.length()) {
                    val el    = elems.getJSONObject(i)
                    val elLat = el.optDouble("lat", Double.NaN).let {
                        if (it.isNaN()) el.optJSONObject("center")?.optDouble("lat") ?: Double.NaN else it
                    }
                    val elLon = el.optDouble("lon", Double.NaN).let {
                        if (it.isNaN()) el.optJSONObject("center")?.optDouble("lon") ?: Double.NaN else it
                    }
                    if (elLat.isNaN() || elLon.isNaN()) continue
                    val tags = el.optJSONObject("tags") ?: continue
                    val name = listOf("name", "name:en", "brand")
                        .map { tags.optString(it) }.firstOrNull { it.isNotEmpty() } ?: continue
                    val key  = "$name|${"%.4f".format(elLat)}|${"%.4f".format(elLon)}"
                    if (!seen.add(key)) continue
                    out += EcoMarker(
                        name         = name,
                        lat          = elLat,
                        lon          = elLon,
                        category     = classifyTags(tags),
                        website      = listOf("website","url","contact:website")
                                           .map { tags.optString(it) }.firstOrNull { it.isNotEmpty() },
                        phone        = listOf("phone","contact:phone")
                                           .map { tags.optString(it) }.firstOrNull { it.isNotEmpty() },
                        address      = buildAddress(tags),
                        openingHours = tags.optString("opening_hours").takeIf { it.isNotEmpty() }
                    )
                }
                return@withContext out
            } catch (_: Exception) { delay(100) }
        }
        emptyList()
    }

private fun classifyTags(tags: JSONObject): MarkerCategory {
    val amenity = tags.optString("amenity")
    val shop    = tags.optString("shop")
    val tourism = tags.optString("tourism")
    if (amenity == "bicycle_rental" || amenity == "bicycle_repair_station" || shop == "bicycle")
        return MarkerCategory.CYCLING
    val hotelTypes = setOf("hotel","hostel","guest_house","motel","camp_site","alpine_hut",
        "chalet","wilderness_hut","caravan_site","apartment","resort","bed_and_breakfast","lodge")
    if (tourism in hotelTypes) return MarkerCategory.ACCOMMODATION
    return MarkerCategory.NATURE
}

private fun buildAddress(tags: JSONObject): String? {
    val parts = listOfNotNull(
        tags.optString("addr:housenumber").takeIf { it.isNotEmpty() },
        tags.optString("addr:street").takeIf { it.isNotEmpty() },
        tags.optString("addr:city").takeIf { it.isNotEmpty() }
    )
    return if (parts.isEmpty()) null else parts.joinToString(", ")
}

private suspend fun geocode(query: String): Pair<Double, Double>? =
    withContext(Dispatchers.IO) {
        try {
            val enc = URLEncoder.encode(query, "UTF-8")
            val con = URL("https://nominatim.openstreetmap.org/search?q=$enc&format=json&limit=1")
                .openConnection()
            con.setRequestProperty("User-Agent", "GreenGo/1.0 (Android; contact: greengo.customerfeedback@gmail.com)")
            val arr = JSONArray(con.getInputStream().bufferedReader().readText())
            if (arr.length() == 0) null
            else {
                val o = arr.getJSONObject(0)
                Pair(o.getString("lat").toDouble(), o.getString("lon").toDouble())
            }
        } catch (_: Exception) { null }
    }

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MapScreen
// Uses OSMDroid for the tile map (OpenStreetMap, same source as iOS)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapScreen(vm: AppStateViewModel) {
        val ws = rememberWindowSize()
        val theme   by vm.theme.collectAsState()
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    var markers        by remember { mutableStateOf<List<EcoMarker>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(false) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var filterCat      by remember { mutableStateOf<MarkerCategory?>(null) }
    var continent      by remember { mutableStateOf(Continent.LEBANON) }
    var showSearch     by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }
    var showInfoPanel  by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<EcoMarker?>(null) }
    var osmMapView     by remember { mutableStateOf<MapView?>(null) }
    var regionJob      by remember { mutableStateOf<Job?>(null) }

    // Configure OSMDroid user-agent once
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "GreenGo/1.0 (Android)"
    }

    fun jumpTo(lat: Double, lon: Double, zoom: Double) {
        osmMapView?.controller?.apply {
            setZoom(zoom)
            setCenter(GeoPoint(lat, lon))
        }
    }

    fun redrawPins() {
        val map = osmMapView ?: return
        map.overlays.removeAll { it is Marker }
        val visible = if (filterCat == null) markers else markers.filter { it.category == filterCat }
        for (m in visible) {
            val marker = Marker(map).apply {
                position    = GeoPoint(m.lat, m.lon)
                title       = m.name
                snippet     = m.category.label
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Tint the default OSMDroid marker icon with the category colour
                icon = context.getDrawable(org.osmdroid.library.R.drawable.marker_default)?.apply {
                    setTint(m.category.pinColor)
                }
                setOnMarkerClickListener(OnMarkerClickListener { _, _ ->
                    selectedMarker = m; true
                })
            }
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    fun loadContinent(c: Continent) {
        continent = c
        jumpTo(c.lat, c.lon, c.zoom)
        isLoading = true; errorMsg = null
        scope.launch {
            val result = runCatching { fetchMarkers(c.lat, c.lon, c.radiusKm) }
            markers   = result.getOrDefault(emptyList())
            errorMsg  = if (result.isFailure) "Could not load map data. Check your connection." else null
            isLoading = false
            redrawPins()
        }
    }

    LaunchedEffect(filterCat) { redrawPins() }
    LaunchedEffect(Unit)      { loadContinent(Continent.LEBANON) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── OSMDroid map tile view ────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)   // OpenStreetMap standard tiles
                    setMultiTouchControls(true)
                    controller.setZoom(10.0)
                    controller.setCenter(GeoPoint(33.8547, 35.8623))
                    minZoomLevel = 2.0
                    maxZoomLevel = 19.0
                    // Debounced region-change loader (mirrors iOS onRegionChanged)
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent): Boolean {
                            val zoom = zoomLevelDouble
                            if (zoom < 9.0) return false
                            val center = mapCenter
                            regionJob?.cancel()
                            regionJob = scope.launch {
                                delay(100)
                                isLoading = true
                                val result = runCatching {
                                    fetchMarkers(center.latitude, center.longitude, 80.0)
                                }
                                markers   = result.getOrDefault(markers)
                                isLoading = false
                                redrawPins()
                            }
                            return false
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent) = false
                    })
                    osmMapView = this
                }
            },
            update  = { /* state driven via osmMapView ref */ },
            modifier = Modifier.fillMaxSize()
        )

        // ── Frosted header (same layout as iOS) ───────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            // Title bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f))
                    .statusBarsPadding()
                    .padding(horizontal = ws.contentPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.navigate(Screen.Home) }, modifier = Modifier.size(ws.navIconSize)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = GreenPrimary, modifier = Modifier.size(ws.smallIconSize))
                }

                AnimatedContent(
                    targetState = showSearch,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "searchToggle"
                ) { searching ->
                    if (searching) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextField(
                                value       = searchQuery,
                                onValueChange = { q ->
                                    searchQuery = q
                                    if (q.length >= 3) {
                                        regionJob?.cancel()
                                        regionJob = scope.launch {
                                            delay(200)
                                            val coord = geocode(q) ?: return@launch
                                            jumpTo(coord.first, coord.second, 12.0)
                                            isLoading = true
                                            markers   = runCatching {
                                                fetchMarkers(coord.first, coord.second, 10.0)
                                            }.getOrDefault(markers)
                                            isLoading = false
                                            redrawPins()
                                            showSearch = false
                                        }
                                    }
                                },
                                placeholder = { Text("Search city, country, region…", fontSize = ws.bodySp.sp) },
                                modifier    = Modifier.weight(1f),
                                singleLine  = true,
                                colors      = TextFieldDefaults.colors(
                                    focusedContainerColor   = Color.White.copy(alpha = 0.8f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            TextButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Text("Cancel", color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Eco Map", fontSize = ws.navTitleSp.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            if (isLoading) CircularProgressIndicator(
                                modifier = Modifier.size(ws.smallIconSize), color = GreenPrimary, strokeWidth = 2.dp)
                            IconButton(onClick = { showInfoPanel = true }, modifier = Modifier.size(ws.navIconSize)) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Gray)
                            }
                            IconButton(onClick = { showSearch = true }, modifier = Modifier.size(ws.navIconSize)) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = GreenPrimary)
                            }
                        }
                    }
                }
            }

            // Continent chips
            if (!showSearch) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.92f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = ws.contentPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Continent.values().forEach { c ->
                        FilterChip(
                            selected = continent == c,
                            onClick  = { loadContinent(c) },
                            label    = {
                                Text("${c.flag} ${c.label}", fontSize = ws.captionSp.sp, fontWeight = FontWeight.Bold)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.85f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = ws.contentPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterCat == null,
                        onClick  = { filterCat = null },
                        label    = { Text("All", fontSize = ws.captionSp.sp, fontWeight = FontWeight.Bold) }
                    )
                    MarkerCategory.values().forEach { cat ->
                        FilterChip(
                            selected = filterCat == cat,
                            onClick  = { filterCat = if (filterCat == cat) null else cat },
                            label    = {
                                Text("${cat.emoji} ${cat.label}", fontSize = ws.captionSp.sp, fontWeight = FontWeight.Bold)
                            }
                        )
                    }
                }
            }

            // Error banner
            errorMsg?.let { err ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ws.contentPadding, vertical = 4.dp)
                        .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .padding(horizontal = ws.contentPadding, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(err, color = Color.White, fontSize = ws.captionSp.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { errorMsg = null; loadContinent(continent) }) {
                        Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Pin count pill
            if (!isLoading && markers.isNotEmpty()) {
                val count = if (filterCat == null) markers.size
                            else markers.count { it.category == filterCat }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 6.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("📍 $count place${if (count == 1) "" else "s"}",
                        fontSize = ws.captionSp.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                }
            }
        }

        // Info overlay
        if (showInfoPanel) {
            MapInfoOverlay(onDismiss = { skipNow ->
                if (skipNow) vm.setSkipMapInfo(true)
                showInfoPanel = false
            })
        }

        // Marker detail sheet
        selectedMarker?.let { marker ->
            MarkerDetailSheet(marker = marker, context = context) { selectedMarker = null }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MapInfoOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapInfoOverlay(onDismiss: (skipNow: Boolean) -> Unit) {
    val ws = rememberWindowSize()
    var doNotShow by remember { mutableStateOf(false) }
    val darkGreen = Color(red = 0.04f, green = 0.16f, blue = 0.07f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ws.contentPadding, vertical = 30.dp)
                .background(darkGreen.copy(alpha = 0.95f), RoundedCornerShape(30.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            Text("Eco Map", fontSize = ws.headingSp.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Powered by OpenStreetMap", fontSize = ws.captionSp.sp, color = Color.White.copy(alpha = 0.5f))

            listOf(
                Triple("🏨", "Eco-friendly Accommodation", "Hotels · Hostels · Campsites"),
                Triple("🚲", "Cycling",                     "Bike rentals · Shops · Repair"),
                Triple("🌿", "Nature",                       "Parks · Reserves · Beaches")
            ).forEach { (emoji, title, detail) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(13.dp))
                        .padding(11.dp),
                    horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = ws.navTitleSp.sp)
                    Column {
                        Text(title,  fontSize = ws.captionSp.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(detail, fontSize = ws.captionSp.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            Text(
                "Tap any pin for details, directions & website. Tap ⓘ on the map any time to re-open this guide.",
                fontSize = ws.captionSp.sp, color = Color.White.copy(alpha = 0.55f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Don't show again", fontSize = ws.bodySp.sp, color = Color.White.copy(alpha = 0.75f))
                Switch(checked = doNotShow, onCheckedChange = { doNotShow = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(red = 0.25f, green = 0.85f, blue = 0.35f)))
            }

            Button(
                onClick = { onDismiss(doNotShow) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(ws.cardRadius),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(red = 0.20f, green = 0.85f, blue = 0.35f))
            ) {
                Text("Open Map", fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MarkerDetailSheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MarkerDetailSheet(
    marker: EcoMarker,
    context: Context,
    onDismiss: () -> Unit
) {
    val ws = rememberWindowSize()
    val darkGreen = Color(red = 0.04f, green = 0.12f, blue = 0.06f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(darkGreen.copy(alpha = 0.97f),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            // Drag handle
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(38.dp).height(5.dp)
                    .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(50)))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
            ) {
                // Header row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(marker.name, fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.12f)) {
                            Text("${marker.category.emoji} ${marker.category.label}",
                                fontSize = ws.captionSp.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("✕", color = Color.White.copy(alpha = 0.55f), fontSize = ws.bodySp.sp)
                    }
                }

                // Detail rows
                marker.address?.let      { DetailRow("📍", it) }
                marker.phone?.let        { DetailRow("📞", it) }
                marker.openingHours?.let { DetailRow("🕐", it) }

                // Google Maps search (universal deep link — works on all Android devices)
                val gmQuery = URLEncoder.encode(marker.name, "UTF-8")
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$gmQuery&center=${marker.lat},${marker.lon}")))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(ws.cardRadius),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(red = 0.9f, green = 0.35f, blue = 0.2f))
                ) { Text("Search on Google Maps", fontWeight = FontWeight.SemiBold, color = Color.White) }

                // Website button (if available)
                marker.website?.takeIf { it.isNotEmpty() }?.let { site ->
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(site)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(ws.cardRadius),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(red = 0.3f, green = 0.8f, blue = 0.5f))
                    ) { Text("Visit Website", fontWeight = FontWeight.SemiBold, color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(emoji: String, text: String) {
    val ws = rememberWindowSize()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(11.dp),
        horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing),
        verticalAlignment = Alignment.Top
    ) {
        Text(emoji, fontSize = ws.captionSp.sp)
        Text(text, fontSize = ws.captionSp.sp, color = Color.White.copy(alpha = 0.72f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - MapInfoScreen  (text guide reachable from Preferences)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapInfoScreen(vm: AppStateViewModel) {
        val ws = rememberWindowSize()
        val theme by vm.theme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ws.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            BackBtn(onClick = { vm.navigate(Screen.Home) })
            Text("Eco Map Guide", fontSize = ws.headingSp.sp, fontWeight = FontWeight.Bold, color = theme.accent)

            listOf(
                "Real eco-friendly places pulled live from OpenStreetMap worldwide.",
                "Tap a continent to jump there and load local places.",
                "Search any city, country, or region.",
                "Filter by Stay, Cycling, or Nature.",
                "Tap any pin for details, directions & website.",
                "Tap ⓘ on the map to reopen this guide anytime."
            ).forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing), verticalAlignment = Alignment.Top) {
                    Text("✅", fontSize = ws.bodySp.sp)
                    Text(tip, fontSize = ws.bodySp.sp, color = theme.text)
                }
            }

            Button(
                onClick = { vm.setSkipMapInfo(false); vm.navigate(Screen.Map) },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(ws.cardRadius),
                colors   = ButtonDefaults.buttonColors(containerColor = theme.accent)
            ) {
                Text("Open Map", fontSize = ws.cardTitleSp.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
