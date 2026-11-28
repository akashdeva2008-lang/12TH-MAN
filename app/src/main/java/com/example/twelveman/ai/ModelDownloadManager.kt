package com.example.twelveman.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : ModelDownloadState()
    object Downloaded : ModelDownloadState()
    object LoadingModel : ModelDownloadState()
    object ModelReady : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

class ModelDownloadManager(private val context: Context) {

    companion object {
        const val MODEL_NAME = "SmolLM2-1.7B-Instruct (Q4_K_M)"
        const val MODEL_FILENAME = "smollm2-1.7b-instruct-q4_k_m.gguf"
        const val MODEL_URL =
            "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf"
        const val EXPECTED_SIZE_BYTES = 1_055_609_536L // ~1.06 GB
        private const val MIN_VALID_SIZE_BYTES = 900_000_000L
        private const val REQUIRED_FREE_BYTES = 1_200_000_000L
    }

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }

    val modelFile: File
        get() = File(modelsDir, MODEL_FILENAME)

    private val partFile: File
        get() = File(modelsDir, "$MODEL_FILENAME.part")

    @Volatile
    private var cancelRequested = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun isModelValid(): Boolean {
        val file = modelFile
        if (!file.exists() || file.length() < MIN_VALID_SIZE_BYTES) return false
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(4)
                val read = raf.read(header)
                read == 4 && String(header, Charsets.US_ASCII) == "GGUF"
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    fun getPartialDownloadedBytes(): Long {
        return if (partFile.exists()) partFile.length() else 0L
    }

    fun isConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun isOnMeteredConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return cm.isActiveNetworkMetered
    }

    fun cancelDownload() {
        cancelRequested = true
    }

    fun deleteModel() {
        if (modelFile.exists()) modelFile.delete()
        if (partFile.exists()) partFile.delete()
    }

    fun downloadModel(): Flow<ModelDownloadState> = flow {
        cancelRequested = false

        if (isModelValid()) {
            emit(ModelDownloadState.Downloaded)
            return@flow
        }

        if (!isConnected()) {
            emit(ModelDownloadState.Error("No internet connection. Connect to Wi-Fi or mobile data and retry."))
            return@flow
        }

        val existingPartBytes = if (partFile.exists()) partFile.length() else 0L
        val neededBytes = (EXPECTED_SIZE_BYTES - existingPartBytes).coerceAtLeast(100_000_000L)
        val freeBytes = getAvailableStorageBytes()
        if (freeBytes < neededBytes + 100_000_000L && freeBytes < REQUIRED_FREE_BYTES - existingPartBytes) {
            val freeMb = freeBytes / (1024 * 1024)
            emit(ModelDownloadState.Error("Insufficient storage ($freeMb MB free). At least 1.2 GB free space is required."))
            return@flow
        }

        try {
            val requestBuilder = Request.Builder().url(MODEL_URL)
            if (existingPartBytes > 0L) {
                requestBuilder.header("Range", "bytes=$existingPartBytes-")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    emit(ModelDownloadState.Error("Download failed (HTTP ${response.code}). Please retry."))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(ModelDownloadState.Error("Empty response from Hugging Face."))
                    return@flow
                }

                val isResuming = (response.code == 206 && existingPartBytes > 0L)
                val contentLength = body.contentLength()
                val totalBytes = when {
                    isResuming && contentLength > 0 -> existingPartBytes + contentLength
                    contentLength > 0 -> contentLength
                    else -> EXPECTED_SIZE_BYTES
                }

                var downloaded = if (isResuming) existingPartBytes else 0L
                val buffer = ByteArray(64 * 1024)
                var lastEmitTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(partFile, isResuming).use { output ->
                        while (true) {
                            if (cancelRequested) {
                                emit(ModelDownloadState.Error("Download paused/cancelled. Tap Download to resume."))
                                return@flow
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 250L || downloaded >= totalBytes) {
                                lastEmitTime = now
                                val percent = ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                emit(ModelDownloadState.Downloading(percent, downloaded, totalBytes))
                            }
                        }
                        output.flush()
                    }
                }

                if (cancelRequested) {
                    emit(ModelDownloadState.Error("Download cancelled."))
                    return@flow
                }

                if (modelFile.exists()) modelFile.delete()
                if (!partFile.renameTo(modelFile)) {
                    partFile.copyTo(modelFile, overwrite = true)
                    partFile.delete()
                }

                if (isModelValid()) {
                    emit(ModelDownloadState.Downloaded)
                } else {
                    modelFile.delete()
                    emit(ModelDownloadState.Error("Downloaded file failed GGUF integrity check. Please retry."))
                }
            }
        } catch (e: Exception) {
            emit(ModelDownloadState.Error("Download interrupted: ${e.localizedMessage ?: "Network error"}. Tap Retry to resume."))
        }
    }.flowOn(Dispatchers.IO)
}
