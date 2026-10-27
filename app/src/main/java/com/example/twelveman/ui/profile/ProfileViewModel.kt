package com.example.twelveman.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.User

class ProfileViewModel : ViewModel() {
    private val _user = MutableLiveData<User>(MockData.currentUser)
    val user: LiveData<User> = _user

    private val _darkModeEnabled = MutableLiveData<Boolean>(false)
    val darkModeEnabled: LiveData<Boolean> = _darkModeEnabled

    private val _notificationsEnabled = MutableLiveData<Boolean>(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _locationEnabled = MutableLiveData<Boolean>(true)
    val locationEnabled: LiveData<Boolean> = _locationEnabled

    private val _currency = MutableLiveData<String>("INR (₹)")
    val currency: LiveData<String> = _currency

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