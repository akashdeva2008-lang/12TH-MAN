package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TournamentRepository {

    private val apiService = RetrofitClient.getApiService()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getTournaments(): Flow<List<Tournament>> = flow {
        // Emit cached data immediately
        emit(MockData.tournaments)

        try {
            val response = apiService.getTournaments()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteList = response.body()!!.data!!
                if (remoteList.isNotEmpty()) {
                    MockData.tournaments.clear()
                    MockData.tournaments.addAll(remoteList)
                    emit(remoteList)
                }
            }
        } catch (e: Exception) {
            // Retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun getTournamentById(id: String): Tournament? {
        return MockData.tournaments.find { it.id.equals(id, ignoreCase = true) }
    }

    fun createTournament(tournament: Tournament) {
        MockData.tournaments.add(0, tournament)
        scope.launch {
            try {
                apiService.createTournament(tournament)
            } catch (ignored: Exception) { }
        }
    }

    fun registerTeam(team: Team) {
        val tournament = MockData.tournaments.find { it.id == team.tournamentId }
        if (tournament != null) {
            val index = MockData.tournaments.indexOf(tournament)
            val updated = tournament.copy(
                registeredTeamsCount = tournament.registeredTeamsCount + 1
            )
            MockData.tournaments[index] = updated
        }
        scope.launch {
            try {
                apiService.registerTeam(team)
            } catch (ignored: Exception) { }
        }
    }

    fun getMatchesForTournament(tournamentId: String): List<Match> {
        scope.launch {
            try {
                val response = apiService.getMatches(tournamentId)
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteMatches = response.body()!!.data!!
                    if (remoteMatches.isNotEmpty()) {
                        for (remote in remoteMatches) {
                            val existingIndex = MockData.matches.indexOfFirst { it.id == remote.id }
                            if (existingIndex != -1) {
                                val current = MockData.matches[existingIndex]
                                MockData.matches[existingIndex] = current.copy(
                                    scoreA = remote.scoreA,
                                    scoreB = remote.scoreB,
                                    status = remote.status,
                                    result = remote.result
                                )
                            } else {
                                MockData.matches.add(remote)
                            }
                        }
                    }
                }
            } catch (ignored: Exception) { }
        }

        return MockData.matches.filter { it.tournamentId.equals(tournamentId, ignoreCase = true) || tournamentId.isEmpty() }
    }

    fun getMatchById(matchId: String): Match? {
        return MockData.matches.find { it.id.equals(matchId, ignoreCase = true) }
    }

    fun scheduleMatch(
        teamA: String,
        teamB: String,
        format: String,
        overs: Int,
        venue: String,
        date: String,
        time: String,
        ballType: String
    ): Match {
        val nextNum = MockData.matches.size + 1
        val matchId = "M" + (500 + nextNum)
        val newMatch = Match(
            id = matchId,
            tournamentId = "T301",
            matchNumber = nextNum,
            teamA = teamA,
            teamB = teamB,
            date = date,
            time = time,
            venue = venue,
            stage = "League Match",
            status = "UPCOMING",
            result = "Scheduled",
            scoreA = "0/0 (0.0)",
            scoreB = "0/0 (0.0)",
            totalOvers = overs,
            format = format,
            ballType = ballType,
            strikerName = "Opening Striker",
            nonStrikerName = "Opening Non-Striker",
            bowlerName = "Opening Bowler"
        )
        MockData.matches.add(0, newMatch)
        syncMatchUpdate(newMatch)
        return newMatch
    }

    fun deleteMatch(matchId: String): Boolean {
        return MockData.matches.removeAll { it.id.equals(matchId, ignoreCase = true) }
    }

    fun updateMatchDetails(matchId: String, newVenue: String, newDate: String, newTime: String): Boolean {
        val match = getMatchById(matchId) ?: return false
        val index = MockData.matches.indexOf(match)
        if (index != -1) {
            val updated = match.copy(venue = newVenue, date = newDate, time = newTime)
            MockData.matches[index] = updated
            syncMatchUpdate(updated)
            return true
        }
        return false
    }

    fun syncMatchUpdate(match: Match) {
        scope.launch {
            try {
                val payload = mapOf(
                    "id" to match.id,
                    "status" to match.status,
                    "result" to match.result,
                    "scoreA" to match.scoreA,
                    "scoreB" to match.scoreB
                )
                apiService.updateMatch(payload)
            } catch (ignored: Exception) { }
        }
    }
}
