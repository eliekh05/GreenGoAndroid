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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
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
// MARK: - Full Google Translate language list (BCP-47 codes)
// ─────────────────────────────────────────────────────────────────────────────

private data class LangEntry(val name: String, val code: String, val bcp47: String)

private val languages = listOf(
    LangEntry("Afrikaans",            "af",    "af-ZA"),
    LangEntry("Albanian",             "sq",    "sq-AL"),
    LangEntry("Amharic",              "am",    "am-ET"),
    LangEntry("Arabic",               "ar",    "ar-SA"),
    LangEntry("Armenian",             "hy",    "hy-AM"),
    LangEntry("Azerbaijani",          "az",    "az-AZ"),
    LangEntry("Basque",               "eu",    "eu-ES"),
    LangEntry("Belarusian",           "be",    "be-BY"),
    LangEntry("Bengali",              "bn",    "bn-BD"),
    LangEntry("Bosnian",              "bs",    "bs-BA"),
    LangEntry("Bulgarian",            "bg",    "bg-BG"),
    LangEntry("Catalan",              "ca",    "ca-ES"),
    LangEntry("Chinese Simplified",   "zh-CN", "zh-CN"),
    LangEntry("Chinese Traditional",  "zh-TW", "zh-TW"),
    LangEntry("Croatian",             "hr",    "hr-HR"),
    LangEntry("Czech",                "cs",    "cs-CZ"),
    LangEntry("Danish",               "da",    "da-DK"),
    LangEntry("Dutch",                "nl",    "nl-NL"),
    LangEntry("Estonian",             "et",    "et-EE"),
    LangEntry("Filipino",             "tl",    "fil-PH"),
    LangEntry("Finnish",              "fi",    "fi-FI"),
    LangEntry("French",               "fr",    "fr-FR"),
    LangEntry("Galician",             "gl",    "gl-ES"),
    LangEntry("Georgian",             "ka",    "ka-GE"),
    LangEntry("German",               "de",    "de-DE"),
    LangEntry("Greek",                "el",    "el-GR"),
    LangEntry("Gujarati",             "gu",    "gu-IN"),
    LangEntry("Haitian Creole",       "ht",    "ht-HT"),
    LangEntry("Hebrew",               "iw",    "he-IL"),
    LangEntry("Hindi",                "hi",    "hi-IN"),
    LangEntry("Hungarian",            "hu",    "hu-HU"),
    LangEntry("Icelandic",            "is",    "is-IS"),
    LangEntry("Indonesian",           "id",    "id-ID"),
    LangEntry("Irish",                "ga",    "ga-IE"),
    LangEntry("Italian",              "it",    "it-IT"),
    LangEntry("Japanese",             "ja",    "ja-JP"),
    LangEntry("Kannada",              "kn",    "kn-IN"),
    LangEntry("Kazakh",               "kk",    "kk-KZ"),
    LangEntry("Khmer",                "km",    "km-KH"),
    LangEntry("Korean",               "ko",    "ko-KR"),
    LangEntry("Lao",                  "lo",    "lo-LA"),
    LangEntry("Latvian",              "lv",    "lv-LV"),
    LangEntry("Lithuanian",           "lt",    "lt-LT"),
    LangEntry("Macedonian",           "mk",    "mk-MK"),
    LangEntry("Malay",                "ms",    "ms-MY"),
    LangEntry("Malayalam",            "ml",    "ml-IN"),
    LangEntry("Maltese",              "mt",    "mt-MT"),
    LangEntry("Marathi",              "mr",    "mr-IN"),
    LangEntry("Mongolian",            "mn",    "mn-MN"),
    LangEntry("Nepali",               "ne",    "ne-NP"),
    LangEntry("Norwegian",            "no",    "no-NO"),
    LangEntry("Persian",              "fa",    "fa-IR"),
    LangEntry("Polish",               "pl",    "pl-PL"),
    LangEntry("Portuguese",           "pt",    "pt-BR"),
    LangEntry("Punjabi",              "pa",    "pa-IN"),
    LangEntry("Romanian",             "ro",    "ro-RO"),
    LangEntry("Russian",              "ru",    "ru-RU"),
    LangEntry("Serbian",              "sr",    "sr-RS"),
    LangEntry("Sinhala",              "si",    "si-LK"),
    LangEntry("Slovak",               "sk",    "sk-SK"),
    LangEntry("Slovenian",            "sl",    "sl-SI"),
    LangEntry("Somali",               "so",    "so-SO"),
    LangEntry("Spanish",              "es",    "es-ES"),
    LangEntry("Swahili",              "sw",    "sw-KE"),
    LangEntry("Swedish",              "sv",    "sv-SE"),
    LangEntry("Tamil",                "ta",    "ta-IN"),
    LangEntry("Telugu",               "te",    "te-IN"),
    LangEntry("Thai",                 "th",    "th-TH"),
    LangEntry("Turkish",              "tr",    "tr-TR"),
    LangEntry("Ukrainian",            "uk",    "uk-UA"),
    LangEntry("Urdu",                 "ur",    "ur-PK"),
    LangEntry("Uzbek",                "uz",    "uz-UZ"),
    LangEntry("Vietnamese",           "vi",    "vi-VN"),
    LangEntry("Welsh",                "cy",    "cy-GB"),
    LangEntry("Xhosa",                "xh",    "xh-ZA"),
    LangEntry("Yiddish",              "yi",    "yi-001"),
    LangEntry("Yoruba",               "yo",    "yo-NG"),
    LangEntry("Zulu",                 "zu",    "zu-ZA")
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TranslatorScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TranslatorScreen(vm: AppStateViewModel) {
    val ws      = rememberWindowSize()
    val theme   by vm.theme.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var inputText      by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating  by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var selectedIndex  by remember { mutableStateOf(0) }
    var isListening    by remember { mutableStateOf(false) }
    var isSpeaking     by remember { mutableStateOf(false) }
    var searchQuery    by remember { mutableStateOf("") }

    val filteredLangs = remember(searchQuery) {
        if (searchQuery.isBlank()) languages
        else languages.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val currentLang = languages[selectedIndex]

    // ── TTS ──────────────────────────────────────────────────────────────────
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val t = TextToSpeech(context) {}
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    fun speak(text: String) {
        tts?.language = Locale.forLanguageTag(currentLang.bcp47)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        isSpeaking = true
    }
    fun stopSpeaking() { tts?.stop(); isSpeaking = false }

    // ── Speech recognition ───────────────────────────────────────────────────
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    fun triggerTranslation(q: String = inputText.trim()) {
        if (q.isEmpty()) return
        isTranslating = true; errorMessage = null; translatedText = ""; stopSpeaking()
        scope.launch {
            runCatching { googleTranslateFree(q, currentLang.code) }
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
                            val idx = languages.indexOf(lang)
                            val selected = idx == selectedIndex
                            Surface(
                                onClick = {
                                    selectedIndex  = idx
                                    translatedText = ""
                                    errorMessage   = null
                                    searchQuery    = ""
                                    stopSpeaking()
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
                    "Selected: ${currentLang.name}",
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
                        if (isTranslating) "Translating…" else "Translate",
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
                        Text(currentLang.name,
                            fontSize = ws.captionSp.sp,
                            color    = theme.text.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (isSpeaking) stopSpeaking() else speak(translatedText)
                        }) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
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
