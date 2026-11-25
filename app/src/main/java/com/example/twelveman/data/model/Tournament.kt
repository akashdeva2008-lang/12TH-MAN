package com.example.twelveman.data.model

data class Tournament(
    val id: String = "",
    val name: String = "",
    val format: String = "T20", // "T10", "T20", "ODI", "Box Cricket", "Test"
    val tournamentFormat: String = "KNOCKOUT", // "KNOCKOUT", "ROUND_ROBIN", "LEAGUE", "GROUP_KNOCKOUT", "CUSTOM"
    val location: String = "",
    val groundName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val registrationDeadline: String = "",
    val totalTeams: Int = 16,
    val registeredTeamsCount: Int = 0,
    val entryFee: Int = 0,
    val prizeMoney: Int = 0,
    val firstPrize: String = "₹50,000",
    val secondPrize: String = "₹25,000",
    val thirdPrize: String = "₹10,000",
    val rules: String = "",
    val overs: Int = 20,
    val isDraft: Int = 0,
    val status: String = "UPCOMING", // "UPCOMING", "ONGOING", "COMPLETED"
    val organizerName: String = "",
    val organizerContact: String = "",
    val description: String = "",
    val logoUrl: String? = null
)
