@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.tharun.vitalmind.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyListState

@Serializable
data class GroqRequest(val model: String, val messages: List<Message>)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class GroqResponse(val choices: List<Choice> = emptyList())

@Serializable
data class Choice(val message: Message)

fun generateHealthSummary(
    steps: List<Pair<String, Float>> = emptyList(),
    calories: List<Pair<String, Float>> = emptyList(),
    distance: List<Pair<String, Float>> = emptyList(),
    heartRate: List<Pair<String, Float>> = emptyList(),
    sleep: List<Pair<String, Float>> = emptyList(),
    activity: List<Pair<String, String>> = emptyList(),
    lastActivity: String = "None",
    lastActivityTime: String = ""
): String {
    return buildString {
        append("User's health summary for the past 7 days:\n")
        if (steps.isNotEmpty()) append("Steps: ${steps.joinToString { it.second.toInt().toString() }}\n")
        if (calories.isNotEmpty()) append("Calories: ${calories.joinToString { it.second.toInt().toString() }}\n")
        if (distance.isNotEmpty()) append("Distance: ${distance.joinToString { String.format("%.2f", it.second) }}\n")
        if (heartRate.isNotEmpty()) append("Heart Rate: ${heartRate.joinToString { it.second.toString() }}\n")
        if (sleep.isNotEmpty()) append("Sleep: ${sleep.joinToString { String.format("%.1f", it.second) }}\n")
        if (activity.isNotEmpty()) append("Activity: ${activity.joinToString { it.second }}\n")
        if (lastActivity != "None") append("Last Activity: $lastActivity at $lastActivityTime\n")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalMindAIScreen(
    dashboardState: DashboardState,
    listState: LazyListState,
    aiViewModel: VitalMindAIViewModel = viewModel()
) {
    val userMessage by aiViewModel.userMessage.collectAsState()
    val chatHistory by aiViewModel.chatHistory.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showSuggestions by remember { mutableStateOf(chatHistory.isEmpty()) }

    // Generate the health summary from DashboardState
    val healthSummary = remember(dashboardState) {
        generateHealthSummary(
            steps = dashboardState.weeklySteps,
            calories = dashboardState.weeklyCalories,
            distance = dashboardState.weeklyDistance,
            heartRate = dashboardState.weeklyHeartRate,
            sleep = dashboardState.weeklySleep,
            activity = dashboardState.weeklyActivity,
            lastActivity = dashboardState.lastActivity,
            lastActivityTime = dashboardState.lastActivityTime
        )
    }

    // Placeholder Q&A
    val suggestions = listOf(
        "How active have I been this week?",
        "Am I sleeping well?",
        "What should I do today?"
    )

    // Helper to build the prompt for the AI
    fun getPromptMessages(): List<Message> {
        val prompt = if (healthSummary.isNotBlank())
            listOf(Message("system", healthSummary)) + chatHistory
        else
            chatHistory
        // Debug: Log the prompt to Logcat
        Log.d("VitalMindAI", "Prompt sent to AI: " + prompt.joinToString("\n") { "[${it.role}] ${it.content}" })
        return prompt
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VitalMind AI",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        aiViewModel.clearHistory()
                        aiViewModel.clearUserMessage()
                        showSuggestions = true
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Clear chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            // Chat UI container with rounded corners and elevation
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp, 8.dp, 8.dp, 80.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 500.dp),
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (showSuggestions && chatHistory.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                androidx.compose.foundation.shape.CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("💡", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Try asking:",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                suggestions.forEach { q ->
                                    FilledTonalButton(
                                        onClick = {
                                            aiViewModel.setUserMessage(q)
                                            showSuggestions = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            q,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatHistory) { message ->
                            val isUser = message.role == "user"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isUser) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (isUser) 20.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 20.dp
                                    ),
                                    shadowElevation = if (isUser) 2.dp else 0.dp,
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(16.dp, 12.dp),
                                        color = if (isUser) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    // Input area above navigation bar, inside chat container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = userMessage,
                                    onValueChange = { aiViewModel.setUserMessage(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Ask about your health...") },
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 3
                                )
                                FilledIconButton(
                                    onClick = {
                                        if (userMessage.isNotBlank()) {
                                            aiViewModel.addUserMessage(userMessage)
                                            aiViewModel.setUserMessage("")
                                            showSuggestions = false
                                            coroutineScope.launch {
                                                val client = HttpClient(CIO) {
                                                    install(ContentNegotiation) {
                                                        json(Json { ignoreUnknownKeys = true })
                                                    }
                                                }
                                                val response = getGroqAIResponse(client, getPromptMessages())
                                                val aiReply = response?.choices?.firstOrNull()?.message?.content ?: "Sorry, I couldn't get a response from the AI."
                                                aiViewModel.addAIMessage(aiReply)
                                                client.close()
                                            }
                                        }
                                    },
                                    enabled = userMessage.isNotBlank(),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "AI responses are AI-generated and for informational purposes only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun getGroqAIResponse(client: HttpClient, messages: List<Message>): GroqResponse? {
    return try {
        val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            setBody(GroqRequest("llama-3.1-8b-instant", messages))
        }
        response.body<GroqResponse>()
    } catch (e: Exception) {
        Log.e("GroqAI", "Error contacting Groq API", e)
        throw e // propagate to show in chat
    }
}
