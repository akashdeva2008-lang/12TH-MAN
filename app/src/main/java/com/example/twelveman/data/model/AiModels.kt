package com.example.twelveman.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Data models for 12THMAN GPT-1 Cricket AI Assistant.
 */

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val latencyMs: Double? = null,
    val tokensGenerated: Int? = null
)

data class AiChatRequest(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 64,
    @SerializedName("top_k") val topK: Int = 25
)

data class AiChatResponse(
    @SerializedName("response") val response: String,
    @SerializedName("model") val model: String = "12THMAN GPT-1",
    @SerializedName("tokens_generated") val tokensGenerated: Int = 0,
    @SerializedName("latency_ms") val latencyMs: Double = 0.0,
    @SerializedName("status") val status: String = "success"
)

data class AiModelInfoResponse(
    @SerializedName("name") val name: String = "12THMAN GPT-1",
    @SerializedName("type") val type: String = "Custom Cricket Language Model",
    @SerializedName("architecture") val architecture: String = "Decoder-Only Transformer (GPT)",
    @SerializedName("pretrained_weights") val pretrainedWeights: Boolean = false,
    @SerializedName("weights_origin") val weightsOrigin: String = "Trained from scratch",
    @SerializedName("parameters") val parameters: Long = 910464,
    @SerializedName("vocab_size") val vocabSize: Int = 823,
    @SerializedName("n_layer") val nLayer: Int = 4,
    @SerializedName("n_head") val nHead: Int = 4,
    @SerializedName("n_embd") val nEmbd: Int = 128,
    @SerializedName("block_size") val blockSize: Int = 128,
    @SerializedName("val_loss") val valLoss: Double = 2.2984,
    @SerializedName("checkpoint_file") val checkpointFile: String = "12thman_gpt1_best.pt"
)
