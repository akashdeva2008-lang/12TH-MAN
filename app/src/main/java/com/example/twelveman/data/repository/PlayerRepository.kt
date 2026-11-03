package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Player
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PlayerRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getPlayers(): Flow<List<Player>> = callbackFlow {
        try {
            val listener = db.collection("players").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    if (snapshot != null && snapshot.isEmpty) {
                        seedPlayers()
                    }
                    trySend(MockData.players)
                    return@addSnapshotListener
                }

                val playersList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Player::class.java)
                }
                if (playersList.isNotEmpty()) {
                    MockData.players.clear()
                    MockData.players.addAll(playersList)
                    trySend(playersList)
                } else {
                    trySend(MockData.players)
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(MockData.players)
            awaitClose { }
        }
    }

    private fun seedPlayers() {
        try {
            for (player in MockData.players) {
                db.collection("players").document(player.id).set(player)
            }
        } catch (ignored: Exception) { }
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