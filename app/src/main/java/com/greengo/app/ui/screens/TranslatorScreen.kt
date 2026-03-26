package com.greengo.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.shape.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Free Google Translate via translate.googleapis.com (no API key)
// URL: https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl={lang}&dt=t&q={text}
// Response: nested JSON array — translation at [0][0][0]
// ─────────────────────────────────────────────────────────────────────────────

private val httpClient = OkHttpClient()

private suspend fun googleTranslateFree(text: String, targetLang: String): String =
    withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
        // Parse: [[["translatedText","originalText",...],...],...]
        val outer = JSONArray(body)
        val segments = outer.getJSONArray(0)
        val sb = StringBuilder()
        for (i in 0 until segments.length()) {
            val seg = segments.optJSONArray(i)
            if (seg != null) sb.append(seg.optString(0))
        }
        sb.toString().trim()
    }

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Language list — fetched dynamically from Google Translate API
// Endpoint: translate.googleapis.com/translate_a/l?client=gtx&hl=en
// Response: {"sl":{...},"tl":{"af":"Afrikaans","sq":"Albanian",...}}
// No API key required.
// ─────────────────────────────────────────────────────────────────────────────

private data class LangEntry(val name: String, val code: String, val bcp47: String)

// Maps Google lang code -> BCP-47 locale for TTS (best-effort, falls back to code itself)
private val bcp47Map = mapOf(
    "af" to "af-ZA", "sq" to "sq-AL", "am" to "am-ET", "ar" to "ar-SA",
    "hy" to "hy-AM", "az" to "az-AZ", "eu" to "eu-ES", "be" to "be-BY",
    "bn" to "bn-BD", "bs" to "bs-BA", "bg" to "bg-BG", "ca" to "ca-ES",
    "zh-CN" to "zh-CN", "zh-TW" to "zh-TW", "hr" to "hr-HR", "cs" to "cs-CZ",
    "da" to "da-DK", "nl" to "nl-NL", "et" to "et-EE", "tl" to "fil-PH",
    "fi" to "fi-FI", "fr" to "fr-FR", "gl" to "gl-ES", "ka" to "ka-GE",
    "de" to "de-DE", "el" to "el-GR", "gu" to "gu-IN", "ht" to "ht-HT",
    "iw" to "he-IL", "hi" to "hi-IN", "hu" to "hu-HU", "is" to "is-IS",
    "id" to "id-ID", "ga" to "ga-IE", "it" to "it-IT", "ja" to "ja-JP",
    "kn" to "kn-IN", "kk" to "kk-KZ", "km" to "km-KH", "ko" to "ko-KR",
    "lo" to "lo-LA", "lv" to "lv-LV", "lt" to "lt-LT", "mk" to "mk-MK",
    "ms" to "ms-MY", "ml" to "ml-IN", "mt" to "mt-MT", "mr" to "mr-IN",
    "mn" to "mn-MN", "ne" to "ne-NP", "no" to "no-NO", "fa" to "fa-IR",
    "pl" to "pl-PL", "pt" to "pt-BR", "pa" to "pa-IN", "ro" to "ro-RO",
    "ru" to "ru-RU", "sr" to "sr-RS", "si" to "si-LK", "sk" to "sk-SK",
    "sl" to "sl-SI", "so" to "so-SO", "es" to "es-ES", "sw" to "sw-KE",
    "sv" to "sv-SE", "ta" to "ta-IN", "te" to "te-IN", "th" to "th-TH",
    "tr" to "tr-TR", "uk" to "uk-UA", "ur" to "ur-PK", "uz" to "uz-UZ",
    "vi" to "vi-VN", "cy" to "cy-GB", "xh" to "xh-ZA", "yi" to "yi-001",
    "yo" to "yo-NG", "zu" to "zu-ZA"
)

private suspend fun fetchLanguages(): List<LangEntry> = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder()
            .url("https://translate.googleapis.com/translate_a/l?client=gtx&hl=en")
            .header("User-Agent", "Mozilla/5.0")
            .get()
            .build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()
        // Response: {"sl":{...},"tl":{"af":"Afrikaans",...}}
        val obj  = org.json.JSONObject(body)
        val tl   = obj.optJSONObject("tl") ?: return@withContext emptyList()
        val list = mutableListOf<LangEntry>()
        tl.keys().forEach { code ->
            val name = tl.getString(code)
            list.add(LangEntry(name = name, code = code, bcp47 = bcp47Map[code] ?: code))
        }
        list.sortedBy { it.name }
    } catch (_: Exception) {
        emptyList()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TranslatorScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TranslatorScreen(vm: AppStateViewModel) {
    val ws      = rememberWindowSize()
    val theme   by vm.theme.collectAsState()
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val keyboardCtrl = LocalSoftwareKeyboardController.current

    var inputText      by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating  by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var selectedIndex  by remember { mutableStateOf(0) }
    var isListening    by remember { mutableStateOf(false) }
    var isSpeaking     by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }

    // Fetch language list dynamically from Google Translate API
    var languages      by remember { mutableStateOf<List<LangEntry>>(emptyList()) }
    var langsLoading   by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val fetched = fetchLanguages()
        languages    = fetched
        langsLoading = false
    }

    val filteredLangs = remember(searchQuery, languages) {
        if (searchQuery.isBlank()) languages
        else languages.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val currentLang = languages.getOrNull(selectedIndex)

    // ── TTS ──────────────────────────────────────────────────────────────────
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) {}
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    fun speak(text: String) {
        tts?.language = Locale.forLanguageTag(currentLang?.bcp47 ?: "en")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        isSpeaking = true
    }
    fun stopSpeaking() { tts?.stop(); isSpeaking = false }

    // ── Speech recognition ───────────────────────────────────────────────────
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    fun triggerTranslation(q: String = inputText.trim()) {
        if (q.isEmpty()) return
        keyboardCtrl?.hide()
        isTranslating = true; errorMessage = null; translatedText = ""; stopSpeaking()
        scope.launch {
            runCatching { googleTranslateFree(q, currentLang?.code ?: "en") }
                .onSuccess { result ->
                    translatedText = result
                    isTranslating  = false
                    speak(result)
                }
                .onFailure { e ->
                    errorMessage  = "Translation failed: ${e.localizedMessage}"
                    isTranslating = false
                }
        }
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull() ?: return
                inputText = recognized; isListening = false
                triggerTranslation(recognized)
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.let { inputText = it }
            }
            override fun onError(error: Int)             { isListening = false }
            override fun onReadyForSpeech(p: Bundle?)    {}
            override fun onBeginningOfSpeech()           {}
            override fun onRmsChanged(p: Float)          {}
            override fun onBufferReceived(p: ByteArray?) {}
            override fun onEndOfSpeech()                 {}
            override fun onEvent(p: Int, p1: Bundle?)    {}
        })
        speechRecognizer.startListening(intent)
        isListening = true
    }

    fun toggleMic() {
        if (isListening) { speechRecognizer.stopListening(); isListening = false }
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    val accentBlue   = Color(red = 0.05f, green = 0.38f, blue = 0.82f)
    val accentPurple = Color(red = 0.45f, green = 0.20f, blue = 0.75f)

    Scaffold(
        topBar = {
            NavBar(
                title  = "Translator",
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
                .verticalScroll(rememberScrollState())
                .padding(ws.cardInnerPadding),
            verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)
        ) {
            // ── Language picker ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Translate to", fontSize = ws.captionSp.sp, color = theme.mutedText)
                // Loading indicator
                if (langsLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp),
                        color = accentBlue, strokeWidth = 2.dp
                    )
                }
                // Search box
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search language…", fontSize = ws.captionSp.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = theme.mutedText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = theme.mutedText)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = theme.inputBackground,
                        unfocusedContainerColor = theme.inputBackground,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor        = theme.text,
                        unfocusedTextColor      = theme.text
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                // Language list (max 5 visible, scrollable)
                if (filteredLangs.isEmpty()) {
                    Text("No language found", color = theme.mutedText, fontSize = ws.captionSp.sp,
                        modifier = Modifier.padding(8.dp))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLangs) { lang ->
                            val idx = languages.indexOfFirst { it.code == lang.code }
                            val selected = idx == selectedIndex
                            Surface(
                                onClick = {
                                    selectedIndex  = idx
                                    translatedText = ""
                                    errorMessage   = null
                                    searchQuery    = ""
                                    stopSpeaking()
                                    keyboardCtrl?.hide()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) accentBlue else theme.cardBackground,
                                border = if (selected) null else BorderStroke(1.dp, theme.mutedText.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    lang.name,
                                    fontSize = ws.captionSp.sp,
                                    color    = if (selected) Color.White else theme.text,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                // Selected language chip
                Text(
                    "Selected: ${currentLang?.name ?: "Loading…"}",
                    fontSize = ws.captionSp.sp,
                    color    = accentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Input ─────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("English text", fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.weight(1f))
                    if (inputText.isNotEmpty()) {
                        TextButton(onClick = {
                            inputText = ""; translatedText = ""; errorMessage = null; stopSpeaking()
                        }) {
                            Text("Clear", fontSize = ws.captionSp.sp, color = theme.accent)
                        }
                    }
                }
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = ws.buttonHeight * 2),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = theme.inputBackground,
                        unfocusedContainerColor = theme.inputBackground,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor        = theme.text,
                        unfocusedTextColor      = theme.text
                    ),
                    placeholder = { Text("Type text here…", color = theme.mutedText) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { triggerTranslation() }),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Translate + Mic buttons ───────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
                Button(
                    onClick  = { triggerTranslation() },
                    enabled  = !isTranslating && inputText.trim().isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = if (isTranslating) Color.Gray else accentBlue,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(vertical = ws.buttonVPadding)
                ) {
                    Text(
                        if (isTranslating) "Translating…" else "Get Translation",
                        fontSize   = ws.bodySp.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
                Button(
                    onClick  = { toggleMic() },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color.Red else accentPurple),
                    contentPadding = PaddingValues(vertical = ws.buttonVPadding)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = null,
                            tint     = Color.White,
                            modifier = Modifier.size(ws.smallIconSize)
                        )
                        Text(
                            if (isListening) "Stop" else "Speak",
                            fontSize   = ws.bodySp.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── Translation output ────────────────────────────────────────────
            if (translatedText.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentLang?.name ?: "",
                            fontSize = ws.captionSp.sp,
                            color    = theme.text.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (isSpeaking) stopSpeaking() else speak(translatedText)
                        }) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak",
                                tint     = theme.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text     = translatedText,
                        fontSize = 17.sp,
                        color    = theme.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.inputBackground, RoundedCornerShape(12.dp))
                            .padding(ws.cardInnerPadding)
                    )
                }
            }

            // ── Error ─────────────────────────────────────────────────────────
            errorMessage?.let { err ->
                Text(err, fontSize = ws.captionSp.sp, color = Color.Red)
            }
        }
    }
}
