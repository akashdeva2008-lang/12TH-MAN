package com.example.twelveman.data.model

data class Tournament(
    val id: String,
    val name: String,
    val format: String, // "T20", "ODI", "Box Cricket"
    val location: String,
    val startDate: String,
    val endDate: String,
    val totalTeams: Int,
    val registeredTeamsCount: Int,
    val entryFee: Int,
    val prizeMoney: Int,
    val status: String, // "UPCOMING", "ONGOING", "COMPLETED"
    val organizerName: String,
    val organizerContact: String,
    val description: String
)