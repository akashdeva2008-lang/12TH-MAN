package com.example.twelveman.data.model

data class TeamStanding(
    val teamId: String = "",
    val teamName: String = "",
    val groupName: String = "Group A",
    val position: Int = 1,
    val played: Int = 0,
    val won: Int = 0,
    val lost: Int = 0,
    val tied: Int = 0,
    val noResult: Int = 0,
    val points: Int = 0,
    val runsScored: Int = 0,
    val oversFaced: Double = 0.0,
    val runsConceded: Int = 0,
    val oversBowled: Double = 0.0,
    val nrr: Double = 0.0,
    val form: List<String> = emptyList()
)
