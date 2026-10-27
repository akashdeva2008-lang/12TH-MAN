package com.example.twelveman.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.User

class AuthViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User?>(MockData.currentUser)
    val currentUser: LiveData<User?> = _currentUser

    private val _authState = MutableLiveData<String>()
    val authState: LiveData<String> = _authState

    fun login(email: String, pass: String): Boolean {
        if (email.isNotBlank() && pass.length >= 4) {
            val user = User("U100", "Rahul Dravid", email, "+91 99000 11222", "ORGANIZER")
            _currentUser.value = user
            _authState.value = "SUCCESS"
            return true
        } else {
            _authState.value = "Please enter valid credentials"
            return false
        }
    }

    fun register(name: String, email: String, phone: String, role: String, pass: String): Boolean {
        if (name.isNotBlank() && email.isNotBlank() && pass.length >= 4) {
            val user = User("U" + System.currentTimeMillis(), name, email, phone, role)
            _currentUser.value = user
            _authState.value = "SUCCESS"
            return true
        } else {
            _authState.value = "Please fill all required fields correctly"
            return false
        }
    }
}