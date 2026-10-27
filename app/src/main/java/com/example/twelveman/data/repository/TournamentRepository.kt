package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TournamentRepository {
    fun getTournaments(): Flow<List<Tournament>> = flow {
        emit(MockData.tournaments)
    }

    fun getTournamentById(id: String): Tournament? {
        return MockData.tournaments.find { it.id == id }
    }

    fun createTournament(tournament: Tournament) {
        MockData.tournaments.add(0, tournament)
    }

    fun registerTeam(team: Team) {
        val tournament = MockData.tournaments.find { it.id == team.tournamentId }
        if (tournament != null) {
            val index = MockData.tournaments.indexOf(tournament)
            MockData.tournaments[index] = tournament.copy(
                registeredTeamsCount = tournament.registeredTeamsCount + 1
            )
        }
    }

    fun getMatchesForTournament(tournamentId: String): List<Match> {
        return MockData.matches.filter { it.tournamentId == tournamentId || tournamentId.isEmpty() }
    }

    fun getMatchById(matchId: String): Match? {
        return MockData.matches.find { it.id == matchId }
    }
}