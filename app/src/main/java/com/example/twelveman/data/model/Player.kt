package com.example.twelveman.data.model

data class Player(
    val id: String,
    val name: String,
    val role: String, // "Batsman", "Bowler", "All-Rounder", "Wicket Keeper"
    val rating: Double,
    val matchesPlayed: Int,
    val runsScored: Int,
    val wicketsTaken: Int,
    val pricePerMatch: Int, // e.g., 1500 INR
    val location: String,
    val availabilityStatus: String, // "Available", "Booked"
    val imageResId: Int = 0,
    val bio: String = "",
    val phone: String = "",
    val email: String = ""
)