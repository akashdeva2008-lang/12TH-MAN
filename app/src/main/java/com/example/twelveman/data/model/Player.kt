package com.example.twelveman.data.model

data class Player(
    val id: String = "",
    val name: String = "",
    val role: String = "", // "Batsman", "Bowler", "All-Rounder", "Wicket Keeper"
    val rating: Double = 0.0,
    val matchesPlayed: Int = 0,
    val runsScored: Int = 0,
    val wicketsTaken: Int = 0,
    val pricePerMatch: Int = 0, // e.g., 1500 INR
    val location: String = "",
    val availabilityStatus: String = "Available", // "Available", "Booked"
    val imageResId: Int = 0,
    val bio: String = "",
    val phone: String = "",
    val email: String = ""
)