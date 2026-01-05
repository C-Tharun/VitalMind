@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.tharun.vitalmind.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tharun.vitalmind.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api

@Serializable
data class GroqRequest(val model: String, val messages: List<Message>)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class GroqResponse(val choices: List<Choice> = emptyList())

@Serializable
data class Choice(val message: Message)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalMindAIScreen() {
    var userMessage by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Message>()) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showSuggestions by remember { mutableStateOf(chatHistory.isEmpty()) }

    // Placeholder Q&A
    val suggestions = listOf(
        "How active have I been this week?",
        "Am I sleeping well?",
        "What should I do today?"
    )
    val aiResponses = mapOf(
        suggestions[0] to "Based on your recent activity data, your movement this week is slightly below your average.",
        suggestions[1] to "Your sleep duration has varied recently. Maintaining a consistent schedule may help.",
        suggestions[2] to "A light walk or stretching activity would be beneficial today."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VitalMind AI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        chatHistory = emptyList()
                        showSuggestions = true
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        // Move input above navigation bar by removing bottomBar and placing input inside main Column
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Chat UI container with rounded corners and elevation
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp, 8.dp, 8.dp, 80.dp) // leave space for nav bar
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (showSuggestions) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Try asking:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                suggestions.forEach { q ->
                                    OutlinedButton(
                                        onClick = {
                                            userMessage = q
                                            showSuggestions = false
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                    ) {
                                        Text(q, textAlign = TextAlign.Start)
                                    }
                                }
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(chatHistory) { message ->
                            val isUser = message.role == "user"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(24.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(18.dp, 12.dp),
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    // Input area above navigation bar, inside chat container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 0.dp, start = 0.dp, end = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = userMessage,
                                onValueChange = { userMessage = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type your message...") },
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (userMessage.isNotBlank()) {
                                        val history = chatHistory.toMutableList()
                                        history.add(Message("user", userMessage))
                                        // Use placeholder response if available, else generic
                                        val aiReply = aiResponses[userMessage.trim()] ?: "I'm here to help! (Prototype mode)"
                                        history.add(Message("assistant", aiReply))
                                        chatHistory = history
                                        userMessage = ""
                                        showSuggestions = false
                                    }
                                },
                                enabled = userMessage.isNotBlank()
                            ) {
                                Text("Send")
                            }
                        }
                        Text(
                            "AI assistant is currently in prototype mode.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private suspend fun getGroqAIResponse(client: HttpClient, messages: List<Message>): GroqResponse? {
    return try {
        val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
            contentType(ContentType.Application.Json)
            setBody(GroqRequest("llama-3.1-8b-instant", messages)) // Updated to requested model
        }
        response.body<GroqResponse>()
    } catch (e: Exception) {
        Log.e("GroqAI", "Error contacting Groq API", e)
        throw e // propagate to show in chat
    }
}
