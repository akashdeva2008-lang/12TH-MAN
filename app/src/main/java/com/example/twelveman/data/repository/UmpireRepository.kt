package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.api.models.AuthResponse
import com.example.twelveman.data.api.models.UmpireRegistrationRequest
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Umpire
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class UmpireRepository {

    private val apiService get() = RetrofitClient.getApiService()

    suspend fun registerUmpire(request: UmpireRegistrationRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerUmpire(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string()
                val msg = try {
                    JsonParser.parseString(raw).asJsonObject.get("message")?.asString
                } catch (_: Exception) { null } ?: "Registration failed (HTTP ${response.code()})"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Unable to connect to server"}"))
        }
    }

    suspend fun fetchMyUmpireProfile(userId: Int = 0): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRoleProfile(userId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Unable to load umpire profile (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyUmpireProfile(payload: Map<String, Any>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateRoleProfile(payload)
            if (response.isSuccessful && response.body()?.status == "success") {
                Result.success(response.body()?.message ?: "Profile updated")
            } else {
                Result.failure(Exception("Failed to update profile (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pollUmpires(intervalMs: Long = 10000L): Flow<List<Umpire>> = flow {
        while (true) {
            try {
                val response = apiService.getUmpires()
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteUmpires = response.body()!!.data!!
                    MockData.umpires.clear()
                    MockData.umpires.addAll(remoteUmpires)
                }
            } catch (ignored: Exception) { }
            emit(MockData.umpires.toList())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun getUmpires(): Flow<List<Umpire>> = flow {
        // Emit cached data immediately
        emit(MockData.umpires)

        try {
            val response = apiService.getUmpires()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteUmpires = response.body()!!.data!!
                if (remoteUmpires.isNotEmpty()) {
                    MockData.umpires.clear()
                    MockData.umpires.addAll(remoteUmpires)
                    emit(remoteUmpires)
                }
            }
        } catch (e: Exception) {
            // Gracefully retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun getUmpireById(id: String): Umpire? {
        return MockData.umpires.find { it.id.equals(id, ignoreCase = true) }
    }

    fun searchUmpires(query: String): List<Umpire> {
        return MockData.umpires.filter { umpire ->
            umpire.name.contains(query, ignoreCase = true) ||
                    umpire.location.contains(query, ignoreCase = true) ||
                    umpire.certificationLevel.contains(query, ignoreCase = true)
        }
    }
}
