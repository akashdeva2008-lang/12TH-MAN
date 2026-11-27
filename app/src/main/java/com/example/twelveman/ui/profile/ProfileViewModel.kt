package com.example.twelveman.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.User
import com.example.twelveman.data.repository.AuthRepository
import com.example.twelveman.data.repository.TournamentRepository
import com.example.twelveman.utils.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val tournamentRepository = TournamentRepository()
    private val sessionManager = SessionManager.getInstance(application)

    private val _user = MutableLiveData<User?>(sessionManager.getCurrentUser())
    val user: LiveData<User?> = _user

    private val _myRegisteredTeams = MutableLiveData<List<Team>>(emptyList())
    val myRegisteredTeams: LiveData<List<Team>> = _myRegisteredTeams

    private val _isLoadingTeams = MutableLiveData<Boolean>(false)
    val isLoadingTeams: LiveData<Boolean> = _isLoadingTeams

    private val _darkModeEnabled = MutableLiveData<Boolean>(false)
    val darkModeEnabled: LiveData<Boolean> = _darkModeEnabled

    private val _notificationsEnabled = MutableLiveData<Boolean>(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _locationEnabled = MutableLiveData<Boolean>(true)
    val locationEnabled: LiveData<Boolean> = _locationEnabled

    private val _currency = MutableLiveData<String>("INR (₹)")
    val currency: LiveData<String> = _currency

    init {
        refreshProfile()
        fetchMyRegisteredTeams()
    }

    fun refreshProfile() {
        val localUser = sessionManager.getCurrentUser()
        if (localUser != null) {
            _user.value = localUser
        }

        if (sessionManager.isLoggedIn()) {
            viewModelScope.launch {
                val result = authRepository.fetchProfile()
                if (result is Resource.Success && result.data != null) {
                    _user.value = result.data
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _user.value = null
    }

    fun fetchMyRegisteredTeams() {
        _isLoadingTeams.value = true
        viewModelScope.launch {
            try {
                tournamentRepository.getTournamentTeams("").collect { allTeams ->
                    _myRegisteredTeams.postValue(allTeams.distinctBy { it.id })
                    _isLoadingTeams.postValue(false)
                }
            } catch (e: Exception) {
                _isLoadingTeams.postValue(false)
            }
        }
    }

    fun createOrUpdateTeam(team: Team, isEdit: Boolean, onComplete: (Boolean, String, Team?) -> Unit) {
        viewModelScope.launch {
            val result = if (isEdit) {
                tournamentRepository.updateCustomTeam(team)
            } else {
                tournamentRepository.createCustomTeam(team)
            }
            if (result.success) {
                fetchMyRegisteredTeams()
            }
            onComplete(result.success, result.message, result.team)
        }
    }

    fun deleteTeam(teamId: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = tournamentRepository.deleteCustomTeamWithResult(teamId)
            if (result.success) {
                fetchMyRegisteredTeams()
            }
            onComplete(result.success, result.message)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _darkModeEnabled.value = enabled
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun toggleLocation(enabled: Boolean) {
        _locationEnabled.value = enabled
    }

    fun setCurrency(curr: String) {
        _currency.value = curr
    }
}
