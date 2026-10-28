package com.example.twelveman.data.model

data class MatchStateSnapshot(
    val runsA: Int,
    val wicketsA: Int,
    val oversA: Double,
    val runsB: Int,
    val wicketsB: Int,
    val oversB: Double,
    val currentInnings: Int,
    val scoreA: String,
    val scoreB: String
)

data class Match(
    val id: String,
    val tournamentId: String,
    val matchNumber: Int,
    val teamA: String,
    val teamB: String,
    val date: String,
    val time: String,
    val venue: String,
    val stage: String, // "Group Stage", "Semi Final", "Final"
    var status: String, // "UPCOMING", "LIVE", "COMPLETED"
    var result: String = "",
    var scoreA: String = "0/0 (0.0)",
    var scoreB: String = "0/0 (0.0)",
    var runsA: Int = 0,
    var wicketsA: Int = 0,
    var oversA: Double = 0.0,
    var runsB: Int = 0,
    var wicketsB: Int = 0,
    var oversB: Double = 0.0,
    var currentInnings: Int = 1, // 1 for Team A batting, 2 for Team B batting
    val commentary: MutableList<String> = mutableListOf("Match scheduled. Ready for live scoring."),
    val historyStack: MutableList<MatchStateSnapshot> = mutableListOf()
)