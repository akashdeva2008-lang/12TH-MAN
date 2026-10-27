package com.example.twelveman.data.model

data class Booking(
    val id: String,
    val bookingType: String, // "PLAYER" or "UMPIRE"
    val itemId: String,
    val itemName: String,
    val userEmail: String,
    val date: String,
    val timeSlot: String,
    val venue: String,
    val matchType: String, // "T20", "50-Over", "Box Cricket"
    val status: String, // "CONFIRMED", "PENDING", "COMPLETED", "CANCELLED"
    val amount: Int,
    val notes: String = ""
)