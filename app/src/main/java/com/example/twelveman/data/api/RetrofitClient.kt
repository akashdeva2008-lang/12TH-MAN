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

object RetrofitClient {

    // Default Host PC IP (Current LAN IP): 172.23.51.43
    const val DEFAULT_LAN_IP = "172.23.51.43"
    const val DEFAULT_EMULATOR_IP = "10.0.2.2"

    @Volatile
    private var apiServiceInstance: ApiService? = null

    @Volatile
    private var currentBaseUrl: String? = null

    fun isEmulator(): Boolean {
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

        val customUrl = appContext?.let { SessionManager.getInstance(it).getCustomServerUrl() }
        if (!customUrl.isNullOrBlank()) {
            return formatUrl(customUrl)
        }

        return if (isEmulator()) {
            "http://$DEFAULT_EMULATOR_IP/12THMAN_BACKEND/"
        } else {
            // Physical Phone defaults to host PC LAN IP
            "http://$DEFAULT_LAN_IP/12THMAN_BACKEND/"
        }
    }

    fun setServerUrl(context: Context, newUrl: String) {
        val formatted = formatUrl(newUrl)
        SessionManager.getInstance(context).saveCustomServerUrl(formatted)
        synchronized(this) {
            apiServiceInstance = null
            currentBaseUrl = formatted
        }
    }

    fun formatUrl(input: String): String {
        var url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (!url.contains("/12THMAN_BACKEND")) {
            url = url.trimEnd('/') + "/12THMAN_BACKEND/"
        }
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        return url
    }

    fun getApiService(context: Context? = null): ApiService {
        val appContext = context?.applicationContext 
            ?: try { TwelveManApplication.instance.applicationContext } catch (e: Exception) { null }

        val targetUrl = getBaseUrl(appContext)

        return apiServiceInstance?.takeIf { currentBaseUrl == targetUrl } ?: synchronized(this) {
            currentBaseUrl = targetUrl
            buildRetrofit(appContext, targetUrl).create(ApiService::class.java).also {
                apiServiceInstance = it
            }
        }
    }

    private fun buildRetrofit(context: Context?, baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (context != null) {
            clientBuilder.addInterceptor(AuthInterceptor(context))
        }

        val gson = GsonBuilder().create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
