package com.example.twelveman.data.repository

import android.content.Context
import com.example.twelveman.data.api.ApiService
import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.api.models.AuthResponse
import com.example.twelveman.data.api.models.LoginRequest
import com.example.twelveman.data.api.models.RegisterRequest
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.User
import com.example.twelveman.utils.Resource
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val apiService: ApiService = RetrofitClient.getApiService(context)
    private val sessionManager: SessionManager = SessionManager.getInstance(context)
    private val gson = Gson()

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        role: String,
        pass: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                fullName = name,
                email = email,
                phone = phone,
                password = pass,
                role = role
            )

            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val userData = authBody.data
                if (userData != null) {
                    val user = User(
                        id = userData.id.toString(),
                        name = userData.fullName,
                        email = userData.email,
                        phone = userData.phone,
                        role = userData.role
                    )
                    sessionManager.saveUser(user, userData.token)
                    Resource.Success(user)
                } else {
                    Resource.Error(authBody.message ?: "Registration succeeded but no user data received")
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Registration failed with code ${response.code()}"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error during registration: ${e.message}")
        }
    }

    suspend fun login(email: String, pass: String): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email = email, password = pass)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val userData = authBody.data
                if (userData != null) {
                    val user = User(
                        id = userData.id.toString(),
                        name = userData.fullName,
                        email = userData.email,
                        phone = userData.phone,
                        role = userData.role
                    )
                    sessionManager.saveUser(user, userData.token)
                    Resource.Success(user)
                } else {
                    Resource.Error(authBody.message ?: "Login succeeded but no user data received")
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Login failed with code ${response.code()}"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error: ${e.message}")
        }
    }

    suspend fun fetchProfile(): Resource<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                val userData = authBody.data
                if (userData != null) {
                    val user = User(
                        id = userData.id.toString(),
                        name = userData.fullName,
                        email = userData.email,
                        phone = userData.phone,
                        role = userData.role
                    )
                    sessionManager.saveUser(user)
                    Resource.Success(user)
                } else {
                    Resource.Error(authBody.message ?: "Failed to retrieve user profile")
                }
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                    ?: "Profile request failed (${response.code()})"
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error fetching profile: ${e.message}")
        }
    }

    fun getCurrentUser(): User? {
        return sessionManager.getCurrentUser()
    }

    fun isLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    fun logout() {
        sessionManager.clearSession()
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val parsed = gson.fromJson(errorBody, AuthResponse::class.java)
            parsed.message
        } catch (e: Exception) {
            null
        }
    }
}

