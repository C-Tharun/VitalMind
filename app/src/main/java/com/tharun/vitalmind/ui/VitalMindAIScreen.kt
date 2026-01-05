package com.tharun.vitalmind.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalMindAIScreen() {
    val suggestedQuestions = listOf(
        "How active have I been this week?",
        "Am I sleeping well?",
        "What should I do today?"
    )
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(true) }

    fun getAIResponse(userMsg: String): String {
        return when {
            userMsg.contains("active", true) -> "Based on your recent activity data, your movement this week is slightly below your average."
            userMsg.contains("sleep", true) -> "Your sleep duration has varied recently. Maintaining a consistent schedule may help."
            userMsg.contains("do", true) -> "A light walk or stretching activity would be beneficial today."
            else -> "I'm here to help with your health data!"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("VitalMind AI") })
        },
        bottomBar = {
            // Add extra bottom padding to avoid overlap with navigation bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 80.dp) // Adjust as needed for nav bar height
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
                                chatMessages = chatMessages + ChatMessage(input, true)
                                chatMessages = chatMessages + ChatMessage(getAIResponse(input), false)
                                input = ""
                                showSuggestions = false
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            chatMessages = chatMessages + ChatMessage(input, true)
                            chatMessages = chatMessages + ChatMessage(getAIResponse(input), false)
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
                .padding(bottom = 80.dp), // Ensure content is above nav bar and input bar
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
