package com.example.twelveman.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Player
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.data.repository.PlayerRepository
import com.example.twelveman.data.repository.TournamentRepository
import com.example.twelveman.data.repository.UmpireRepository
import com.example.twelveman.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val playerRepository = PlayerRepository()
    private val umpireRepository = UmpireRepository()
    private val tournamentRepository = TournamentRepository()

    private val _featuredPlayers = MutableLiveData<List<Player>>()
    val featuredPlayers: LiveData<List<Player>> = _featuredPlayers

    private val _topUmpires = MutableLiveData<List<Umpire>>()
    val topUmpires: LiveData<List<Umpire>> = _topUmpires

    private val _upcomingTournaments = MutableLiveData<List<Tournament>>()
    val upcomingTournaments: LiveData<List<Tournament>> = _upcomingTournaments

    private val _matchesState = MutableLiveData<Resource<List<Match>>>(Resource.Loading())
    val matchesState: LiveData<Resource<List<Match>>> = _matchesState

    private val _liveMatch = MutableLiveData<Match?>()
    val liveMatch: LiveData<Match?> = _liveMatch

    private val _featuredUpcomingMatch = MutableLiveData<Match?>()
    val featuredUpcomingMatch: LiveData<Match?> = _featuredUpcomingMatch

    private val _recentResult = MutableLiveData<Match?>()
    val recentResult: LiveData<Match?> = _recentResult

    private var matchesSyncJob: Job? = null
    private var secondarySyncStarted = false

    init {
        startRealtimeSync()
    }

    fun startRealtimeSync() {
        startMatchesSync()

        if (!secondarySyncStarted) {
            secondarySyncStarted = true
            // 2. Real-time Players sync
            viewModelScope.launch {
                playerRepository.pollPlayers(intervalMs = 8000L).collect { players ->
                    _featuredPlayers.value = players.distinctBy { it.id }.take(3)
                }
            }

            // 3. Real-time Umpires sync
            viewModelScope.launch {
                umpireRepository.pollUmpires(intervalMs = 10000L).collect { umpires ->
                    _topUmpires.value = umpires.distinctBy { it.id }.take(3)
                }
            }

            // 4. Real-time Tournaments sync
            viewModelScope.launch {
                tournamentRepository.pollTournaments(intervalMs = 10000L).collect { tournaments ->
                    _upcomingTournaments.value = tournaments.distinctBy { it.id }.take(2)
                }
            }
        }
    }

    fun retrySync() {
        startMatchesSync()
    }

    private fun startMatchesSync() {
        matchesSyncJob?.cancel()
        matchesSyncJob = viewModelScope.launch {
            tournamentRepository.pollDashboardMatchesState(intervalMs = 5000L).collect { resource ->
                _matchesState.value = resource
                if (resource is Resource.Success) {
                    val distinct = (resource.data ?: emptyList()).distinctBy { it.id }
                    _liveMatch.value = distinct.firstOrNull { it.status.equals("LIVE", ignoreCase = true) }
                    _featuredUpcomingMatch.value = distinct.firstOrNull { it.status.equals("UPCOMING", ignoreCase = true) }
                    _recentResult.value = distinct.firstOrNull { it.status.equals("COMPLETED", ignoreCase = true) }
                } else if (resource is Resource.Error) {
                    _liveMatch.value = null
                    _featuredUpcomingMatch.value = null
                    _recentResult.value = null
                }
            }
        }
    }
}
