package com.example.twelveman.data.model

data class OrganizerDashboardData(
    val tournament: Tournament? = null,
    val stats: OrganizerStats? = null,
    val teams: List<Team> = emptyList(),
    val matches: List<Match> = emptyList()
)

data class OrganizerStats(
    val registeredTeams: Int = 0,
    val maxTeams: Int = 16,
    val totalMatches: Int = 0,
    val upcomingMatches: Int = 0,
    val liveMatches: Int = 0,
    val completedMatches: Int = 0,
    val progressPercent: Int = 0
)

data class GenerateFixturesRequest(
    val tournamentId: String,
    val format: String = "KNOCKOUT",
    val venue: String = "Cricket Ground",
    val startDate: String = "",
    val overs: Int = 20,
    val umpire: String = "Official Umpire",
    val scorer: String = "Official Scorer",
    val clearExisting: Boolean = true
)

data class ManageTeamRequest(
    val action: String, // "add_team", "create_team", "update_team", "remove_team", "delete_team", "update_status"
    val tournamentId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val shortName: String = "",
    val captainName: String = "",
    val captainPhone: String = "",
    val captainEmail: String = "",
    val viceCaptain: String = "",
    val managerName: String = "",
    val description: String = "",
    val city: String = "Mumbai",
    val logoUrl: String = "",
    val status: String = "APPROVED",
    val groupName: String = "Group A",
    val playerNames: List<String> = emptyList(),
    val players: List<TeamPlayerItem> = emptyList()
)
