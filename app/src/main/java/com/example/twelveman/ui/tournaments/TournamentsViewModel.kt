package com.example.twelveman.ui.tournaments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Match
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
        match.status = "LIVE"

        if (match.currentInnings == 1) {
            match.runsA += runs
            if (isWicket) match.wicketsA += 1
            // increment overs by 0.1
            var balls = (match.oversA * 10).toInt() + 1
            if (balls % 10 >= 6) {
                match.oversA = (match.oversA.toInt() + 1).toDouble()
            } else {
                match.oversA += 0.1
            }
            // round to 1 decimal place to prevent float drift
            match.oversA = String.format(Locale.US, "%.1f", match.oversA).toDouble()
            match.scoreA = "${match.runsA}/${match.wicketsA} (${match.oversA})"
            
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
            var balls = (match.oversB * 10).toInt() + 1
            if (balls % 10 >= 6) {
                match.oversB = (match.oversB.toInt() + 1).toDouble()
            } else {
                match.oversB += 0.1
            }
            match.oversB = String.format(Locale.US, "%.1f", match.oversB).toDouble()
            match.scoreB = "${match.runsB}/${match.wicketsB} (${match.oversB})"

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
        else if (match.runsB > match.runsA) "${match.teamB} won"
        else "Match Tied"
        match.commentary.add(0, "Match Concluded: ${match.result}")
        _selectedMatch.value = match
    }
}