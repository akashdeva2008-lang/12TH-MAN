package com.example.twelveman.ui.players

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.api.models.AuthResponse
import com.example.twelveman.data.api.models.PlayerRegistrationRequest
import com.example.twelveman.data.model.Player
import com.example.twelveman.data.repository.PlayerRepository
import kotlinx.coroutines.launch

class PlayersViewModel : ViewModel() {
    private val repository = PlayerRepository()

    private val _playersList = MutableLiveData<List<Player>>()
    val playersList: LiveData<List<Player>> = _playersList

    private val _selectedPlayer = MutableLiveData<Player?>()
    val selectedPlayer: LiveData<Player?> = _selectedPlayer

    private val _myPlayerProfile = MutableLiveData<Map<String, Any>?>()
    val myPlayerProfile: LiveData<Map<String, Any>?> = _myPlayerProfile

    fun registerPlayer(request: PlayerRegistrationRequest, onResult: (Result<AuthResponse>) -> Unit) {
        viewModelScope.launch {
            val result = repository.registerPlayer(request)
            if (result.isSuccess) {
                fetchPlayers()
            }
            onResult(result)
        }
    }

    fun loadMyPlayerProfile(userId: Int = 0) {
        viewModelScope.launch {
            repository.fetchMyPlayerProfile(userId).onSuccess { data ->
                _myPlayerProfile.value = data
            }
        }
    }

    fun updateMyPlayerProfile(payload: Map<String, Any>, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateMyPlayerProfile(payload)
            if (res.isSuccess) {
                loadMyPlayerProfile()
            }
            onResult(res)
        }
    }

    private var currentQuery = ""
    private var currentRoleFilter = "All"

    private var cachedRawPlayers: List<Player> = emptyList()

    init {
        startRealtimeSync()
    }

    fun startRealtimeSync() {
        viewModelScope.launch {
            repository.pollPlayers(intervalMs = 6000L).collect { players ->
                cachedRawPlayers = players
                _playersList.value = filterPlayers(players, currentQuery, currentRoleFilter)
            }
        }
    }

    fun fetchPlayers() {
        if (cachedRawPlayers.isNotEmpty()) {
            _playersList.value = filterPlayers(cachedRawPlayers, currentQuery, currentRoleFilter)
        }
        viewModelScope.launch {
            repository.getPlayers().collect { players ->
                cachedRawPlayers = players
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