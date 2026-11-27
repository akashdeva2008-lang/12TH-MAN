package com.example.twelveman.data.api

import android.content.Context
import android.os.Build
import com.example.twelveman.TwelveManApplication
import com.example.twelveman.data.local.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AiRetrofitClient {

    const val DEFAULT_LAN_IP = "172.23.53.204"
    const val DEFAULT_EMULATOR_IP = "10.0.2.2"
    const val AI_PORT = 8000

    @Volatile
    private var aiApiServiceInstance: AiApiService? = null

    @Volatile
    private var currentAiBaseUrl: String? = null

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    fun getBaseUrl(context: Context? = null): String {
        val appContext = context?.applicationContext
            ?: try { TwelveManApplication.instance.applicationContext } catch (e: Exception) { null }

        val customServerUrl = appContext?.let { SessionManager.getInstance(it).getCustomServerUrl() }
        if (!customServerUrl.isNullOrBlank()) {
            val cleanHost = customServerUrl
                .replace("http://", "")
                .replace("https://", "")
                .split("/")[0]
                .split(":")[0]
            return "http://$cleanHost:$AI_PORT/"
        }

        return if (isEmulator()) {
            "http://$DEFAULT_EMULATOR_IP:$AI_PORT/"
        } else {
            "http://$DEFAULT_LAN_IP:$AI_PORT/"
        }
    }

    fun getAiApiService(context: Context? = null): AiApiService {
        val appContext = context?.applicationContext
            ?: try { TwelveManApplication.instance.applicationContext } catch (e: Exception) { null }

        val targetUrl = getBaseUrl(appContext)

        return aiApiServiceInstance?.takeIf { currentAiBaseUrl == targetUrl } ?: synchronized(this) {
            currentAiBaseUrl = targetUrl
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val gson = GsonBuilder().setLenient().create()

            Retrofit.Builder()
                .baseUrl(targetUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(AiApiService::class.java)
                .also { aiApiServiceInstance = it }
        }
    }
}
