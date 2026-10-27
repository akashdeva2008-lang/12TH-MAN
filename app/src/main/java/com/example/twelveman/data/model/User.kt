package com.example.twelveman.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String // "PLAYER", "UMPIRE", "ORGANIZER", "FAN"
)