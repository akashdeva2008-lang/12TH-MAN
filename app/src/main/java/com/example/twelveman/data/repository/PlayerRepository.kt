package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.api.models.AuthResponse
import com.example.twelveman.data.api.models.PlayerRegistrationRequest
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Player
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlayerRepository {

    private val apiService get() = RetrofitClient.getApiService()

    suspend fun registerPlayer(request: PlayerRegistrationRequest): Result<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.registerPlayer(request)
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

    suspend fun fetchMyPlayerProfile(userId: Int = 0): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRoleProfile(userId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Unable to load player profile (HTTP ${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMyPlayerProfile(payload: Map<String, Any>): Result<String> = withContext(Dispatchers.IO) {
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

    fun pollPlayers(intervalMs: Long = 8000L): Flow<List<Player>> = flow {
        while (true) {
            try {
                val response = apiService.getPlayers()
                if (response.isSuccessful && response.body()?.data != null) {
                    val remotePlayers = response.body()!!.data!!
                    MockData.players.clear()
                    MockData.players.addAll(remotePlayers)
                }
            } catch (ignored: Exception) { }
            emit(MockData.players.toList())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun getPlayers(): Flow<List<Player>> = flow {
        // Emit cached data immediately for zero latency
        emit(MockData.players)

        try {
            val response = apiService.getPlayers()
            if (response.isSuccessful && response.body()?.data != null) {
                val remotePlayers = response.body()!!.data!!
                if (remotePlayers.isNotEmpty()) {
                    MockData.players.clear()
                    MockData.players.addAll(remotePlayers)
                    emit(remotePlayers)
                }
            }
        } catch (e: Exception) {
            // Gracefully retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun getPlayerById(id: String): Player? {
        return MockData.players.find { it.id.equals(id, ignoreCase = true) }
    }

    fun searchPlayers(query: String, roleFilter: String): List<Player> {
        return MockData.players.filter { player ->
            val matchesQuery = player.name.contains(query, ignoreCase = true) ||
                    player.location.contains(query, ignoreCase = true)
            val matchesRole = roleFilter == "All" || player.role.equals(roleFilter, ignoreCase = true)
            matchesQuery && matchesRole
        }
    }
}
