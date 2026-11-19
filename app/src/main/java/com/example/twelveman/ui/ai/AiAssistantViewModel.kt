package com.example.twelveman.ui.ai

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.AiChatMessage
import com.example.twelveman.data.model.AiModelInfoResponse
import com.example.twelveman.data.repository.AiRepository
import kotlinx.coroutines.launch

class AiAssistantViewModel : ViewModel() {

    private val repository = AiRepository()

    private val _messages = MutableLiveData<List<AiChatMessage>>()
    val messages: LiveData<List<AiChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _modelInfo = MutableLiveData<AiModelInfoResponse?>()
    val modelInfo: LiveData<AiModelInfoResponse?> = _modelInfo

    private var lastPrompt: String? = null

    init {
        // Welcome message explaining 12THMAN GPT-1
        val welcomeMessage = AiChatMessage(
            text = "🏏 Welcome to 12THMAN GPT-1!\n\nI am your custom cricket AI assistant, powered by a 4-layer Transformer model trained from scratch with PyTorch (zero pretrained weights). Ask me about cricket rules, match strategies, bowling variations, batting techniques, or tournament formats!",
            isUser = false
        )
        _messages.value = listOf(welcomeMessage)
        fetchModelInfo()
    }

    private fun fetchModelInfo() {
        viewModelScope.launch {
            val result = repository.getModelInfo()
            result.onSuccess { info ->
                _modelInfo.value = info
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value == true) return

        lastPrompt = trimmed

        val userMsg = AiChatMessage(text = trimmed, isUser = true)
        val currentList = _messages.value.orEmpty().toMutableList()
        currentList.add(userMsg)
        _messages.value = currentList
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendMessage(trimmed)
            _isLoading.value = false

            val updatedList = _messages.value.orEmpty().toMutableList()
            result.onSuccess { response ->
                val botMsg = AiChatMessage(
                    text = response.response,
                    isUser = false,
                    latencyMs = response.latencyMs,
                    tokensGenerated = response.tokensGenerated
                )
                updatedList.add(botMsg)
            }.onFailure { error ->
                val errorMsg = AiChatMessage(
                    text = "⚠️ Could not connect to 12THMAN GPT-1 service: ${error.localizedMessage ?: "Unknown error"}. Check if backend is running.",
                    isUser = false,
                    isError = true
                )
                updatedList.add(errorMsg)
            }
            _messages.value = updatedList
        }
    }

    fun retryLast() {
        lastPrompt?.let { sendMessage(it) }
    }

    fun clearHistory() {
        val welcomeMessage = AiChatMessage(
            text = "🏏 Conversation reset. Ask me anything about cricket rules, shots, bowling types, or match scenarios!",
            isUser = false
        )
        _messages.value = listOf(welcomeMessage)
    }
}
