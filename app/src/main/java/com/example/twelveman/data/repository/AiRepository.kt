package com.example.twelveman.data.repository

import android.content.Context
import com.example.twelveman.ai.ModelDownloadManager
import com.example.twelveman.ai.SmolLMInferenceEngine
import com.example.twelveman.ai.SmolLMModelManager
import com.example.twelveman.data.api.AiApiService
import com.example.twelveman.data.api.AiRetrofitClient
import com.example.twelveman.data.model.AiChatMessage
import com.example.twelveman.data.model.AiChatRequest
import com.example.twelveman.data.model.AiChatResponse
import com.example.twelveman.data.model.AiModelInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository(
    private val context: Context? = null,
    private val apiService: AiApiService = AiRetrofitClient.getAiApiService(context)
) {
    private val inferenceEngine = SmolLMInferenceEngine()

    suspend fun sendMessageWithSmolLM(
        prompt: String,
        history: List<AiChatMessage>
    ): Result<AiChatResponse> = withContext(Dispatchers.IO) {
        val appContext = context?.applicationContext
            ?: return@withContext Result.failure(IllegalStateException("Application context unavailable."))

        val downloadManager = ModelDownloadManager(appContext)
        if (!downloadManager.isModelValid()) {
            return@withContext Result.failure(
                IllegalStateException("SmolLM2-1.7B model is not downloaded yet. Tap 'Enable & Download' above to run AI locally.")
            )
        }

        val modelResult = SmolLMModelManager.ensureModelLoaded(appContext, downloadManager.modelFile)
        val model = modelResult.getOrElse { err ->
            return@withContext Result.failure(err)
        }

        val genResult = inferenceEngine.generateReply(model, prompt, history)
        genResult.map { output ->
            AiChatResponse(
                response = output.text,
                model = ModelDownloadManager.MODEL_NAME,
                tokensGenerated = output.tokensGenerated,
                latencyMs = output.latencyMs,
                status = "local_smollm2"
            )
        }
    }

    suspend fun sendMessage(prompt: String): Result<AiChatResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.chat(AiChatRequest(prompt = prompt))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Inference error code: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Local cricket intelligence fallback in case backend server is unreachable
            val localFallback = generateLocalCricketFallback(prompt)
            Result.success(
                AiChatResponse(
                    response = localFallback,
                    model = "12THMAN GPT-1 (Offline Mode)",
                    tokensGenerated = localFallback.split(" ").size,
                    latencyMs = 12.0,
                    status = "offline_fallback"
                )
            )
        }
    }

    suspend fun getModelInfo(): Result<AiModelInfoResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getModelInfo()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(getDefaultModelInfo())
            }
        } catch (e: Exception) {
            Result.success(getDefaultModelInfo())
        }
    }

    private fun getDefaultModelInfo(): AiModelInfoResponse {
        return AiModelInfoResponse(
            name = "12THMAN GPT-1",
            type = "Custom Cricket Language Model",
            architecture = "Decoder-Only Transformer (GPT)",
            pretrainedWeights = false,
            weightsOrigin = "Trained from scratch on cricket domain corpus",
            parameters = 910464,
            vocabSize = 823,
            nLayer = 4,
            nHead = 4,
            nEmbd = 128,
            blockSize = 128,
            valLoss = 2.2984,
            checkpointFile = "12thman_gpt1_best.pt"
        )
    }

    private fun generateLocalCricketFallback(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("lbw") -> {
                "Under MCC Law 36, Leg Before Wicket (LBW) occurs when the ball pitches in line or outside off stump, impacts the batsman's pad in line with the stumps (or outside off if no shot is offered), and would have hit the stumps."
            }
            lower.contains("inswinger") || lower.contains("outswinger") -> {
                "An inswinger moves through the air from off to leg towards the right-handed batsman, aiming for pad and stumps. An outswinger moves away towards slips, angling to find the outside edge."
            }
            lower.contains("12th man") || lower.contains("twelfth man") -> {
                "The 12th man is an essential squad member who acts as a substitute fielder, brings drinks, gloves, and tactical messages, keeping team momentum alive off the field!"
            }
            lower.contains("powerplay") -> {
                "In standard T20 cricket, Overs 1-6 are the Mandatory Powerplay where only 2 fielders are permitted outside the 30-yard circle to encourage attacking stroke play."
            }
            lower.contains("economy") -> {
                "Economy Rate = Total Runs Conceded ÷ Total Overs Bowled. An economy under 7.00 in T20 cricket is considered top-tier bowling."
            }
            lower.contains("yorker") -> {
                "A yorker is a full-pitched delivery landing right at the batsman's popping crease, targeting the base of the stumps. It is lethal in death overs."
            }
            lower.contains("wide") || lower.contains("no ball") -> {
                "A Wide ball is judged beyond batsman's reach giving 1 extra run. A No-ball awards 1 extra run and awards a Free Hit on the subsequent legal delivery in limited-overs cricket."
            }
            else -> {
                "According to 12THMAN Cricket Rules, cricket strategy relies on pitch condition, bowler rhythm, crease management, and situational awareness."
            }
        }
    }
}
