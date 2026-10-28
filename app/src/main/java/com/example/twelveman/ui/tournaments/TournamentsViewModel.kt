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

    fun recordBall(matchId: String, runs: Int, isWicket: Boolean, extraType: String) {
        val match = repository.getMatchById(matchId) ?: return
        
        // Save snapshot for Undo
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
                scoreB = match.scoreB
            )
        )

        match.status = "LIVE"
        val isLegalDelivery = extraType.isBlank() || extraType.contains("Bye")

        if (match.currentInnings == 1) {
            match.runsA += runs
            if (isWicket) match.wicketsA += 1
            if (isLegalDelivery) {
                val currentOversInt = match.oversA.toInt()
                val currentBallsInt = Math.round((match.oversA - currentOversInt) * 10).toInt()
                val newBalls = currentBallsInt + 1
                if (newBalls >= 6) {
                    match.oversA = (currentOversInt + 1).toDouble()
                } else {
                    match.oversA = currentOversInt + (newBalls / 10.0)
                }
            }
            match.scoreA = "${match.runsA}/${match.wicketsA} (${String.format(Locale.US, "%.1f", match.oversA)} ov)"
            
            val commentaryText = buildString {
                append("Innings 1: ")
                if (extraType.isNotBlank()) append("$extraType! ")
                if (isWicket) append("WICKET! ")
                append("$runs runs scored. Score: ${match.scoreA}")
            }
            match.commentary.add(0, commentaryText)
        } else {
            match.runsB += runs
            if (isWicket) match.wicketsB += 1
            if (isLegalDelivery) {
                val currentOversInt = match.oversB.toInt()
                val currentBallsInt = Math.round((match.oversB - currentOversInt) * 10).toInt()
                val newBalls = currentBallsInt + 1
                if (newBalls >= 6) {
                    match.oversB = (currentOversInt + 1).toDouble()
                } else {
                    match.oversB = currentOversInt + (newBalls / 10.0)
                }
            }
            match.scoreB = "${match.runsB}/${match.wicketsB} (${String.format(Locale.US, "%.1f", match.oversB)} ov)"

            val commentaryText = buildString {
                append("Innings 2: ")
                if (extraType.isNotBlank()) append("$extraType! ")
                if (isWicket) append("WICKET! ")
                append("$runs runs scored. Score: ${match.scoreB}")
            }
            match.commentary.add(0, commentaryText)
        }

        _selectedMatch.value = match
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
            if (match.commentary.isNotEmpty()) {
                match.commentary.removeAt(0)
            }
            match.commentary.add(0, "Undo performed: Restored previous delivery state.")
            _selectedMatch.value = match
            return true
        }
        return false
    }

    fun switchInnings(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.currentInnings = 2
        match.commentary.add(0, "Innings break. Team B is now batting.")
        _selectedMatch.value = match
    }

    fun completeMatch(matchId: String) {
        val match = repository.getMatchById(matchId) ?: return
        match.status = "COMPLETED"
        match.result = if (match.runsA > match.runsB) "${match.teamA} won by ${match.runsA - match.runsB} runs"
        else if (match.runsB > match.runsA) "${match.teamB} won by ${match.wicketsB.let { 10 - it }} wickets"
        else "Match Tied"
        match.commentary.add(0, "Match Concluded: ${match.result}")
        _selectedMatch.value = match
    }
}