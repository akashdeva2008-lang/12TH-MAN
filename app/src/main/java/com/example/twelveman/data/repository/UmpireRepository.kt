package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Umpire
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UmpireRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getUmpires(): Flow<List<Umpire>> = callbackFlow {
        try {
            val listener = db.collection("umpires").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    if (snapshot != null && snapshot.isEmpty) {
                        seedUmpires()
                    }
                    trySend(MockData.umpires)
                    return@addSnapshotListener
                }

                val umpiresList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Umpire::class.java)
                }
                if (umpiresList.isNotEmpty()) {
                    MockData.umpires.clear()
                    MockData.umpires.addAll(umpiresList)
                    trySend(umpiresList)
                } else {
                    trySend(MockData.umpires)
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(MockData.umpires)
            awaitClose { }
        }
    }

    private fun seedUmpires() {
        try {
            for (umpire in MockData.umpires) {
                db.collection("umpires").document(umpire.id).set(umpire)
            }
        } catch (ignored: Exception) { }
    }

    fun getUmpireById(id: String): Umpire? {
        return MockData.umpires.find { it.id == id }
    }

    fun searchUmpires(query: String): List<Umpire> {
        return MockData.umpires.filter { umpire ->
            umpire.name.contains(query, ignoreCase = true) ||
                    umpire.location.contains(query, ignoreCase = true) ||
                    umpire.certificationLevel.contains(query, ignoreCase = true)
        }
    }
}