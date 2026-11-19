package com.example.twelveman.data.api

import com.example.twelveman.data.model.AiChatRequest
import com.example.twelveman.data.model.AiChatResponse
import com.example.twelveman.data.model.AiModelInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AiApiService {

    @POST("chat")
    suspend fun chat(
        @Body request: AiChatRequest
    ): Response<AiChatResponse>

    @GET("model-info")
    suspend fun getModelInfo(): Response<AiModelInfoResponse>

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}
