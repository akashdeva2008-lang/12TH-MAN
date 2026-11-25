package com.example.twelveman.data.model

import java.util.UUID

data class TeamPlayerItem(
    val id: String = UUID.randomUUID().toString(),
    val playerName: String = "",
    val role: String = "Batsman", // "Batsman", "Bowler", "All-Rounder", "Wicketkeeper"
    val jerseyNumber: String = "",
    val contact: String = "",
    var isCaptain: Boolean = false,
    var isViceCaptain: Boolean = false
)

data class Team(
    val id: String = "",
    val numeric_id: Int = 0,
    val tournamentId: String = "",
    val teamName: String = "",
    val shortName: String = "",
    val captainName: String = "",
    val captainPhone: String = "",
    val captainEmail: String = "",
    val viceCaptain: String = "",
    val managerName: String = "",
    val description: String = "",
    val city: String = "Mumbai",
    val logoUrl: String? = null,
    val approvalStatus: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED"
    val groupName: String = "Group A",
    val playerNames: List<String> = emptyList(),
    val players: List<TeamPlayerItem> = emptyList(),
    val playerCount: Int = 0,
    val registrationDate: String = "",
    val isCustomCreated: Boolean = true
)

data class TeamOpResult(
    val success: Boolean,
    val message: String,
    val team: Team? = null
)
