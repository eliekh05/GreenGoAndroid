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
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import com.greengo.app.data.AppStateViewModel
import com.greengo.app.data.Screen
import com.greengo.app.ui.components.NavBar
import com.greengo.app.ui.components.rememberWindowSize
import com.greengo.app.ui.components.WindowSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Google Cloud Translate API key
// Replace with your actual API key from Google Cloud Console
// ─────────────────────────────────────────────────────────────────────────────

private const val GOOGLE_TRANSLATE_API_KEY = "YOUR_GOOGLE_TRANSLATE_API_KEY"

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Language list
// Uses BCP-47 language codes for Google Translate API
// ─────────────────────────────────────────────────────────────────────────────

private data class LangEntry(val name: String, val googleCode: String, val bcp47: String)

private val languages = listOf(
    LangEntry("Arabic",              "ar",    "ar-SA"),
    LangEntry("Chinese Simplified",  "zh-CN", "zh-CN"),
    LangEntry("Chinese Traditional", "zh-TW", "zh-TW"),
    LangEntry("Czech",               "cs",    "cs-CZ"),
    LangEntry("Danish",              "da",    "da-DK"),
    LangEntry("Dutch",               "nl",    "nl-NL"),
    LangEntry("Finnish",             "fi",    "fi-FI"),
    LangEntry("French",              "fr",    "fr-FR"),
    LangEntry("German",              "de",    "de-DE"),
    LangEntry("Greek",               "el",    "el-GR"),
    LangEntry("Hebrew",              "iw",    "he-IL"),
    LangEntry("Hindi",               "hi",    "hi-IN"),
    LangEntry("Hungarian",           "hu",    "hu-HU"),
    LangEntry("Indonesian",          "id",    "id-ID"),
    LangEntry("Italian",             "it",    "it-IT"),
    LangEntry("Japanese",            "ja",    "ja-JP"),
    LangEntry("Korean",              "ko",    "ko-KR"),
    LangEntry("Malay",               "ms",    "ms-MY"),
    LangEntry("Norwegian",           "no",    "no-NO"),
    LangEntry("Persian",             "fa",    "fa-IR"),
    LangEntry("Polish",              "pl",    "pl-PL"),
    LangEntry("Portuguese",          "pt",    "pt-BR"),
    LangEntry("Romanian",            "ro",    "ro-RO"),
    LangEntry("Russian",             "ru",    "ru-RU"),
    LangEntry("Spanish",             "es",    "es-ES"),
    LangEntry("Swedish",             "sv",    "sv-SE"),
    LangEntry("Thai",                "th",    "th-TH"),
    LangEntry("Turkish",             "tr",    "tr-TR"),
    LangEntry("Ukrainian",           "uk",    "uk-UA"),
    LangEntry("Vietnamese",          "vi",    "vi-VN")
)

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Google Cloud Translate REST helper
// ─────────────────────────────────────────────────────────────────────────────

private val httpClient = OkHttpClient()

private suspend fun googleTranslate(text: String, targetLang: String): String =
    withContext(Dispatchers.IO) {
        val url = "https://translation.googleapis.com/language/translate/v2?key=$GOOGLE_TRANSLATE_API_KEY"
        val json = JSONObject().apply {
            put("q", text)
            put("source", "en")
            put("target", targetLang)
            put("format", "text")
        }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) {
            val errObj = runCatching { JSONObject(responseBody).getJSONObject("error").getString("message") }
                .getOrDefault("HTTP ${response.code}")
            throw Exception("Translate error: $errObj")
        }
        val result = JSONObject(responseBody)
        result.getJSONObject("data")
            .getJSONArray("translations")
            .getJSONObject(0)
            .getString("translatedText")
    }

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - TranslatorScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TranslatorScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
    val theme by vm.theme.collectAsState()
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var inputText      by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating  by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf<String?>(null) }
    var selectedIndex  by remember { mutableStateOf(0) }
    var isListening    by remember { mutableStateOf(false) }
    var isSpeaking     by remember { mutableStateOf(false) }

    val currentLang = languages[selectedIndex]

    // TTS
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

    // Speech recognition
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    DisposableEffect(Unit) { onDispose { speechRecognizer.destroy() } }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    inputText = matches?.firstOrNull() ?: inputText
                    isListening = false
                    if (inputText.isNotBlank()) {
                        isTranslating = true; errorMessage = null; translatedText = ""
                        scope.launch {
                            runCatching { googleTranslate(inputText.trim(), currentLang.googleCode) }
                                .onSuccess { result ->
                                    translatedText = result
                                    isTranslating = false
                                    speak(result)
                                }
                                .onFailure { e ->
                                    errorMessage = "Translation failed: ${e.localizedMessage}"
                                    isTranslating = false
                                }
                        }
                    }
                }
                override fun onPartialResults(partial: Bundle?) {
                    val matches = partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { inputText = it }
                }
                override fun onError(error: Int) { isListening = false }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(p: Float) {}
                override fun onBufferReceived(p: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(p: Int, p1: Bundle?) {}
            })
            speechRecognizer.startListening(intent)
            isListening = true
        }
    }

    fun toggleMic() {
        if (isListening) {
            speechRecognizer.stopListening(); isListening = false
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun triggerTranslation() {
        val q = inputText.trim(); if (q.isEmpty()) return
        isTranslating = true; errorMessage = null; translatedText = ""; stopSpeaking()
        scope.launch {
            runCatching { googleTranslate(q, currentLang.googleCode) }
                .onSuccess { result ->
                    translatedText = result
                    isTranslating = false
                    speak(result)
                }
                .onFailure { e ->
                    errorMessage = "Translation failed: ${e.localizedMessage}"
                    isTranslating = false
                }
        }
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
            // Language picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Translate to", fontSize = ws.captionSp.sp, color = theme.mutedText)
                ExposedDropdownMenuLanguagePicker(
                    selectedIndex = selectedIndex,
                    onSelect = { idx ->
                        selectedIndex  = idx
                        translatedText = ""
                        errorMessage   = null
                        stopSpeaking()
                    },
                    theme = theme
                )
            }

            // Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("English text", fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.weight(1f))
                    if (inputText.isNotEmpty()) {
                        TextButton(onClick = { inputText = ""; translatedText = ""; errorMessage = null; stopSpeaking() }) {
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
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Translate + Mic buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
                Button(
                    onClick  = { triggerTranslation() },
                    enabled  = !isTranslating && inputText.trim().isNotEmpty(),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (isTranslating) Color.Gray else accentBlue,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(vertical = ws.buttonVPadding)
                ) {
                    Text(
                        if (isTranslating) "Translating…" else "Get Translation",
                        fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold, color = Color.White
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
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(ws.smallIconSize)
                        )
                        Text(if (isListening) "Stop" else "Speak",
                            fontSize = ws.bodySp.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Translation output
            if (translatedText.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentLang.name, fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            if (isSpeaking) stopSpeaking() else speak(translatedText)
                        }) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speak",
                                tint = theme.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = translatedText,
                        fontSize = 17.sp,
                        color = theme.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.inputBackground, RoundedCornerShape(12.dp))
                            .padding(ws.cardInnerPadding)
                    )
                }
            }

            // Error
            errorMessage?.let { err ->
                Text(err, fontSize = ws.captionSp.sp, color = Color.Red)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - Language picker (ExposedDropdownMenu)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuLanguagePicker(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    theme: com.greengo.app.data.AppTheme
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        TextField(
            value = languages[selectedIndex].name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            languages.forEachIndexed { i, lang ->
                DropdownMenuItem(
                    text = { Text(lang.name) },
                    onClick = { onSelect(i); expanded = false }
                )
            }
        }
    }
}
