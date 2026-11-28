package com.example.twelveman.ai

import com.example.twelveman.data.model.AiChatMessage
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class SmolLMGenerationOutput(
    val text: String,
    val tokensGenerated: Int,
    val latencyMs: Double,
    val tokensPerSecond: Float
)

class SmolLMInferenceEngine {

    companion object {
        const val MAX_GENERATION_TOKENS = 320

        const val CRICKET_SYSTEM_PROMPT =
            "You are 12THMAN GPT-1, a helpful and knowledgeable cricket expert assistant inside the 12THMAN Cricket Management App. " +
                "Answer questions clearly in simple English about: cricket rules and MCC regulations, batting techniques, bowling variations and techniques, " +
                "fielding positions, umpiring decisions (LBW, no-ball, wide, bye, leg-bye, run-out), T20, ODI, and Test match formats, " +
                "match strategies, tournament organization, cricket terminology, and how to read cricket statistics and scorecards. " +
                "Keep answers concise, accurate, and focused on cricket. Do not invent match scores, player career statistics, or fake records."
    }

    suspend fun generateReply(
        model: LlamaModel,
        userPrompt: String,
        history: List<AiChatMessage>
    ): Result<SmolLMGenerationOutput> = withContext(Dispatchers.IO) {
        try {
            coroutineContext.ensureActive()

            // Include last few turns for multi-turn context without exceeding 2048 tokens
            val recentTurns = history
                .filter { !it.isError && it.text.isNotBlank() }
                .takeLast(6)

            val formattedPrompt = buildString {
                if (recentTurns.isNotEmpty()) {
                    append("Recent conversation:\n")
                    for (msg in recentTurns) {
                        val role = if (msg.isUser) "User" else "Assistant"
                        val cleanText = msg.text.take(400)
                        append("$role: $cleanText\n")
                    }
                    append("\n")
                }
                append("User: $userPrompt")
            }

            val startNs = System.nanoTime()
            val result = Llama.complete(
                model = model,
                prompt = formattedPrompt,
                systemPrompt = CRICKET_SYSTEM_PROMPT,
                maxTokens = MAX_GENERATION_TOKENS
            )
            coroutineContext.ensureActive()

            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
            val cleanReply = result.text
                .replace("<|im_end|>", "")
                .replace("<|im_start|>", "")
                .trim()

            if (cleanReply.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("SmolLM2 generated an empty response."))
            }

            Result.success(
                SmolLMGenerationOutput(
                    text = cleanReply,
                    tokensGenerated = result.tokensGenerated,
                    latencyMs = if (result.generateTimeMs > 0) result.generateTimeMs.toDouble() else elapsedMs,
                    tokensPerSecond = result.tokensPerSecond
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (oom: OutOfMemoryError) {
            Result.failure(OutOfMemoryError("Out of memory during inference. Try clearing chat history or closing background apps."))
        } catch (t: Throwable) {
            Result.failure(Exception("Inference failed: ${t.localizedMessage ?: t.javaClass.simpleName}", t))
        }
    }
}
