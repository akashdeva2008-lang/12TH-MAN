package com.example.twelveman.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableLiveData<User?>(MockData.currentUser)
    val currentUser: LiveData<User?> = _currentUser

    private val _authState = MutableLiveData<String>()
    val authState: LiveData<String> = _authState

    init {
        checkActiveUser()
    }

    private fun checkActiveUser() {
        try {
            val firebaseUser = auth.currentUser
            if (firebaseUser != null) {
                db.collection("users").document(firebaseUser.uid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            _currentUser.value = doc.toObject(User::class.java)
                        } else {
                            _currentUser.value = User(
                                id = firebaseUser.uid,
                                name = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email ?: "",
                                phone = "",
                                role = "ORGANIZER"
                            )
                        }
                    }
                    .addOnFailureListener {
                        _currentUser.value = MockData.currentUser
                    }
            }
        } catch (e: Exception) {
            _currentUser.value = MockData.currentUser
        }
    }

    fun login(email: String, pass: String): Boolean {
        if (email.isNotBlank() && pass.length >= 4) {
            val user = User("U100", "Rahul Dravid", email, "+91 99000 11222", "ORGANIZER")
            _currentUser.value = user
            _authState.value = "SUCCESS"

            try {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: "U100"
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    _currentUser.value = doc.toObject(User::class.java)
                                }
                            }
                    }
            } catch (ignored: Exception) { }

            return true
        } else {
            _authState.value = "Please enter valid credentials"
            return false
        }
    }

    fun register(name: String, email: String, phone: String, role: String, pass: String): Boolean {
        if (name.isNotBlank() && email.isNotBlank() && pass.length >= 4) {
            val newUser = User("U" + System.currentTimeMillis(), name, email, phone, role)
            _currentUser.value = newUser
            _authState.value = "SUCCESS"

            try {
                auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: newUser.id
                        val createdUser = newUser.copy(id = uid)
                        _currentUser.value = createdUser
                        db.collection("users").document(uid).set(createdUser)
                    }
            } catch (ignored: Exception) { }

            return true
        } else {
            _authState.value = "Please fill all required fields correctly"
            return false
        }
    }
}