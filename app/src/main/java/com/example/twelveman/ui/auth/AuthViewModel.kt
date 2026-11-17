package com.example.twelveman.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.User
import com.example.twelveman.data.repository.AuthRepository
import com.example.twelveman.utils.Resource
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)
    private val sessionManager = SessionManager.getInstance(application)

    private val _currentUser = MutableLiveData<User?>(sessionManager.getCurrentUser() ?: MockData.currentUser)
    val currentUser: LiveData<User?> = _currentUser

    private val _authState = MutableLiveData<String>()
    val authState: LiveData<String> = _authState

    private val _authResult = MutableLiveData<Resource<User>?>()
    val authResult: LiveData<Resource<User>?> = _authResult

    fun login(email: String, pass: String): Boolean {
        if (email.isBlank() || pass.isBlank()) {
            val msg = "Please enter both email and password"
            _authState.value = msg
            _authResult.value = Resource.Error(msg)
            return false
        }

        if (pass.length < 6) {
            val msg = "Password must be at least 6 characters"
            _authState.value = msg
            _authResult.value = Resource.Error(msg)
            return false
        }

        _authResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.login(email.trim(), pass)
            when (result) {
                is Resource.Success -> {
                    val user = result.data
                    _currentUser.value = user
                    _authState.value = "SUCCESS"
                    _authResult.value = result
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Invalid email or password"
                    _authState.value = msg
                    _authResult.value = Resource.Error(msg)
                }
                is Resource.Loading -> {
                    _authResult.value = Resource.Loading()
                }
            }
        }

        return true
    }

    fun register(name: String, email: String, phone: String, role: String, pass: String): Boolean {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            val msg = "Please fill all required fields"
            _authState.value = msg
            _authResult.value = Resource.Error(msg)
            return false
        }

        if (pass.length < 6) {
            val msg = "Password must be at least 6 characters long"
            _authState.value = msg
            _authResult.value = Resource.Error(msg)
            return false
        }

        _authResult.value = Resource.Loading()

        viewModelScope.launch {
            val result = repository.register(name.trim(), email.trim(), phone.trim(), role, pass)
            when (result) {
                is Resource.Success -> {
                    val user = result.data
                    _currentUser.value = user
                    _authState.value = "SUCCESS"
                    _authResult.value = result
                }
                is Resource.Error -> {
                    val msg = result.message ?: "Registration failed"
                    _authState.value = msg
                    _authResult.value = Resource.Error(msg)
                }
                is Resource.Loading -> {
                    _authResult.value = Resource.Loading()
                }
            }
        }

        return true
    }

    fun resetAuthResult() {
        _authResult.value = null
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authState.value = "LOGGED_OUT"
    }
}
