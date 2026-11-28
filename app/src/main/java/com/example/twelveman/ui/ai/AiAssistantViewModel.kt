package com.example.twelveman.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.twelveman.ai.ModelDownloadManager
import com.example.twelveman.ai.ModelDownloadState
import com.example.twelveman.ai.SmolLMModelManager
import com.example.twelveman.data.model.AiChatMessage
import com.example.twelveman.data.model.AiModelInfoResponse
import com.example.twelveman.data.repository.AiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val downloadManager = ModelDownloadManager(appContext)
    private val repository = AiRepository(appContext)

    private val _messages = MutableLiveData<List<AiChatMessage>>()
    val messages: LiveData<List<AiChatMessage>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _downloadState = MutableLiveData<ModelDownloadState>()
    val downloadState: LiveData<ModelDownloadState> = _downloadState

    private val _modelInfo = MutableLiveData<AiModelInfoResponse?>()
    val modelInfo: LiveData<AiModelInfoResponse?> = _modelInfo

    private var lastPrompt: String? = null
    private var downloadJob: Job? = null
    private var generationJob: Job? = null

    init {
        val welcomeMessage = AiChatMessage(
            text = "Welcome to 12THMAN GPT-1 powered by SmolLM2-1.7B-Instruct (on-device via llama.cpp).\n\nAsk me about cricket rules, LBW, batting & bowling techniques, fielding positions, match strategies, or tournament formats!",
            isUser = false
        )
        _messages.value = listOf(welcomeMessage)
        refreshModelStatus()
    }

    fun refreshModelStatus() {
        when {
            SmolLMModelManager.isModelLoaded() -> {
                _downloadState.value = ModelDownloadState.ModelReady
            }
            downloadManager.isModelValid() -> {
                _downloadState.value = ModelDownloadState.Downloaded
                loadSmolLMModel()
            }
            else -> {
                _downloadState.value = ModelDownloadState.NotDownloaded
            }
        }
    }

    fun isOnMeteredNetwork(): Boolean = downloadManager.isOnMeteredConnection()

    fun getAvailableStorageMb(): Long = downloadManager.getAvailableStorageBytes() / (1024L * 1024L)

    fun startModelDownload() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            downloadManager.downloadModel().collect { state ->
                _downloadState.value = state
                if (state is ModelDownloadState.Downloaded) {
                    loadSmolLMModel()
                }
            }
        }
    }

    fun cancelModelDownload() {
        downloadManager.cancelDownload()
        downloadJob?.cancel()
        downloadJob = null
    }

    fun loadSmolLMModel() {
        if (!downloadManager.isModelValid()) {
            _downloadState.value = ModelDownloadState.NotDownloaded
            return
        }
        viewModelScope.launch {
            _downloadState.value = ModelDownloadState.LoadingModel
            val result = SmolLMModelManager.ensureModelLoaded(appContext, downloadManager.modelFile)
            result.onSuccess {
                _downloadState.value = ModelDownloadState.ModelReady
            }.onFailure { err ->
                _downloadState.value = ModelDownloadState.Error(
                    err.localizedMessage ?: "Failed to load SmolLM2 model into memory."
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isLoading.value == true) return

        lastPrompt = trimmed

        val historySnapshot = _messages.value.orEmpty().drop(1)
        val userMsg = AiChatMessage(text = trimmed, isUser = true)
        val currentList = _messages.value.orEmpty().toMutableList()
        currentList.add(userMsg)
        _messages.value = currentList
        _isLoading.value = true

        generationJob = viewModelScope.launch {
            try {
                val result = repository.sendMessageWithSmolLM(trimmed, historySnapshot)
                result.onSuccess { response ->
                    streamResponseIntoChat(
                        fullText = response.response,
                        latencyMs = response.latencyMs,
                        tokensGenerated = response.tokensGenerated
                    )
                    _downloadState.value = ModelDownloadState.ModelReady
                }.onFailure { error ->
                    val updatedList = _messages.value.orEmpty().toMutableList()
                    updatedList.add(
                        AiChatMessage(
                            text = "Unable to generate response with SmolLM2: ${error.localizedMessage ?: "Model not ready."}",
                            isUser = false,
                            isError = true
                        )
                    )
                    _messages.value = updatedList
                }
            } catch (ce: CancellationException) {
                val updatedList = _messages.value.orEmpty().toMutableList()
                updatedList.add(
                    AiChatMessage(
                        text = "Generation stopped by user.",
                        isUser = false
                    )
                )
                _messages.value = updatedList
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun streamResponseIntoChat(
        fullText: String,
        latencyMs: Double,
        tokensGenerated: Int
    ) {
        val words = fullText.split(" ")
        if (words.size <= 6) {
            val updatedList = _messages.value.orEmpty().toMutableList()
            updatedList.add(
                AiChatMessage(
                    text = fullText,
                    isUser = false,
                    latencyMs = latencyMs,
                    tokensGenerated = tokensGenerated
                )
            )
            _messages.value = updatedList
            return
        }

        val streamingMsg = AiChatMessage(text = "", isUser = false)
        val listWithPlaceholder = _messages.value.orEmpty().toMutableList()
        listWithPlaceholder.add(streamingMsg)
        _messages.value = listWithPlaceholder

        val sb = StringBuilder()
        val chunkSize = 3
        var i = 0
        while (i < words.size) {
            val end = (i + chunkSize).coerceAtMost(words.size)
            for (j in i until end) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append(words[j])
            }
            val currentList = _messages.value.orEmpty().toMutableList()
            val idx = currentList.indexOfFirst { it.id == streamingMsg.id }
            if (idx != -1) {
                val isLast = end >= words.size
                currentList[idx] = streamingMsg.copy(
                    text = sb.toString(),
                    latencyMs = if (isLast) latencyMs else null,
                    tokensGenerated = if (isLast) tokensGenerated else null
                )
                _messages.value = currentList
            }
            i = end
            if (i < words.size) delay(28L)
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isLoading.value = false
    }

    fun regenerateLastResponse() {
        val prompt = lastPrompt ?: return
        if (_isLoading.value == true) return
        val currentList = _messages.value.orEmpty().toMutableList()
        if (currentList.isNotEmpty() && !currentList.last().isUser) {
            currentList.removeAt(currentList.size - 1)
            _messages.value = currentList
        }
        sendMessage(prompt)
    }

    fun retryLast() {
        lastPrompt?.let { sendMessage(it) }
    }

    fun clearHistory() {
        stopGeneration()
        val welcomeMessage = AiChatMessage(
            text = "Conversation cleared. Ask me anything about cricket rules, batting, bowling, fielding, or match tactics!",
            isUser = false
        )
        _messages.value = listOf(welcomeMessage)
    }
}
