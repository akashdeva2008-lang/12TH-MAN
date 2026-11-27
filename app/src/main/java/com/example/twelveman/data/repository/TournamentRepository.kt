package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TournamentRepository {

    private val apiService get() = RetrofitClient.getApiService()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun pollTournaments(intervalMs: Long = 10000L): Flow<List<Tournament>> = flow {
        while (true) {
            try {
                val response = apiService.getTournaments()
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteList = response.body()!!.data!!
                    MockData.tournaments.clear()
                    MockData.tournaments.addAll(remoteList)
                }
            } catch (ignored: Exception) { }
            emit(MockData.tournaments.toList())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

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

    fun pollDashboardMatchesState(intervalMs: Long = 5000L): Flow<Resource<List<Match>>> = flow {
        if (MockData.matches.isEmpty()) {
            emit(Resource.Loading())
        } else {
            emit(Resource.Success(MockData.matches.toList()))
        }

        while (true) {
            try {
                val response = apiService.getMatches("")
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteMatches = response.body()!!.data!!
                    syncRemoteMatchesIntoCache(remoteMatches)
                    emit(Resource.Success(MockData.matches.toList()))
                } else {
                    val errMsg = "Server error (${response.code()}): Unable to load matches"
                    if (MockData.matches.isEmpty()) {
                        emit(Resource.Error(errMsg))
                    } else {
                        emit(Resource.Success(MockData.matches.toList()))
                    }
                }
            } catch (e: Exception) {
                val errMsg = "Network error: Unable to reach server (${RetrofitClient.getBaseUrl()}). Check Wi-Fi or Server IP."
                if (MockData.matches.isEmpty()) {
                    emit(Resource.Error(errMsg))
                } else {
                    emit(Resource.Success(MockData.matches.toList()))
                }
            }
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private fun syncRemoteMatchesIntoCache(remoteMatches: List<Match>) {
        synchronized(MockData.matches) {
            val remoteIds = remoteMatches.map { it.id }.toSet()
            // Remove stale matches that no longer exist in DB (unless newly pending creation)
            MockData.matches.removeAll { !remoteIds.contains(it.id) && !it.id.startsWith("TEMP_") }

            for (remote in remoteMatches) {
                val existingIndex = MockData.matches.indexOfFirst { it.id == remote.id }
                if (existingIndex != -1) {
                    val current = MockData.matches[existingIndex]
                    MockData.matches[existingIndex] = current.copy(
                        teamA = remote.teamA,
                        teamB = remote.teamB,
                        date = remote.date,
                        time = remote.time,
                        venue = remote.venue,
                        format = remote.format.ifBlank { current.format },
                        stage = remote.stage.ifBlank { current.stage },
                        scoreA = remote.scoreA,
                        scoreB = remote.scoreB,
                        status = remote.status,
                        result = remote.result
                    )
                } else {
                    MockData.matches.add(remote)
                }
            }
            // Keep newest matches first (descending numeric part of ID)
            MockData.matches.sortByDescending {
                it.id.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
        }
    }

    fun pollDashboardMatches(intervalMs: Long = 8000L): Flow<List<Match>> = flow {
        while (true) {
            try {
                val response = apiService.getMatches("")
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteMatches = response.body()!!.data!!
                    syncRemoteMatchesIntoCache(remoteMatches)
                }
            } catch (ignored: Exception) { }
            emit(MockData.matches.toList())
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

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
        val tempId = "TEMP_${System.currentTimeMillis()}"
        val newMatch = Match(
            id = tempId,
            tournamentId = "T301",
            matchNumber = MockData.matches.size + 1,
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
        synchronized(MockData.matches) {
            MockData.matches.add(0, newMatch)
        }

        scope.launch {
            try {
                val payload = mapOf(
                    "tournamentId" to "T301",
                    "teamA" to teamA,
                    "teamB" to teamB,
                    "date" to date,
                    "time" to time,
                    "venue" to venue,
                    "format" to format,
                    "stage" to "League Match",
                    "status" to "UPCOMING"
                )
                val response = apiService.createMatch(payload)
                if (response.isSuccessful && response.body()?.data != null) {
                    val saved = response.body()!!.data!!
                    synchronized(MockData.matches) {
                        val idx = MockData.matches.indexOfFirst { it.id == tempId }
                        if (idx != -1) {
                            MockData.matches[idx] = MockData.matches[idx].copy(
                                id = saved.id,
                                matchNumber = saved.matchNumber
                            )
                        }
                    }
                }
            } catch (ignored: Exception) { }
        }
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

    fun pollLiveMatch(matchId: String, intervalMs: Long = 3000L): Flow<Match> = flow {
        while (true) {
            try {
                val response = apiService.getLiveMatch(matchId)
                if (response.isSuccessful && response.body()?.status == "success") {
                    val matchData = response.body()?.data
                    if (matchData != null) {
                        emit(matchData)
                    }
                }
            } catch (ignored: Exception) { }
            val localMatch = getMatchById(matchId)
            if (localMatch != null) {
                emit(localMatch)
            }
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    // ----------------------------------------------------
    // TOURNAMENT ORGANIZER REPOSITORY METHODS
    // ----------------------------------------------------
    fun getOrganizerDashboard(tournamentId: String = ""): Flow<com.example.twelveman.data.model.OrganizerDashboardData?> = flow {
        try {
            val response = apiService.getOrganizerDashboard(tournamentId)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(response.body()!!.data)
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    fun pollOrganizerDashboard(tournamentId: String = "", intervalMs: Long = 6000L): Flow<com.example.twelveman.data.model.OrganizerDashboardData?> = flow {
        while (true) {
            try {
                val response = apiService.getOrganizerDashboard(tournamentId)
                if (response.isSuccessful && response.body()?.data != null) {
                    emit(response.body()!!.data)
                }
            } catch (ignored: Exception) { }
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun getTournamentTeams(tournamentId: String = ""): Flow<List<Team>> = flow {
        // Emit cache if available
        if (MockData.teams.isNotEmpty()) {
            val cached = synchronized(MockData.teams) {
                if (tournamentId.isEmpty()) MockData.teams.toList()
                else MockData.teams.filter { it.tournamentId.equals(tournamentId, ignoreCase = true) }
            }
            emit(cached)
        }
        try {
            val response = apiService.getTournamentTeams(tournamentId)
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteTeams = response.body()!!.data!!
                synchronized(MockData.teams) {
                    if (tournamentId.isEmpty()) {
                        MockData.teams.clear()
                        MockData.teams.addAll(remoteTeams)
                    } else {
                        remoteTeams.forEach { remote ->
                            val idx = MockData.teams.indexOfFirst { it.id == remote.id }
                            if (idx != -1) MockData.teams[idx] = remote else MockData.teams.add(remote)
                        }
                    }
                    MockData.teams.sortByDescending {
                        it.id.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                    }
                }
                emit(remoteTeams)
            } else {
                emit(synchronized(MockData.teams) { MockData.teams.toList() })
            }
        } catch (e: Exception) {
            emit(synchronized(MockData.teams) { MockData.teams.toList() })
        }
    }.flowOn(Dispatchers.IO)

    fun pollAllTeams(intervalMs: Long = 6000L): Flow<List<Team>> = flow {
        while (true) {
            try {
                val response = apiService.getTournamentTeams("")
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteTeams = response.body()!!.data!!
                    synchronized(MockData.teams) {
                        MockData.teams.clear()
                        MockData.teams.addAll(remoteTeams)
                        MockData.teams.sortByDescending {
                            it.id.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                        }
                    }
                }
            } catch (ignored: Exception) { }
            emit(synchronized(MockData.teams) { MockData.teams.toList() })
            kotlinx.coroutines.delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun createCustomTeam(team: Team): com.example.twelveman.data.model.TeamOpResult {
        return try {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "create_team",
                tournamentId = team.tournamentId,
                teamId = team.id,
                teamName = team.teamName,
                shortName = team.shortName,
                captainName = team.captainName,
                captainPhone = team.captainPhone,
                captainEmail = team.captainEmail,
                viceCaptain = team.viceCaptain,
                managerName = team.managerName,
                description = team.description,
                city = team.city,
                logoUrl = team.logoUrl ?: "RED",
                status = "APPROVED",
                playerNames = team.players.map { it.playerName },
                players = team.players
            )
            val response = apiService.manageTeam(req)
            if (response.isSuccessful && response.body()?.status == "success") {
                val returnedId = (response.body()?.data?.get("id") as? String)
                    ?: ((response.body()?.data?.get("numeric_id") as? Number)?.let { "TM$it" })
                    ?: team.id
                val savedTeam = team.copy(id = returnedId)
                synchronized(MockData.teams) {
                    val existing = MockData.teams.indexOfFirst {
                        it.teamName.equals(team.teamName, ignoreCase = true) ||
                            (it.id == returnedId && returnedId.isNotBlank())
                    }
                    if (existing != -1) MockData.teams[existing] = savedTeam else MockData.teams.add(0, savedTeam)
                }
                com.example.twelveman.data.model.TeamOpResult(
                    true,
                    response.body()?.message ?: "Team '${team.teamName}' saved successfully",
                    savedTeam
                )
            } else {
                val errorMsg = extractErrorMessage(response)
                com.example.twelveman.data.model.TeamOpResult(false, errorMsg)
            }
        } catch (e: Exception) {
            val msg = if (e is java.net.ConnectException || e is java.net.SocketTimeoutException || e is java.net.UnknownHostException) {
                "Failed to save team: Server unreachable at ${RetrofitClient.getBaseUrl()}. Check Wi-Fi or Server IP."
            } else {
                "Failed to save team: ${e.localizedMessage ?: "Unknown database error"}"
            }
            com.example.twelveman.data.model.TeamOpResult(false, msg)
        }
    }

    suspend fun updateCustomTeam(team: Team): com.example.twelveman.data.model.TeamOpResult {
        return try {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "update_team",
                tournamentId = team.tournamentId,
                teamId = team.id,
                teamName = team.teamName,
                shortName = team.shortName,
                captainName = team.captainName,
                captainPhone = team.captainPhone,
                captainEmail = team.captainEmail,
                viceCaptain = team.viceCaptain,
                managerName = team.managerName,
                description = team.description,
                city = team.city,
                logoUrl = team.logoUrl ?: "RED",
                status = team.approvalStatus,
                playerNames = team.players.map { it.playerName },
                players = team.players
            )
            val response = apiService.manageTeam(req)
            if (response.isSuccessful && response.body()?.status == "success") {
                synchronized(MockData.teams) {
                    val idx = MockData.teams.indexOfFirst { it.id == team.id }
                    if (idx != -1) MockData.teams[idx] = team
                }
                com.example.twelveman.data.model.TeamOpResult(
                    true,
                    response.body()?.message ?: "Team '${team.teamName}' updated successfully",
                    team
                )
            } else {
                val errorMsg = extractErrorMessage(response)
                com.example.twelveman.data.model.TeamOpResult(false, errorMsg)
            }
        } catch (e: Exception) {
            val msg = if (e is java.net.ConnectException || e is java.net.SocketTimeoutException || e is java.net.UnknownHostException) {
                "Failed to update team: Server unreachable at ${RetrofitClient.getBaseUrl()}."
            } else {
                "Failed to update team: ${e.localizedMessage ?: "Unknown error"}"
            }
            com.example.twelveman.data.model.TeamOpResult(false, msg)
        }
    }

    private fun <T> extractErrorMessage(response: retrofit2.Response<T>): String {
        return try {
            val raw = response.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: "Server returned HTTP ${response.code()}"
            } else {
                "Server returned HTTP ${response.code()}"
            }
        } catch (e: Exception) {
            "Server error (HTTP ${response.code()})"
        }
    }

    suspend fun deleteCustomTeamWithResult(teamId: String): com.example.twelveman.data.model.TeamOpResult {
        return try {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "delete_team",
                teamId = teamId
            )
            val response = apiService.manageTeam(req)
            if (response.isSuccessful && response.body()?.status == "success") {
                synchronized(MockData.teams) {
                    MockData.teams.removeAll { it.id == teamId }
                }
                com.example.twelveman.data.model.TeamOpResult(true, response.body()?.message ?: "Team deleted successfully.")
            } else {
                com.example.twelveman.data.model.TeamOpResult(false, extractErrorMessage(response))
            }
        } catch (e: Exception) {
            com.example.twelveman.data.model.TeamOpResult(false, e.localizedMessage ?: "Failed to delete team.")
        }
    }

    suspend fun deleteCustomTeam(teamId: String): Boolean {
        return deleteCustomTeamWithResult(teamId).success
    }

    suspend fun manageTeam(request: com.example.twelveman.data.model.ManageTeamRequest): Boolean {
        return try {
            val response = apiService.manageTeam(request)
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateFixtures(request: com.example.twelveman.data.model.GenerateFixturesRequest): Boolean {
        return try {
            val response = apiService.generateFixtures(request)
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            false
        }
    }

    fun getStandings(tournamentId: String = ""): Flow<List<com.example.twelveman.data.model.TeamStanding>> = flow {
        try {
            val response = apiService.getTournamentStandings(tournamentId)
            if (response.isSuccessful && response.body()?.data != null) {
                emit(response.body()!!.data!!)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}
