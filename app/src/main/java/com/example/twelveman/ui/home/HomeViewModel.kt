package com.example.twelveman.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Player
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.data.repository.PlayerRepository
import com.example.twelveman.data.repository.TournamentRepository
import com.example.twelveman.data.repository.UmpireRepository
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

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            playerRepository.getPlayers().collect { players ->
                _featuredPlayers.value = players.take(3)
            }
        }
        viewModelScope.launch {
            umpireRepository.getUmpires().collect { umpires ->
                _topUmpires.value = umpires.take(3)
            }
        }
        viewModelScope.launch {
            tournamentRepository.getTournaments().collect { tournaments ->
                _upcomingTournaments.value = tournaments.take(2)
            }
        }
    }
}