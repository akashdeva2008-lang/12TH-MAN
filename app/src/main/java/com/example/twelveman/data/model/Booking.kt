package com.example.twelveman.data.model

data class Booking(
    val id: String = "",
    val bookingType: String = "PLAYER", // "PLAYER" or "UMPIRE"
    val itemId: String = "",
    val itemName: String = "",
    val userEmail: String = "",
    val date: String = "",
    val timeSlot: String = "",
    val venue: String = "",
    val matchType: String = "T20", // "T20", "50-Over", "Box Cricket"
    val status: String = "CONFIRMED", // "CONFIRMED", "PENDING", "COMPLETED", "CANCELLED"
    val amount: Int = 1800,
    val notes: String = "",
    val paymentStatus: String = "PAID"
) {
    val playerName: String
        get() = if (itemName.contains("(")) itemName.substringBefore("(").trim() else itemName.trim()

    val playerRole: String
        get() = if (itemName.contains("(") && itemName.contains(")")) {
            itemName.substringAfter("(").substringBefore(")").trim()
        } else if (bookingType.equals("UMPIRE", ignoreCase = true)) {
            "Certified Umpire"
        } else {
            "Batsman"
        }

    val isPlayerBooking: Boolean
        get() = bookingType.equals("PLAYER", ignoreCase = true)
}
