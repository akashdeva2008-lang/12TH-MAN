package com.example.twelveman.data.local

import com.example.twelveman.data.model.*

/**
 * In-memory runtime cache for 12THMAN.
 * Populated dynamically with live records from the database/API.
 * All dummy and static fake data have been removed.
 */
object MockData {
    val players = mutableListOf<Player>()
    val umpires = mutableListOf<Umpire>()
    val tournaments = mutableListOf<Tournament>()
    val bookings = mutableListOf<Booking>()
    val matches = mutableListOf<Match>()
    val teams = mutableListOf<Team>()
    var currentUser: User? = null
}
