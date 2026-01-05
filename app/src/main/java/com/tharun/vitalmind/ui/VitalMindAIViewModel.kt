package com.tharun.vitalmind.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VitalMindAIViewModel : ViewModel() {
    private val _chatHistory = MutableStateFlow<List<Message>>(emptyList())
    val chatHistory: StateFlow<List<Message>> = _chatHistory.asStateFlow()

    private val _userMessage = MutableStateFlow("")
    val userMessage: StateFlow<String> = _userMessage.asStateFlow()

    fun setUserMessage(msg: String) {
        _userMessage.value = msg
    }

    fun addUserMessage(msg: String) {
        _chatHistory.update { it + Message("user", msg) }
    }

    fun addAIMessage(msg: String) {
        _chatHistory.update { it + Message("assistant", msg) }
    }

    fun clearHistory() {
        _chatHistory.value = emptyList()
    }
    fun clearUserMessage() {
        _userMessage.value = ""
    }
}

