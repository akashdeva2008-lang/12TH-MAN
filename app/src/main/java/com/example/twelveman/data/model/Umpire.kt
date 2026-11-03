package com.example.twelveman.data.model

data class Umpire(
    val id: String = "",
    val name: String = "",
    val experienceYears: Int = 0,
    val matchesOfficiated: Int = 0,
    val rating: Double = 0.0,
    val certificationLevel: String = "", // "State Level", "National Certified", "ICC Accredited"
    val pricePerMatch: Int = 0,
    val location: String = "",
    val availabilityStatus: String = "Available",
    val imageResId: Int = 0,
    val bio: String = "",
    val phone: String = ""
)