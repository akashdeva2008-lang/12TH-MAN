package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlayerRepository {
    fun getPlayers(): Flow<List<Player>> = flow {
        emit(MockData.players)
    }

    fun getPlayerById(id: String): Player? {
        return MockData.players.find { it.id == id }
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