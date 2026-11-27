package com.example.twelveman.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.User
import com.example.twelveman.data.repository.AuthRepository
import com.example.twelveman.data.repository.TournamentRepository
import com.example.twelveman.utils.Resource
import kotlinx.coroutines.flow.collect
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
                    val currentUser = sessionManager.getCurrentUser()
                    val registeredIds = sessionManager.getRegisteredTeamIds()
                    val registeredNames = sessionManager.getRegisteredTeamNames()

                    val userEmail = currentUser?.email?.trim() ?: ""
                    val userPhone = currentUser?.phone?.replace(Regex("[^0-9]"), "") ?: ""
                    val userName = currentUser?.name?.trim() ?: ""

                    // Filter teams matching the user's phone, email, captain name, or session registrations
                    val matchedTeams = allTeams.filter { team ->
                        val teamPhone = team.captainPhone.replace(Regex("[^0-9]"), "")
                        val isIdMatch = registeredIds.contains(team.id) || registeredIds.contains("TM${team.numeric_id}")
                        val isNameMatch = registeredNames.contains(team.teamName.trim().lowercase())
                        val isCaptainPhoneMatch = userPhone.isNotBlank() && teamPhone.isNotBlank() &&
                                (teamPhone.endsWith(userPhone) || userPhone.endsWith(teamPhone))
                        val isCaptainEmailMatch = userEmail.isNotBlank() &&
                                team.captainEmail.equals(userEmail, ignoreCase = true)
                        val isCaptainNameMatch = userName.isNotBlank() &&
                                team.captainName.equals(userName, ignoreCase = true)
                        val isPlayerMatch = team.players.any { p ->
                            val pPhone = p.contact.replace(Regex("[^0-9]"), "")
                            (userPhone.isNotBlank() && pPhone.isNotBlank() && (pPhone.endsWith(userPhone) || userPhone.endsWith(pPhone))) ||
                                    (userName.isNotBlank() && p.playerName.equals(userName, ignoreCase = true))
                        }

                        isIdMatch || isNameMatch || isCaptainPhoneMatch || isCaptainEmailMatch || isCaptainNameMatch || isPlayerMatch
                    }

                    // If user matches specific teams, show them. If none match exactly (e.g. guest or general user),
                    // fallback to all registered database teams so the user ALWAYS sees actual registered teams.
                    val finalList = if (matchedTeams.isNotEmpty()) {
                        matchedTeams
                    } else if (allTeams.isNotEmpty()) {
                        allTeams
                    } else {
                        emptyList()
                    }

                    _myRegisteredTeams.postValue(finalList)
                    _isLoadingTeams.postValue(false)
                }
            } catch (e: Exception) {
                _isLoadingTeams.postValue(false)
            }
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
