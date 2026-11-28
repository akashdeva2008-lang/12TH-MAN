package com.example.twelveman.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object SmolLMModelManager {

    const val CONTEXT_SIZE = 2048
    private const val MIN_AVAILABLE_RAM_BYTES = 550L * 1024L * 1024L

    private val loadMutex = Mutex()

    @Volatile
    private var loadedModel: LlamaModel? = null

    @Volatile
    private var loadedModelPath: String? = null

    fun isDeviceArchitectureSupported(): Boolean {
        return Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }
    }

    fun getMemoryStatus(context: Context): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        return Pair(info.availMem, info.totalMem)
    }

    fun isModelLoaded(): Boolean {
        return loadedModel?.isLoaded == true
    }

    fun getLoadedModel(): LlamaModel? {
        return loadedModel?.takeIf { it.isLoaded }
    }

    fun getSystemInfoSafe(): String {
        return try {
            Llama.getSystemInfo()
        } catch (e: Throwable) {
            "ABI: ${Build.SUPPORTED_ABIS.joinToString()}"
        }
    }

    suspend fun ensureModelLoaded(context: Context, modelFile: File): Result<LlamaModel> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            val existing = loadedModel
            if (existing != null && existing.isLoaded && loadedModelPath == modelFile.absolutePath) {
                return@withContext Result.success(existing)
            }

            if (!isDeviceArchitectureSupported()) {
                return@withContext Result.failure(
                    IllegalStateException("On-device SmolLM2 requires a 64-bit arm64-v8a Android device (detected: ${Build.SUPPORTED_ABIS.joinToString()}).")
                )
            }

            if (!modelFile.exists() || modelFile.length() < 900_000_000L) {
                return@withContext Result.failure(
                    IllegalStateException("SmolLM2 GGUF model file is missing or incomplete. Please download it first.")
                )
            }

            val (availMem, totalMem) = getMemoryStatus(context)
            if (availMem > 0 && availMem < MIN_AVAILABLE_RAM_BYTES) {
                val availMb = availMem / (1024 * 1024)
                val totalMb = totalMem / (1024 * 1024)
                return@withContext Result.failure(
                    OutOfMemoryError("Insufficient free RAM ($availMb MB free / $totalMb MB total). Close background apps and retry.")
                )
            }

            try {
                // Release any previously loaded copy to avoid duplicate memory allocation
                existing?.let {
                    try {
                        Llama.releaseModel(it)
                    } catch (_: Throwable) { }
                    loadedModel = null
                    loadedModelPath = null
                }

                val cpuCount = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                val config = LlamaConfig(
                    contextSize = CONTEXT_SIZE,
                    threads = cpuCount,
                    gpuLayers = 0,
                    temperature = 0.6f,
                    topP = 0.9f,
                    topK = 40,
                    seed = 42
                )

                val model = Llama.loadModel(modelFile.absolutePath, config)
                loadedModel = model
                loadedModelPath = modelFile.absolutePath
                Result.success(model)
            } catch (oom: OutOfMemoryError) {
                loadedModel = null
                loadedModelPath = null
                Result.failure(OutOfMemoryError("Device ran out of memory while loading SmolLM2-1.7B. Close background apps and retry."))
            } catch (t: Throwable) {
                loadedModel = null
                loadedModelPath = null
                Result.failure(Exception("Failed to initialize SmolLM2 native engine: ${t.localizedMessage ?: t.javaClass.simpleName}", t))
            }
        }
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            loadedModel?.let {
                try {
                    Llama.releaseModel(it)
                } catch (_: Throwable) { }
            }
            loadedModel = null
            loadedModelPath = null
        }
    }
}
