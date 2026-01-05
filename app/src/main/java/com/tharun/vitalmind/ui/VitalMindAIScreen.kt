package com.tharun.vitalmind.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalMindAIScreen(viewModel: MainViewModel) {
    val suggestedQuestions = listOf(
        "How active have I been this week?",
        "Am I sleeping well?",
        "What should I do today?"
    )
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    suspend fun getGroqAIResponse(userMsg: String, weeklySteps: List<Pair<String, Float>>, weeklyCalories: List<Pair<String, Float>>): String {
        val apiKey = BuildConfig.GROQ_API_KEY
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val weekSummary = buildString {
            append("User's last 7 days activity (steps, calories):\n")
            for (i in weeklySteps.indices) {
                val day = weeklySteps.getOrNull(i)?.first ?: "-"
                val steps = weeklySteps.getOrNull(i)?.second ?: 0f
                val calories = weeklyCalories.getOrNull(i)?.second ?: 0f
                append("$day: $steps steps, $calories kcal\n")
            }
        }
        val prompt = "You are a health assistant. Use the following 7-day activity data to answer the user's question.\n$weekSummary\nUser: $userMsg\nAI:"
        val requestBody = GroqRequest(
            model = "llama3-8b-8192", // or your preferred model
            messages = listOf(GroqMessage(role = "user", content = prompt)),
            maxTokens = 256
        )
        return try {
            val response: GroqResponse = client.post("https://api.groq.com/openai/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                headers { append("Authorization", "Bearer $apiKey") }
                setBody(requestBody)
            }.body()
            response.choices.firstOrNull()?.message?.content?.trim() ?: "Sorry, I couldn't generate a response."
        } catch (e: Exception) {
            Log.e("GroqAI", "Error contacting Groq API", e)
            "Sorry, there was a problem contacting the AI service.\n${e.localizedMessage ?: "Unknown error"}"
        } finally {
            client.close()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VitalMind AI") })
        },
        bottomBar = {
            // Increase bottom padding to lift the input bar further above the navigation bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 110.dp) // Increased from 80.dp to 110.dp
            ) {
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask a question...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (input.isNotBlank()) {
                                isLoading = true
                                chatMessages = chatMessages + ChatMessage(input, true)
                                coroutineScope.launch {
                                    val aiResponse = getGroqAIResponse(input, state.weeklySteps, state.weeklyCalories)
                                    chatMessages = chatMessages + ChatMessage(aiResponse, false)
                                    isLoading = false
                                }
                                input = ""
                                showSuggestions = false
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            isLoading = true
                            chatMessages = chatMessages + ChatMessage(input, true)
                            coroutineScope.launch {
                                val aiResponse = getGroqAIResponse(input, state.weeklySteps, state.weeklyCalories)
                                chatMessages = chatMessages + ChatMessage(aiResponse, false)
                                isLoading = false
                            }
                            input = ""
                            showSuggestions = false
                        }
                    }) {
                        Icon(Icons.Default.Chat, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        // Center the chat UI and wrap it in a curved box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 110.dp), // Increased from 80.dp to 110.dp
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AI assistant is currently in prototype mode.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (showSuggestions && chatMessages.isEmpty()) {
                        Text("Try asking:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        suggestedQuestions.forEach { q ->
                            SuggestionChip(text = q, onClick = {
                                input = q
                                showSuggestions = false
                            })
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (isLoading) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        items(chatMessages.size) { idx ->
                            val msg = chatMessages[chatMessages.size - 1 - idx]
                            ChatBubble(msg)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (msg.isUser) 4.dp else 2.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = if (msg.isUser) Color.White else Color.Black,
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 256
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)
