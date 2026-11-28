package com.example.twelveman.ui.tournaments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.MatchStateSnapshot
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.data.repository.TournamentRepository
import kotlinx.coroutines.launch
import java.util.Locale

class TournamentsViewModel : ViewModel() {
    private val repository = TournamentRepository()

    private val _tournamentsList = MutableLiveData<List<Tournament>>()
    val tournamentsList: LiveData<List<Tournament>> = _tournamentsList

    private val _selectedTournament = MutableLiveData<Tournament?>()
    val selectedTournament: LiveData<Tournament?> = _selectedTournament

    private val _matchesList = MutableLiveData<List<Match>>()
    val matchesList: LiveData<List<Match>> = _matchesList

    private val _selectedMatch = MutableLiveData<Match?>()
    val selectedMatch: LiveData<Match?> = _selectedMatch

    private val _actionSuccess = MutableLiveData<Boolean>()
    val actionSuccess: LiveData<Boolean> = _actionSuccess

    // Organizer Dashboard LiveData
    private val _organizerData = MutableLiveData<com.example.twelveman.data.model.OrganizerDashboardData?>()
    val organizerData: LiveData<com.example.twelveman.data.model.OrganizerDashboardData?> = _organizerData

    private val _tournamentTeams = MutableLiveData<List<com.example.twelveman.data.model.Team>>()
    val tournamentTeams: LiveData<List<com.example.twelveman.data.model.Team>> = _tournamentTeams

    // All Created Teams for Quick Match & Ecosystem
    private val _allTeams = MutableLiveData<List<com.example.twelveman.data.model.Team>>()
    val allTeams: LiveData<List<com.example.twelveman.data.model.Team>> = _allTeams

    private val _selectedTeam1 = MutableLiveData<com.example.twelveman.data.model.Team?>()
    val selectedTeam1: LiveData<com.example.twelveman.data.model.Team?> = _selectedTeam1

    private val _selectedTeam2 = MutableLiveData<com.example.twelveman.data.model.Team?>()
    val selectedTeam2: LiveData<com.example.twelveman.data.model.Team?> = _selectedTeam2

    private val _standingsList = MutableLiveData<List<com.example.twelveman.data.model.TeamStanding>>()
    val standingsList: LiveData<List<com.example.twelveman.data.model.TeamStanding>> = _standingsList

    init {
        startRealtimeSync()
    }

    fun startRealtimeSync() {
        viewModelScope.launch {
            repository.pollTournaments(intervalMs = 8000L).collect { list ->
                _tournamentsList.value = list.distinctBy { it.id }
            }
        }
        viewModelScope.launch {
            repository.pollDashboardMatches(intervalMs = 6000L).collect { list ->
                _matchesList.value = list.distinctBy { it.id }
                val current = _selectedMatch.value
                if (current != null) {
                    val updated = list.find { it.id.equals(current.id, ignoreCase = true) }
                    if (updated != null && (updated.scoreA != current.scoreA || updated.scoreB != current.scoreB || updated.status != current.status)) {
                        _selectedMatch.value = updated
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.pollAllTeams(intervalMs = 6000L).collect { list ->
                _allTeams.value = list.distinctBy { it.id }
            }
        }
    }

    fun loadAllTeams() {
        viewModelScope.launch {
            repository.getTournamentTeams("").collect { list ->
                _allTeams.value = list.distinctBy { it.id }
            }
        }
    }

    fun selectTeam1(team: com.example.twelveman.data.model.Team) {
        _selectedTeam1.value = team
    }

    fun selectTeam2(team: com.example.twelveman.data.model.Team) {
        _selectedTeam2.value = team
    }

    fun createOrUpdateTeam(
        team: com.example.twelveman.data.model.Team,
        isEdit: Boolean,
        onComplete: (Boolean, String, com.example.twelveman.data.model.Team?) -> Unit
    ) {
        viewModelScope.launch {
            val result = if (isEdit) {
                repository.updateCustomTeam(team)
            } else {
                repository.createCustomTeam(team)
            }
            if (result.success) {
                loadAllTeams()
            }
            onComplete(result.success, result.message, result.team)
        }
    }

    fun deleteTeam(teamId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteCustomTeamWithResult(teamId)
            if (result.success) {
                loadAllTeams()
                if (_selectedTeam1.value?.id == teamId) _selectedTeam1.value = null
                if (_selectedTeam2.value?.id == teamId) _selectedTeam2.value = null
            }
            onComplete(result.success, result.message)
        }
    }

    fun loadOrganizerDashboard(tournamentId: String = "") {
        viewModelScope.launch {
            repository.pollOrganizerDashboard(tournamentId, intervalMs = 5000L).collect { data ->
                if (data != null) {
                    _organizerData.value = data
                    _tournamentTeams.value = data.teams
                    _matchesList.value = data.matches
                }
            }
        }
        loadStandings(tournamentId)
    }

    fun loadStandings(tournamentId: String = "") {
        viewModelScope.launch {
            repository.getStandings(tournamentId).collect { list ->
                _standingsList.value = list
            }
        }
    }

    fun loadTeamsForTournament(tournamentId: String = "") {
        viewModelScope.launch {
            repository.getTournamentTeams(tournamentId).collect { list ->
                _tournamentTeams.value = list
            }
        }
    }

    fun generateFixtures(
        tournamentId: String,
        format: String,
        venue: String,
        startDate: String,
        overs: Int,
        umpire: String,
        scorer: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val req = com.example.twelveman.data.model.GenerateFixturesRequest(
                tournamentId = tournamentId,
                format = format,
                venue = venue,
                startDate = startDate,
                overs = overs,
                umpire = umpire,
                scorer = scorer,
                clearExisting = true
            )
            val success = repository.generateFixtures(req)
            if (success) {
                loadOrganizerDashboard(tournamentId)
                fetchMatches(tournamentId)
            }
            onComplete(success)
        }
    }

    fun addOrganizerTeam(
        tournamentId: String,
        teamName: String,
        captainName: String,
        phone: String,
        email: String,
        viceCaptain: String = "",
        manager: String = "",
        description: String = "",
        players: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "add_team",
                tournamentId = tournamentId,
                teamName = teamName,
                captainName = captainName,
                captainPhone = phone,
                captainEmail = email,
                viceCaptain = viceCaptain,
                managerName = manager,
                description = description,
                status = "APPROVED",
                playerNames = players
            )
            val success = repository.manageTeam(req)
            if (success) {
                loadTeamsForTournament(tournamentId)
                loadOrganizerDashboard(tournamentId)
            }
            onComplete(success)
        }
    }

    fun removeTeamFromTournament(tournamentId: String, teamId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "remove_team",
                tournamentId = tournamentId,
                teamId = teamId
            )
            val success = repository.manageTeam(req)
            if (success) {
                loadTeamsForTournament(tournamentId)
                loadOrganizerDashboard(tournamentId)
            }
            onComplete(success)
        }
    }

    fun approveRejectTeam(tournamentId: String, teamId: String, status: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val req = com.example.twelveman.data.model.ManageTeamRequest(
                action = "update_status",
                tournamentId = tournamentId,
                teamId = teamId,
                status = status
            )
            val success = repository.manageTeam(req)
            if (success) {
                loadTeamsForTournament(tournamentId)
                loadOrganizerDashboard(tournamentId)
            }
            onComplete(success)
        }
    }

    fun fetchTournaments() {
        viewModelScope.launch {
            repository.getTournaments().collect { list ->
                _tournamentsList.value = list.distinctBy { it.id }
            }
        }
    }

    fun loadTournamentDetails(id: String) {
        _selectedTournament.value = repository.getTournamentById(id)
    }

    fun createTournament(
        name: String,
        format: String,
        location: String,
        startDate: String,
        endDate: String,
        totalTeams: Int,
        entryFee: Int,
        prizeMoney: Int,
        organizerName: String,
        organizerContact: String,
        description: String,
        tournamentFormat: String = "KNOCKOUT",
        groundName: String = "",
        registrationDeadline: String = "",
        firstPrize: String = "₹50,000",
        secondPrize: String = "₹25,000",
        thirdPrize: String = "₹10,000",
        rules: String = "",
        overs: Int = 20,
        isDraft: Int = 0
    ) {
        val newTournament = Tournament(
            id = "T" + (300..999).random(),
            name = name,
            format = format,
            tournamentFormat = tournamentFormat,
            location = location,
            groundName = groundName,
            startDate = startDate,
            endDate = endDate,
            registrationDeadline = registrationDeadline,
            totalTeams = totalTeams,
            registeredTeamsCount = 0,
            entryFee = entryFee,
            prizeMoney = prizeMoney,
            firstPrize = firstPrize,
            secondPrize = secondPrize,
            thirdPrize = thirdPrize,
            rules = rules,
            overs = overs,
            isDraft = isDraft,
            status = "UPCOMING",
            organizerName = organizerName,
            organizerContact = organizerContact,
            description = description
        )
        repository.createTournament(newTournament)
        _actionSuccess.value = true
        fetchTournaments()
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
        val match = repository.scheduleMatch(
            teamA = teamA,
            teamB = teamB,
            format = format,
            overs = overs,
            venue = venue,
            date = date,
            time = time,
            ballType = ballType
        )
        _selectedMatch.value = match
        fetchMatches(match.tournamentId)
        return match
    }

    fun registerTeam(tournamentId: String, teamName: String, captainName: String, phone: String, email: String, players: List<String>) {
        val newTeam = Team(
            id = "TM" + (100..999).random(),
            tournamentId = tournamentId,
            teamName = teamName,
            captainName = captainName,
            captainPhone = phone,
            captainEmail = email,
            playerNames = players,
            registrationDate = "Today"
        )
        repository.registerTeam(newTeam)
        _actionSuccess.value = true
        loadTournamentDetails(tournamentId)
    }

    fun fetchMatches(tournamentId: String) {
        _matchesList.value = repository.getMatchesForTournament(tournamentId)
    }

    private var livePollingJob: kotlinx.coroutines.Job? = null

    fun loadMatchDetails(matchId: String) {
        _selectedMatch.value = repository.getMatchById(matchId)
        startLiveMatchPolling(matchId)
        loadAllTeams()
    }

    fun getSquadForTeam(teamNameOrId: String): List<com.example.twelveman.data.model.TeamPlayerItem> {
        val allTeams = (_allTeams.value ?: emptyList()) +
                       (_tournamentTeams.value ?: emptyList()) +
                       com.example.twelveman.data.local.MockData.teams
        val found = allTeams.firstOrNull {
            it.teamName.equals(teamNameOrId, ignoreCase = true) ||
            it.id.equals(teamNameOrId, ignoreCase = true) ||
            it.shortName.equals(teamNameOrId, ignoreCase = true)
        } ?: return emptyList()

        if (found.players.isNotEmpty()) {
            return found.players
        }
        if (found.playerNames.isNotEmpty()) {
            return found.playerNames.mapIndexed { i, name ->
                com.example.twelveman.data.model.TeamPlayerItem(
                    id = "P_${found.id}_$i",
                    playerName = name,
                    role = if (i == 0) "Batsman" else if (i == 1) "All-Rounder" else "Bowler",
                    jerseyNumber = "${i + 1}",
                    isCaptain = name.equals(found.captainName, ignoreCase = true)
                )
            }
        }
        if (found.captainName.isNotBlank()) {
            return listOf(
                com.example.twelveman.data.model.TeamPlayerItem(
                    id = "P_${found.id}_cap",
                    playerName = found.captainName,
                    role = "All-Rounder",
                    jerseyNumber = "7",
                    isCaptain = true
                )
            )
        }
        return emptyList()
    }

    fun addPlayerToTeamSquad(
        teamNameOrId: String,
        newPlayer: com.example.twelveman.data.model.TeamPlayerItem,
        onComplete: (Boolean, String, com.example.twelveman.data.model.TeamPlayerItem) -> Unit
    ) {
        viewModelScope.launch {
            val allTeams = (_allTeams.value ?: emptyList()) +
                           (_tournamentTeams.value ?: emptyList()) +
                           com.example.twelveman.data.local.MockData.teams
            val team = allTeams.firstOrNull {
                it.teamName.equals(teamNameOrId, ignoreCase = true) ||
                it.id.equals(teamNameOrId, ignoreCase = true)
            }
            if (team == null) {
                onComplete(false, "Team '$teamNameOrId' not found", newPlayer)
                return@launch
            }

            if (team.players.any { it.playerName.equals(newPlayer.playerName, ignoreCase = true) }) {
                onComplete(false, "Player '${newPlayer.playerName}' is already registered in this squad", newPlayer)
                return@launch
            }

            val updatedSquad = team.players.toMutableList()
            updatedSquad.add(newPlayer)
            val updatedTeam = team.copy(
                players = updatedSquad,
                playerNames = updatedSquad.map { it.playerName },
                playerCount = updatedSquad.size
            )

            val result = repository.updateCustomTeam(updatedTeam)
            loadAllTeams()
            onComplete(true, "Player added to squad successfully", newPlayer)
        }
    }

    fun getEligibleIncomingBatters(battingTeamName: String, match: com.example.twelveman.data.model.Match): List<com.example.twelveman.data.model.TeamPlayerItem> {
        val squad = getSquadForTeam(battingTeamName)
        val inn = match.activeInnings
        val dismissedBatters = inn.battingList.filter { it.isOut }.map { it.name.trim().lowercase(java.util.Locale.ROOT) }
        val currentCrease = listOf(
            match.strikerName.trim().lowercase(java.util.Locale.ROOT),
            match.nonStrikerName.trim().lowercase(java.util.Locale.ROOT)
        )

        return squad.filter { p ->
            val pName = p.playerName.trim().lowercase(java.util.Locale.ROOT)
            pName !in dismissedBatters && pName !in currentCrease
        }
    }

    fun startLiveMatchPolling(matchId: String) {
        livePollingJob?.cancel()
        livePollingJob = viewModelScope.launch {
            repository.    pollLiveMatch(  matchId, intervalMs = 3000L).collect { liveMatch ->
                _selectedMatch.value = liveMatch
            }
        }
    }

    fun recordTossAndOpeners(
        matchId: String,
        tossWinner: String,
        tossDecision: String,
        battingTeam: String,
        bowlingTeam: String,
        striker: String,
        nonStriker: String,
        bowler: String,
        totalOvers: Int = 20
    ) {
        val match = repository.getMatchById(matchId) ?: return
        if (totalOvers > 0) {
            match.totalOvers = totalOvers
        }
        match.tossWinner = tossWinner
        match.tossDecision = tossDecision
        match.battingTeam = battingTeam
        match.bowlingTeam = bowlingTeam
        match.strikerName = striker
        match.strikerRuns = 0
        match.strikerBalls = 0
        match.nonStrikerName = nonStriker
        match.nonStrikerRuns = 0
        match.nonStrikerBalls = 0
        match.bowlerName = bowler
        match.previousBowlerName = ""
        match.bowlerOvers = 0.0
        match.bowlerRunsConceded = 0
        match.bowlerWickets = 0
        match.status = "LIVE"
        match.partnershipRuns = 0
        match.partnershipBalls = 0

        // Initialize active innings scorecard
        val inn = match.activeInnings
        inn.battingList.clear()
        inn.bowlingList.clear()
        inn.fallOfWickets.clear()
        inn.extras = com.example.twelveman.data.model.ExtrasBreakdown()
        inn.currentOverDeliveries.clear()

        val strikerScore = com.example.twelveman.data.model.BatterScore(name = striker, isStriker = true)
        val nonStrikerScore = com.example.twelveman.data.model.BatterScore(name = nonStriker, isStriker = false)
        inn.battingList.add(strikerScore)
        inn.battingList.add(nonStrikerScore)

        val bowlerScore = com.example.twelveman.data.model.BowlerScore(name = bowler)
        inn.bowlingList.add(bowlerScore)

        val commentaryText = "Toss Result: $tossWinner won the toss & elected to $tossDecision first. Match is LIVE! Openers: $striker & $nonStriker. Bowler: $bowler."
        match.commentary.add(0, commentaryText)

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    fun updateMatchDetails(matchId: String, venue: String, date: String, time: String) {
        val success = repository.updateMatchDetails(matchId, venue, date, time)
        if (success) {
            _selectedMatch.value = repository.getMatchById(matchId)
            _actionSuccess.value = true
        }
    }

    fun deleteMatch(matchId: String): Boolean {
        val success = repository.deleteMatch(matchId)
        if (success) {
            _selectedMatch.value = null
            _actionSuccess.value = true
        }
        return success
    }

    fun recordBall(
        matchId: String,
        runs: Int,
        isWicket: Boolean = false,
        extraType: String = "",
        wicketType: String = "",
        dismissedPlayer: String = "",
        incomingBatsman: String = "",
        fielderName: String = "",
        penaltyRuns: Int = 0
    ) {
        val match = repository.getMatchById(matchId) ?: return
        if (match.status.equals("COMPLETED", ignoreCase = true)) return
        if (match.currentInnings == 1 && (match.isFirstInningsCompleted || match.legalBallsA >= match.maxLegalBalls || match.wicketsA >= 10)) return
        if (match.currentInnings == 2 && (match.legalBallsB >= match.maxLegalBalls || match.wicketsB >= 10 || (match.target > 0 && match.runsB >= match.target))) return

        // 1. Save deep snapshot for accurate Undo
        match.historyStack.add(
            MatchStateSnapshot(
                runsA = match.runsA,
                wicketsA = match.wicketsA,
                legalBallsA = match.legalBallsA,
                oversA = match.oversA,
                runsB = match.runsB,
                wicketsB = match.wicketsB,
                legalBallsB = match.legalBallsB,
                oversB = match.oversB,
                currentInnings = match.currentInnings,
                scoreA = match.scoreA,
                scoreB = match.scoreB,
                strikerName = match.strikerName,
                strikerRuns = match.strikerRuns,
                strikerBalls = match.strikerBalls,
                nonStrikerName = match.nonStrikerName,
                nonStrikerRuns = match.nonStrikerRuns,
                nonStrikerBalls = match.nonStrikerBalls,
                bowlerName = match.bowlerName,
                bowlerOvers = match.bowlerOvers,
                bowlerRunsConceded = match.bowlerRunsConceded,
                bowlerWickets = match.bowlerWickets,
                previousBowlerName = match.previousBowlerName,
                partnershipRuns = match.partnershipRuns,
                partnershipBalls = match.partnershipBalls,
                extrasWide = match.extrasWide,
                extrasNoBall = match.extrasNoBall,
                extrasBye = match.extrasBye,
                extrasLegBye = match.extrasLegBye,
                extrasPenalty = match.extrasPenalty,
                target = match.target,
                innings1Snapshot = match.innings1.deepCopy(),
                innings2Snapshot = match.innings2.deepCopy()
            )
        )

        match.status = "LIVE"
        val isWide = extraType.contains("Wide", ignoreCase = true)
        val isNoBall = extraType.contains("No Ball", ignoreCase = true)
        val isBye = extraType.contains("Bye", ignoreCase = true) && !extraType.contains("Leg Bye", ignoreCase = true)
        val isLegBye = extraType.contains("Leg Bye", ignoreCase = true)
        val isPenalty = extraType.contains("Penalty", ignoreCase = true) || penaltyRuns > 0
        val isLegalDelivery = !isWide && !isNoBall && !isPenalty

        val inn = match.activeInnings
        val strikerObj = match.getStrikerScore()
        val bowlerObj = match.getCurrentBowlerScore()

        var teamRunsToAdd = runs
        var deliveryTag = runs.toString()

        if (isWide) {
            val wideTotal = runs + 1
            teamRunsToAdd = wideTotal
            match.extrasWide += wideTotal
            inn.extras.wides += wideTotal
            bowlerObj.wides += 1
            bowlerObj.runsConceded += wideTotal
            deliveryTag = if (runs == 0) "Wd" else "${runs + 1}Wd"
        } else if (isNoBall) {
            val nbTotal = runs + 1
            teamRunsToAdd = nbTotal
            match.extrasNoBall += 1
            inn.extras.noBalls += 1
            bowlerObj.noBalls += 1
            bowlerObj.runsConceded += 1 // 1 extra run for NB
            if (runs > 0) {
                strikerObj.runs += runs
                bowlerObj.runsConceded += runs
                if (runs == 4) strikerObj.fours += 1
                if (runs == 6) strikerObj.sixes += 1
            }
            strikerObj.balls += 1
            deliveryTag = if (runs == 0) "Nb" else "${runs}Nb"
        } else if (isBye) {
            teamRunsToAdd = runs
            match.extrasBye += runs
            inn.extras.byes += runs
            strikerObj.balls += 1
            bowlerObj.legalBalls += 1
            deliveryTag = "B$runs"
        } else if (isLegBye) {
            teamRunsToAdd = runs
            match.extrasLegBye += runs
            inn.extras.legByes += runs
            strikerObj.balls += 1
            bowlerObj.legalBalls += 1
            deliveryTag = "LB$runs"
        } else if (isPenalty) {
            val pRuns = if (penaltyRuns > 0) penaltyRuns else 5
            teamRunsToAdd = pRuns
            match.extrasPenalty += pRuns
            inn.extras.penalty += pRuns
            deliveryTag = "P$pRuns"
        } else {
            // Normal delivery off the bat
            strikerObj.runs += runs
            strikerObj.balls += 1
            if (runs == 4) strikerObj.fours += 1
            if (runs == 6) strikerObj.sixes += 1

            bowlerObj.runsConceded += runs
            bowlerObj.legalBalls += 1
        }

        // Apply team runs & legal ball counting
        if (match.currentInnings == 1) {
            match.runsA += teamRunsToAdd
            if (isLegalDelivery) match.legalBallsA += 1
            match.oversA = "${match.legalBallsA / 6}.${match.legalBallsA % 6}".toDoubleOrNull() ?: 0.0
            match.scoreA = "${match.runsA}/${match.wicketsA} (${match.oversADisplay} ov)"
        } else {
            match.runsB += teamRunsToAdd
            if (isLegalDelivery) match.legalBallsB += 1
            match.oversB = "${match.legalBallsB / 6}.${match.legalBallsB % 6}".toDoubleOrNull() ?: 0.0
            match.scoreB = "${match.runsB}/${match.wicketsB} (${match.oversBDisplay} ov)"
        }

        // Sync local match properties for legacy readers
        match.strikerRuns = strikerObj.runs
        match.strikerBalls = strikerObj.balls
        match.bowlerRunsConceded = bowlerObj.runsConceded
        match.bowlerWickets = bowlerObj.wickets
        match.bowlerOvers = "${bowlerObj.legalBalls / 6}.${bowlerObj.legalBalls % 6}".toDoubleOrNull() ?: 0.0

        // Partnership
        match.partnershipRuns += teamRunsToAdd
        if (isLegalDelivery) match.partnershipBalls += 1

        // Wicket Processing
        if (isWicket) {
            val wType = if (wicketType.isNotBlank()) wicketType else "Bowled"
            val outBatterName = if (dismissedPlayer.isNotBlank()) dismissedPlayer else match.strikerName
            val isStrikerOut = outBatterName.equals(match.strikerName, ignoreCase = true)
            val outObj = if (isStrikerOut) strikerObj else match.getNonStrikerScore()

            outObj.isOut = true
            outObj.isStriker = false
            outObj.dismissalType = when (wType) {
                "Bowled" -> "b ${match.bowlerName}"
                "Caught" -> if (fielderName.isNotBlank()) "c $fielderName b ${match.bowlerName}" else "c & b ${match.bowlerName}"
                "Caught and Bowled" -> "c & b ${match.bowlerName}"
                "LBW" -> "lbw b ${match.bowlerName}"
                "Stumped" -> if (fielderName.isNotBlank()) "st $fielderName b ${match.bowlerName}" else "st b ${match.bowlerName}"
                "Hit Wicket" -> "hit wicket b ${match.bowlerName}"
                "Run Out" -> if (fielderName.isNotBlank()) "run out ($fielderName)" else "run out"
                "Retired Out" -> "retired out"
                "Retired Hurt" -> "retired hurt"
                else -> wType
            }
            outObj.bowlerName = match.bowlerName
            outObj.fielderName = fielderName

            val creditsBowler = wType != "Run Out" && wType != "Retired Out" && wType != "Retired Hurt"
            if (creditsBowler) {
                bowlerObj.wickets += 1
                match.bowlerWickets = bowlerObj.wickets
            }

            if (match.currentInnings == 1) {
                match.wicketsA += 1
                match.scoreA = "${match.runsA}/${match.wicketsA} (${match.oversADisplay} ov)"
            } else {
                match.wicketsB += 1
                match.scoreB = "${match.runsB}/${match.wicketsB} (${match.oversBDisplay} ov)"
            }

            val currentOversStr = match.activeOversDisplay
            inn.fallOfWickets.add(
                com.example.twelveman.data.model.FallOfWicket(
                    wicketNumber = match.activeWickets,
                    runs = match.activeRuns,
                    overs = currentOversStr,
                    batsmanName = outBatterName
                )
            )

            // Incoming batsman
            val newBatterName = if (incomingBatsman.isNotBlank()) incomingBatsman else "${match.battingTeam} Batter ${match.activeWickets + 2}"
            val newBatterScore = com.example.twelveman.data.model.BatterScore(name = newBatterName, isStriker = isStrikerOut)
            inn.battingList.add(newBatterScore)

            if (isStrikerOut) {
                match.strikerName = newBatterName
                match.strikerRuns = 0
                match.strikerBalls = 0
            } else {
                match.nonStrikerName = newBatterName
                match.nonStrikerRuns = 0
                match.nonStrikerBalls = 0
            }

            // Reset partnership for new wicket
            match.partnershipRuns = 0
            match.partnershipBalls = 0

            deliveryTag = if (deliveryTag == "0") "W" else "$deliveryTag+W"
        }

        // Add to current over deliveries
        inn.currentOverDeliveries.add(deliveryTag)

        // Generate descriptive commentary
        val overBall = "${match.activeLegalBalls / 6}.${(match.activeLegalBalls % 6).let { if (it == 0 && isLegalDelivery) 6 else it }}"
        val commentaryText = buildString {
            append("$overBall: ${match.bowlerName} to ${strikerObj.name}, ")
            if (isWicket) {
                append("OUT! ${strikerObj.name} is dismissed (${strikerObj.dismissalType}). ")
            } else if (runs == 6) {
                append("SIX! Massive hit sailing over the boundary ropes! ")
            } else if (runs == 4) {
                append("FOUR! Pierces the gap with immaculate timing! ")
            } else if (runs > 0 && isLegalDelivery) {
                append("$runs run${if (runs > 1) "s" else ""} taken. ")
            } else if (isWide) {
                append("Wide delivery signalled by umpire. ")
            } else if (isNoBall) {
                append("No ball! Overstepped line. Free Hit next ball! ")
            } else if (isBye || isLegBye) {
                append("$extraType! $runs run${if (runs > 1) "s" else ""}. ")
            } else {
                append("no run, defensive push into the field. ")
            }
            append("Score: ${if (match.currentInnings == 1) match.scoreA else match.scoreB}")
        }
        match.commentary.add(0, commentaryText)

        // Strike rotation on odd runs (if legal delivery or runs ran off extras)
        if (!isWide && runs % 2 != 0) {
            swapStrike(match)
        }

        // Over completion check
        val activeLegalBalls = match.activeLegalBalls
        val isOverCompleted = isLegalDelivery && (activeLegalBalls > 0) && (activeLegalBalls % 6 == 0)
        if (isOverCompleted) {
            match.previousBowlerName = match.bowlerName
            swapStrike(match)
            inn.currentOverDeliveries.clear()
            match.commentary.add(0, "End of Over ${activeLegalBalls / 6}. Strike rotated to ${match.strikerName}.")
        }

        // Reusable Innings Completion Check (Dynamic Overs Limit, All Out, or Target Reached)
        evaluateInningsCompletion(match)

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    private fun evaluateInningsCompletion(match: com.example.twelveman.data.model.Match) {
        if (match.currentInnings == 1) {
            if (match.legalBallsA >= match.maxLegalBalls || match.wicketsA >= 10) {
                match.isFirstInningsCompleted = true
                match.target = match.runsA + 1
                match.status = "LIVE"
                val firstTeam = match.battingTeam.ifBlank { match.teamA }
                match.commentary.add(
                    0,
                    "FIRST INNINGS COMPLETED: $firstTeam scored ${match.runsA}/${match.wicketsA} in ${match.oversADisplay} overs. Target: ${match.target}."
                )
            }
        } else if (match.currentInnings == 2) {
            val chasingTeam = match.battingTeam.ifBlank { match.teamB }
            val defendingTeam = match.bowlingTeam.ifBlank { match.teamA }
            if (match.target > 0 && match.runsB >= match.target) {
                val wicketsRemaining = (10 - match.wicketsB).coerceAtLeast(1)
                match.status = "COMPLETED"
                match.result = "$chasingTeam won by $wicketsRemaining wicket${if (wicketsRemaining > 1) "s" else ""}"
                match.commentary.add(0, "MATCH COMPLETED: ${match.result}")
            } else if (match.legalBallsB >= match.maxLegalBalls || match.wicketsB >= 10) {
                match.status = "COMPLETED"
                match.result = when {
                    match.runsA > match.runsB -> {
                        val margin = match.runsA - match.runsB
                        "$defendingTeam won by $margin run${if (margin > 1) "s" else ""}"
                    }
                    match.runsB > match.runsA -> {
                        val wicketsRemaining = (10 - match.wicketsB).coerceAtLeast(1)
                        "$chasingTeam won by $wicketsRemaining wicket${if (wicketsRemaining > 1) "s" else ""}"
                    }
                    else -> "Match Tied"
                }
                match.commentary.add(0, "MATCH COMPLETED: ${match.result}")
            }
        }
    }


    fun updateMatchOvers(matchId: String, newOvers: Int) {
        val match = repository.getMatchById(matchId) ?: return
        if (newOvers > 0) {
            match.totalOvers = newOvers
            match.commentary.add(0, "Match overs updated to $newOvers overs per innings.")
            _selectedMatch.value = match
            repository.syncMatchUpdate(match)
            loadOrganizerDashboard(match.tournamentId)
            loadStandings(match.tournamentId)
        }
    }


    fun swapStrike(match: Match) {
        val tempName = match.strikerName
        val tempRuns = match.strikerRuns
        val tempBalls = match.strikerBalls

        match.strikerName = match.nonStrikerName
        match.strikerRuns = match.nonStrikerRuns
        match.strikerBalls = match.nonStrikerBalls

        match.nonStrikerName = tempName
        match.nonStrikerRuns = tempRuns
        match.nonStrikerBalls = tempBalls

        // Update isStriker flag on active scorecard
        val inn = match.activeInnings
        inn.battingList.forEach {
            it.isStriker = it.name.equals(match.strikerName, ignoreCase = true)
        }
    }

    fun rotateStrike(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        swapStrike(match)
        match.commentary.add(0, "Manual strike change: ${match.strikerName} is now on strike.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    fun changeBowler(matchId: String, newBowlerName: String): Boolean {
        val match = repository.getMatchById(matchId) ?: return false
        if (newBowlerName.equals(match.previousBowlerName, ignoreCase = true) && match.activeLegalBalls % 6 == 0) {
            // Consecutive overs by the same bowler prohibited in standard cricket
            return false
        }
        match.bowlerName = newBowlerName
        val bowlerObj = match.getCurrentBowlerScore()
        match.bowlerRunsConceded = bowlerObj.runsConceded
        match.bowlerWickets = bowlerObj.wickets
        match.bowlerOvers = "${bowlerObj.legalBalls / 6}.${bowlerObj.legalBalls % 6}".toDoubleOrNull() ?: 0.0

        match.commentary.add(0, "New Bowler: $newBowlerName comes into the attack.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
        return true
    }

    fun changeStriker(matchId: String, newStrikerName: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.strikerName = newStrikerName
        val sObj = match.getStrikerScore()
        match.strikerRuns = sObj.runs
        match.strikerBalls = sObj.balls
        match.commentary.add(0, "Striker changed to $newStrikerName.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun changeNonStriker(matchId: String, newNonStrikerName: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.nonStrikerName = newNonStrikerName
        val nsObj = match.getNonStrikerScore()
        match.nonStrikerRuns = nsObj.runs
        match.nonStrikerBalls = nsObj.balls
        match.commentary.add(0, "Non-Striker changed to $newNonStrikerName.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun retireBatsman(matchId: String, newBatsmanName: String) {
        val match = repository.getMatchById(matchId) ?: return
        val retiredName = match.strikerName
        val retiredRuns = match.strikerRuns
        val retiredBalls = match.strikerBalls

        val strikerObj = match.getStrikerScore()
        strikerObj.isOut = true
        strikerObj.isStriker = false
        strikerObj.dismissalType = "retired out"

        val newBatter = com.example.twelveman.data.model.BatterScore(name = newBatsmanName, isStriker = true)
        match.activeInnings.battingList.add(newBatter)

        match.commentary.add(0, "Retired: $retiredName retired on $retiredRuns ($retiredBalls balls). New batsman $newBatsmanName takes strike.")
        match.strikerName = newBatsmanName
        match.strikerRuns = 0
        match.strikerBalls = 0

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    fun endOverManually(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.previousBowlerName = match.bowlerName
        swapStrike(match)
        match.activeInnings.currentOverDeliveries.clear()
        match.commentary.add(0, "Over concluded manually. Strike rotated to ${match.strikerName}.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    fun undoLastBall(matchId: String): Boolean {
        val match = repository.getMatchById(matchId) ?: return false
        if (match.historyStack.isNotEmpty()) {
            val lastState = match.historyStack.removeAt(match.historyStack.size - 1)
            match.runsA = lastState.runsA
            match.wicketsA = lastState.wicketsA
            match.legalBallsA = lastState.legalBallsA
            match.oversA = lastState.oversA
            match.runsB = lastState.runsB
            match.wicketsB = lastState.wicketsB
            match.legalBallsB = lastState.legalBallsB
            match.oversB = lastState.oversB
            match.currentInnings = lastState.currentInnings
            match.scoreA = lastState.scoreA
            match.scoreB = lastState.scoreB

            match.strikerName = lastState.strikerName
            match.strikerRuns = lastState.strikerRuns
            match.strikerBalls = lastState.strikerBalls
            match.nonStrikerName = lastState.nonStrikerName
            match.nonStrikerRuns = lastState.nonStrikerRuns
            match.nonStrikerBalls = lastState.nonStrikerBalls

            match.bowlerName = lastState.bowlerName
            match.previousBowlerName = lastState.previousBowlerName
            match.bowlerOvers = lastState.bowlerOvers
            match.bowlerRunsConceded = lastState.bowlerRunsConceded
            match.bowlerWickets = lastState.bowlerWickets

            match.partnershipRuns = lastState.partnershipRuns
            match.partnershipBalls = lastState.partnershipBalls
            match.extrasWide = lastState.extrasWide
            match.extrasNoBall = lastState.extrasNoBall
            match.extrasBye = lastState.extrasBye
            match.extrasLegBye = lastState.extrasLegBye
            match.extrasPenalty = lastState.extrasPenalty
            match.target = lastState.target

            lastState.innings1Snapshot?.let { match.innings1 = it.deepCopy() }
            lastState.innings2Snapshot?.let { match.innings2 = it.deepCopy() }

            if (match.commentary.isNotEmpty()) {
                match.commentary.removeAt(0)
            }
            match.commentary.add(0, "Undo: Restored previous delivery state.")
            _selectedMatch.value = match
            repository.syncMatchUpdate(match)
            return true
        }
        return false
    }

    fun finishFirstInningsManually(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        if (match.currentInnings != 1) return
        match.isFirstInningsCompleted = true
        match.target = match.runsA + 1
        match.status = "LIVE"
        val firstTeam = match.battingTeam.ifBlank { match.teamA }
        match.commentary.add(0, "FIRST INNINGS COMPLETED: $firstTeam finished at ${match.runsA}/${match.wicketsA}. Target: ${match.target}.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun startSecondInningsWithPlayers(
        matchId: String,
        strikerName: String,
        nonStrikerName: String,
        bowlerName: String
    ) {
        val match = repository.getMatchById(matchId) ?: return
        // Prevent duplicate second innings creation
        if (match.currentInnings == 2) return

        match.isFirstInningsCompleted = true
        match.currentInnings = 2
        match.status = "LIVE"
        match.target = match.runsA + 1

        // Reset second innings counters cleanly
        match.runsB = 0
        match.wicketsB = 0
        match.legalBallsB = 0
        match.oversB = 0.0
        match.scoreB = "0/0 (0.0)"
        match.partnershipRuns = 0
        match.partnershipBalls = 0

        // Swap batting & bowling teams reliably
        val firstBatting = match.battingTeam.ifBlank { match.teamA }
        val firstBowling = match.bowlingTeam.ifBlank {
            if (firstBatting.equals(match.teamA, ignoreCase = true)) match.teamB else match.teamA
        }
        match.battingTeam = firstBowling
        match.bowlingTeam = firstBatting

        match.strikerName = strikerName.ifBlank { "${match.battingTeam} Opener 1" }
        match.strikerRuns = 0
        match.strikerBalls = 0
        match.nonStrikerName = nonStrikerName.ifBlank { "${match.battingTeam} Opener 2" }
        match.nonStrikerRuns = 0
        match.nonStrikerBalls = 0
        match.bowlerName = bowlerName.ifBlank { "${match.bowlingTeam} Bowler 1" }
        match.previousBowlerName = ""
        match.bowlerOvers = 0.0
        match.bowlerRunsConceded = 0
        match.bowlerWickets = 0

        val inn = match.innings2
        inn.battingList.clear()
        inn.bowlingList.clear()
        inn.fallOfWickets.clear()
        inn.extras = com.example.twelveman.data.model.ExtrasBreakdown()
        inn.currentOverDeliveries.clear()

        inn.battingList.add(com.example.twelveman.data.model.BatterScore(name = match.strikerName, isStriker = true))
        inn.battingList.add(com.example.twelveman.data.model.BatterScore(name = match.nonStrikerName, isStriker = false))
        inn.bowlingList.add(com.example.twelveman.data.model.BowlerScore(name = match.bowlerName))

        match.commentary.add(0, "Innings 2 Started! ${match.battingTeam} need ${match.target} runs in ${match.totalOvers} overs.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }

    fun switchInnings(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        if (match.currentInnings == 2) return
        val firstBatting = match.battingTeam.ifBlank { match.teamA }
        val firstBowling = match.bowlingTeam.ifBlank {
            if (firstBatting.equals(match.teamA, ignoreCase = true)) match.teamB else match.teamA
        }
        startSecondInningsWithPlayers(
            matchId = matchId,
            strikerName = "$firstBowling Opener 1",
            nonStrikerName = "$firstBowling Opener 2",
            bowlerName = "$firstBatting Bowler 1"
        )
    }

    fun completeMatch(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.status = "COMPLETED"
        match.result = if (match.runsA > match.runsB) "${match.teamA} won by ${match.runsA - match.runsB} runs"
        else if (match.runsB > match.runsA) "${match.teamB} won by ${match.wicketsB.let { 10 - it }} wickets"
        else "Match Tied"
        match.commentary.add(0, "Match Concluded: ${match.result}")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
        loadOrganizerDashboard(match.tournamentId)
        loadStandings(match.tournamentId)
    }
}
