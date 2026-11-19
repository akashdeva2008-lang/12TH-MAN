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

    init {
        fetchTournaments()
    }

    fun fetchTournaments() {
        viewModelScope.launch {
            repository.getTournaments().collect { list ->
                _tournamentsList.value = list
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
        description: String
    ) {
        val newTournament = Tournament(
            id = "T" + (300..999).random(),
            name = name,
            format = format,
            location = location,
            startDate = startDate,
            endDate = endDate,
            totalTeams = totalTeams,
            registeredTeamsCount = 1,
            entryFee = entryFee,
            prizeMoney = prizeMoney,
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

    fun loadMatchDetails(matchId: String) {
        _selectedMatch.value = repository.getMatchById(matchId)
    }

    fun recordTossAndOpeners(
        matchId: String,
        tossWinner: String,
        tossDecision: String,
        battingTeam: String,
        bowlingTeam: String,
        striker: String,
        nonStriker: String,
        bowler: String
    ) {
        val match = repository.getMatchById(matchId) ?: return
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
        match.bowlerOvers = 0.0
        match.bowlerRunsConceded = 0
        match.bowlerWickets = 0
        match.status = "LIVE"
        match.partnershipRuns = 0
        match.partnershipBalls = 0

        val commentaryText = "Toss Result: $tossWinner won the toss & elected to $tossDecision first. Match is LIVE! Openers: $striker & $nonStriker. Bowler: $bowler."
        match.commentary.add(0, commentaryText)

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
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

    fun recordBall(matchId: String, runs: Int, isWicket: Boolean, extraType: String) {
        val match = repository.getMatchById(matchId) ?: return

        // 1. Save detailed snapshot for Undo
        match.historyStack.add(
            MatchStateSnapshot(
                runsA = match.runsA,
                wicketsA = match.wicketsA,
                oversA = match.oversA,
                runsB = match.runsB,
                wicketsB = match.wicketsB,
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
                partnershipRuns = match.partnershipRuns,
                partnershipBalls = match.partnershipBalls,
                extrasWide = match.extrasWide,
                extrasNoBall = match.extrasNoBall,
                extrasBye = match.extrasBye,
                extrasLegBye = match.extrasLegBye
            )
        )

        match.status = "LIVE"
        val isWide = extraType.contains("Wide", ignoreCase = true)
        val isNoBall = extraType.contains("No Ball", ignoreCase = true)
        val isBye = extraType.contains("Bye", ignoreCase = true) && !extraType.contains("Leg Bye", ignoreCase = true)
        val isLegBye = extraType.contains("Leg Bye", ignoreCase = true)
        val isLegalDelivery = !isWide && !isNoBall

        var overCompleted = false

        // Update total team score
        val runsToAdd = if (isWide || isNoBall) runs + 1 else runs
        if (match.currentInnings == 1) {
            match.runsA += runsToAdd
            if (isWicket) match.wicketsA += 1
            if (isLegalDelivery) {
                val currentOversInt = match.oversA.toInt()
                val currentBallsInt = Math.round((match.oversA - currentOversInt) * 10).toInt()
                val newBalls = currentBallsInt + 1
                if (newBalls >= 6) {
                    match.oversA = (currentOversInt + 1).toDouble()
                    overCompleted = true
                } else {
                    match.oversA = currentOversInt + (newBalls / 10.0)
                }
            }
            match.scoreA = "${match.runsA}/${match.wicketsA} (${String.format(Locale.US, "%.1f", match.oversA)} ov)"
        } else {
            match.runsB += runsToAdd
            if (isWicket) match.wicketsB += 1
            if (isLegalDelivery) {
                val currentOversInt = match.oversB.toInt()
                val currentBallsInt = Math.round((match.oversB - currentOversInt) * 10).toInt()
                val newBalls = currentBallsInt + 1
                if (newBalls >= 6) {
                    match.oversB = (currentOversInt + 1).toDouble()
                    overCompleted = true
                } else {
                    match.oversB = currentOversInt + (newBalls / 10.0)
                }
            }
            match.scoreB = "${match.runsB}/${match.wicketsB} (${String.format(Locale.US, "%.1f", match.oversB)} ov)"
        }

        // Extras statistics
        if (isWide) match.extrasWide += 1
        if (isNoBall) match.extrasNoBall += 1
        if (isBye) match.extrasBye += runs
        if (isLegBye) match.extrasLegBye += runs

        // Partnership updates
        match.partnershipRuns += runsToAdd
        if (isLegalDelivery) match.partnershipBalls += 1

        // Striker batsman stats
        if (!isWide) {
            match.strikerBalls += 1
            if (!isBye && !isLegBye) {
                match.strikerRuns += runs
            }
        }

        // Bowler stats
        if (!isBye && !isLegBye) {
            match.bowlerRunsConceded += runsToAdd
        }
        if (isWicket) {
            match.bowlerWickets += 1
        }
        if (isLegalDelivery) {
            val bowlerOversInt = match.bowlerOvers.toInt()
            val bowlerBallsInt = Math.round((match.bowlerOvers - bowlerOversInt) * 10).toInt()
            val newBowlerBalls = bowlerBallsInt + 1
            if (newBowlerBalls >= 6) {
                match.bowlerOvers = (bowlerOversInt + 1).toDouble()
            } else {
                match.bowlerOvers = bowlerOversInt + (newBowlerBalls / 10.0)
            }
        }

        // Commentary text
        val commentaryText = buildString {
            append("Innings ${match.currentInnings}: ")
            if (extraType.isNotBlank()) append("$extraType! ")
            if (isWicket) append("WICKET! ")
            append("$runsToAdd runs. Score: ${if (match.currentInnings == 1) match.scoreA else match.scoreB}")
            append(" [${match.strikerName} ${match.strikerRuns}(${match.strikerBalls})]")
        }
        match.commentary.add(0, commentaryText)

        // Strike rotation on odd runs
        if (!isWide && runs % 2 != 0) {
            swapStrike(match)
        }

        // Strike rotation & event on over completion
        if (overCompleted) {
            swapStrike(match)
            match.commentary.add(0, "End of over. Strike rotated to ${match.strikerName}.")
        }

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    private fun swapStrike(match: Match) {
        val tempName = match.strikerName
        val tempRuns = match.strikerRuns
        val tempBalls = match.strikerBalls

        match.strikerName = match.nonStrikerName
        match.strikerRuns = match.nonStrikerRuns
        match.strikerBalls = match.nonStrikerBalls

        match.nonStrikerName = tempName
        match.nonStrikerRuns = tempRuns
        match.nonStrikerBalls = tempBalls
    }

    fun rotateStrike(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        swapStrike(match)
        match.commentary.add(0, "Manual strike change: ${match.strikerName} is now on strike.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun retireBatsman(matchId: String, newBatsmanName: String) {
        val match = repository.getMatchById(matchId) ?: return
        val retiredName = match.strikerName
        val retiredRuns = match.strikerRuns
        val retiredBalls = match.strikerBalls

        match.commentary.add(0, "Retired: $retiredName retired on $retiredRuns ($retiredBalls balls). New batsman $newBatsmanName takes strike.")
        match.strikerName = newBatsmanName
        match.strikerRuns = 0
        match.strikerBalls = 0

        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun changeBowler(matchId: String, newBowlerName: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.bowlerName = newBowlerName
        match.bowlerOvers = 0.0
        match.bowlerRunsConceded = 0
        match.bowlerWickets = 0
        match.commentary.add(0, "New Bowler: $newBowlerName comes into the attack.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun endOverManually(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        swapStrike(match)
        match.commentary.add(0, "Over concluded manually. Strike rotated to ${match.strikerName}.")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
    }

    fun undoLastBall(matchId: String): Boolean {
        val match = repository.getMatchById(matchId) ?: return false
        if (match.historyStack.isNotEmpty()) {
            val lastState = match.historyStack.removeAt(match.historyStack.size - 1)
            match.runsA = lastState.runsA
            match.wicketsA = lastState.wicketsA
            match.oversA = lastState.oversA
            match.runsB = lastState.runsB
            match.wicketsB = lastState.wicketsB
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
            match.bowlerOvers = lastState.bowlerOvers
            match.bowlerRunsConceded = lastState.bowlerRunsConceded
            match.bowlerWickets = lastState.bowlerWickets

            match.partnershipRuns = lastState.partnershipRuns
            match.partnershipBalls = lastState.partnershipBalls
            match.extrasWide = lastState.extrasWide
            match.extrasNoBall = lastState.extrasNoBall
            match.extrasBye = lastState.extrasBye
            match.extrasLegBye = lastState.extrasLegBye

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

    fun switchInnings(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.currentInnings = 2
        match.target = match.runsA + 1
        // Reset partnership and openers for 2nd innings
        match.partnershipRuns = 0
        match.partnershipBalls = 0
        val tempTeam = match.battingTeam
        match.battingTeam = match.bowlingTeam
        match.bowlingTeam = tempTeam
        match.strikerName = "${match.battingTeam} Opener 1"
        match.strikerRuns = 0
        match.strikerBalls = 0
        match.nonStrikerName = "${match.battingTeam} Opener 2"
        match.nonStrikerRuns = 0
        match.nonStrikerBalls = 0
        match.bowlerName = "${match.bowlingTeam} Bowler 1"
        match.bowlerOvers = 0.0
        match.bowlerRunsConceded = 0
        match.bowlerWickets = 0

        match.commentary.add(0, "Innings 1 concluded. Target: ${match.target} runs in ${match.totalOvers} overs. Innings 2 starts!")
        _selectedMatch.value = match
        repository.syncMatchUpdate(match)
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
    }
}
