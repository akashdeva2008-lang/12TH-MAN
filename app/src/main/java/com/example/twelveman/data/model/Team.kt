package com.example.twelveman.data.model

data class Team(
    val id: String,
    val tournamentId: String,
    val teamName: String,
    val captainName: String,
    val captainPhone: String,
    val captainEmail: String,
    val playerNames: List<String> = emptyList(),
    val registrationDate: String = ""
)