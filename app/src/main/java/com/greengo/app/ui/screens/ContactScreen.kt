package com.greengo.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.*
import androidx.compose.foundation.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.concurrent.TimeUnit
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*

private val supabaseClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

private suspend fun supabaseSend(replyTo: String, subject: String, message: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("replyTo", replyTo)
                put("subject", subject)
                put("message", message)
            }
            val body = payload.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://sjsjagoqzjvgsiyejial.supabase.co/functions/v1/send-email")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "sb_publishable_fbfUw36cqPdExnkaHxxtBw_Kfx9Sj5L")
                .build()
            supabaseClient.newCall(request).execute().use { response ->
                if (response.code == 200) return@withContext null
                val json = runCatching { JSONObject(response.body?.string() ?: "") }.getOrNull()
                json?.optString("error")?.takeIf { it.isNotEmpty() }
                    ?: "Server error (${response.code})"
            }
        } catch (e: Exception) {
            e.localizedMessage ?: "Unknown network error"
        }
    }

private val emailRegex = Regex("^[A-Z0-9a-z._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\$")
private fun sanitize(raw: String): String {
    val htmlStripped = raw.replace(Regex("<[^>]*>"), "")
    return htmlStripped.replace(
        Regex("(?i)(--|;|\\b(DROP|SELECT|INSERT|UPDATE|DELETE|UNION|EXEC|CAST|DECLARE|TRUNCATE)\\b)"),
        ""
    ).trim()
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: - ContactScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContactScreen(vm: AppStateViewModel) {
    val ws = rememberWindowSize()
    val theme by vm.theme.collectAsState()
        val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var emailText   by remember { mutableStateOf("") }
    var subjectText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isSending   by remember { mutableStateOf(false) }
    var showAlert   by remember { mutableStateOf(false) }
    var alertMsg    by remember { mutableStateOf("") }
    var alertSuccess by remember { mutableStateOf(false) }

    val toAddress = "greengo.customerfeedback@gmail.com"
    val green = Color(red = 0.08f, green = 0.52f, blue = 0.10f)

    val isValidEmail = remember(emailText) { emailRegex.matches(emailText.trim()) }
    val canSend = isValidEmail && messageText.trim().length >= 3 && !isSending

    val emailResId = remember {
        context.resources.getIdentifier("email", "drawable", context.packageName)
    }

    Scaffold(
        topBar = {
            NavBar(
                title = "Contact Us",
                onBack = { vm.navigate(Screen.Home) },
                onHome = { vm.navigate(Screen.Home) },
                theme = theme
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
            // Email icon
            if (emailResId != 0) {
                Image(
                    painter = painterResource(id = emailResId),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = green,
                    modifier = Modifier.size(60.dp).align(Alignment.CenterHorizontally)
                )
            }

            Text(
                "We'd love to hear from you!",
                fontSize = ws.bodySp.sp,
                color = theme.text.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Email field
            ContactField("Your Email", emailText, onValueChange = { emailText = it },
                keyboardType = KeyboardType.Email, theme = theme)

            if (emailText.isNotEmpty() && !isValidEmail) {
                Text("Please enter a valid email address",
                    fontSize = ws.captionSp.sp, color = Color.Red.copy(alpha = 0.8f))
            }

            // Subject field
            ContactField("Subject (optional)", subjectText, onValueChange = { subjectText = it },
                theme = theme)

            // Message
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Message", fontSize = ws.captionSp.sp, color = theme.mutedText)
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .shadow(3.dp, RoundedCornerShape(12.dp)),
                    colors = contactFieldColors(theme),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Send button
            Button(
                onClick = {
                    val from    = sanitize(emailText)
                    val subject = sanitize(subjectText).ifEmpty { "GreenGo Feedback" }.take(200)
                    val msg     = sanitize(messageText)
                    isSending = true
                    scope.launch {
                        val err = supabaseSend(from, subject, msg)
                        isSending = false
                        if (err != null) {
                            alertMsg = "Could not send your message ($err). Please try again or email $toAddress directly."
                            alertSuccess = false
                        } else {
                            alertMsg = "Your feedback was sent successfully. Thank you!"
                            alertSuccess = true
                            emailText = ""; subjectText = ""; messageText = ""
                        }
                        showAlert = true
                    }
                },
                enabled = canSend,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ws.cardRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSend) green else Color.Gray.copy(alpha = 0.5f),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(ws.buttonVPadding)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(ws.smallIconSize), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending…", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(ws.smallIconSize))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Email", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Mailto link
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("You can also reach us at: ", fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.65f))
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$toAddress"))
                        context.startActivity(intent)
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(toAddress, fontSize = ws.captionSp.sp, color = theme.accent)
                }
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("Message") },
            text  = { Text(alertMsg) },
            confirmButton = {
                TextButton(onClick = {
                    showAlert = false
                    if (alertSuccess) vm.navigate(Screen.Home)
                }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ContactField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    theme: com.greengo.app.data.AppTheme
) {
    val ws = rememberWindowSize()
    Column(verticalArrangement = Arrangement.spacedBy(ws.cardSpacing)) {
        Text(placeholder, fontSize = ws.captionSp.sp, color = theme.text.copy(alpha = 0.7f))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(10.dp)),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                autoCorrectEnabled = false,
                capitalization = if (keyboardType == KeyboardType.Email)
                    KeyboardCapitalization.None else KeyboardCapitalization.Sentences
            ),
            colors = contactFieldColors(theme),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )
    }
}

@Composable
private fun contactFieldColors(theme: com.greengo.app.data.AppTheme) =
    TextFieldDefaults.colors(
        focusedContainerColor   = theme.inputBackground,
        unfocusedContainerColor = theme.inputBackground,
        focusedIndicatorColor   = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        focusedTextColor        = theme.text,
        unfocusedTextColor      = theme.text
    )
