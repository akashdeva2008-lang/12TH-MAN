package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TournamentRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getTournaments(): Flow<List<Tournament>> = callbackFlow {
        try {
            val listener = db.collection("tournaments").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    if (snapshot != null && snapshot.isEmpty) {
                        seedTournaments()
                    }
                    trySend(MockData.tournaments)
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Tournament::class.java)
                }
                if (list.isNotEmpty()) {
                    MockData.tournaments.clear()
                    MockData.tournaments.addAll(list)
                    trySend(list)
                } else {
                    trySend(MockData.tournaments)
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(MockData.tournaments)
            awaitClose { }
        }
    }

    private fun seedTournaments() {
        try {
            for (tournament in MockData.tournaments) {
                db.collection("tournaments").document(tournament.id).set(tournament)
            }
            for (match in MockData.matches) {
                db.collection("matches").document(match.id).set(match)
            }
        } catch (ignored: Exception) { }
    }

    fun getTournamentById(id: String): Tournament? {
        return MockData.tournaments.find { it.id == id }
    }

    fun createTournament(tournament: Tournament) {
        MockData.tournaments.add(0, tournament)
        try {
            db.collection("tournaments").document(tournament.id).set(tournament)
        } catch (ignored: Exception) { }
    }

    fun registerTeam(team: Team) {
        val tournament = MockData.tournaments.find { it.id == team.tournamentId }
        if (tournament != null) {
            val index = MockData.tournaments.indexOf(tournament)
            val updated = tournament.copy(
                registeredTeamsCount = tournament.registeredTeamsCount + 1
            )
            MockData.tournaments[index] = updated
            try {
                db.collection("teams").document(team.id).set(team)
                db.collection("tournaments").document(updated.id).set(updated)
            } catch (ignored: Exception) { }
        }
    }

    fun getMatchesForTournament(tournamentId: String): List<Match> {
        return MockData.matches.filter { it.tournamentId == tournamentId || tournamentId.isEmpty() }
    }

    fun getMatchById(matchId: String): Match? {
        return MockData.matches.find { it.id == matchId }
    }
}