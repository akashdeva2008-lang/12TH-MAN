package com.example.twelveman.data.model

data class Tournament(
    val id: String = "",
    val name: String = "",
    val format: String = "", // "T20", "ODI", "Box Cricket"
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalTeams: Int = 0,
    val registeredTeamsCount: Int = 0,
    val entryFee: Int = 0,
    val prizeMoney: Int = 0,
    val status: String = "UPCOMING", // "UPCOMING", "ONGOING", "COMPLETED"
    val organizerName: String = "",
    val organizerContact: String = "",
    val description: String = ""
)