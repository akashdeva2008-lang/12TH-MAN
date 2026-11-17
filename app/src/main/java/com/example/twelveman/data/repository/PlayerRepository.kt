package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class PlayerRepository {

    private val apiService = RetrofitClient.getApiService()

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
