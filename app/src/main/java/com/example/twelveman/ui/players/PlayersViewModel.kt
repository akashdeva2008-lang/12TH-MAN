package com.example.twelveman.ui.players

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Player
import com.example.twelveman.data.repository.PlayerRepository
import kotlinx.coroutines.launch

class PlayersViewModel : ViewModel() {
    private val repository = PlayerRepository()

    private val _playersList = MutableLiveData<List<Player>>()
    val playersList: LiveData<List<Player>> = _playersList

    private val _selectedPlayer = MutableLiveData<Player?>()
    val selectedPlayer: LiveData<Player?> = _selectedPlayer

    private var currentQuery = ""
    private var currentRoleFilter = "All"

    init {
        fetchPlayers()
    }

    fun fetchPlayers() {
        viewModelScope.launch {
            repository.getPlayers().collect { players ->
                _playersList.value = filterPlayers(players, currentQuery, currentRoleFilter)
            }
        }
    }

    fun search(query: String) {
        currentQuery = query
        fetchPlayers()
    }

    fun filterByRole(role: String) {
        currentRoleFilter = role
        fetchPlayers()
    }

    fun loadPlayerDetails(id: String) {
        _selectedPlayer.value = repository.getPlayerById(id)
    }

    private fun filterPlayers(list: List<Player>, query: String, role: String): List<Player> {
        return list.filter { p ->
            val matchesSearch = p.name.contains(query, ignoreCase = true) || p.location.contains(query, ignoreCase = true)
            val matchesRole = role == "All" || p.role.equals(role, ignoreCase = true)
            matchesSearch && matchesRole
        }
    }
}