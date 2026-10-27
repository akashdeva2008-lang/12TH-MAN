package com.example.twelveman.data.model

data class Umpire(
    val id: String,
    val name: String,
    val experienceYears: Int,
    val matchesOfficiated: Int,
    val rating: Double,
    val certificationLevel: String, // "State Level", "National Certified", "ICC Accredited"
    val pricePerMatch: Int,
    val location: String,
    val availabilityStatus: String,
    val imageResId: Int = 0,
    val bio: String = "",
    val phone: String = ""
)