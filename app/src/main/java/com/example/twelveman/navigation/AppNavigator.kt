package com.example.twelveman.navigation

import android.os.Bundle
import androidx.navigation.NavController
import com.example.twelveman.R

object AppNavigator {
    fun navigateToPlayerDetails(navController: NavController, playerId: String) {
        val bundle = Bundle().apply { putString("playerId", playerId) }
        navController.navigate(R.id.action_players_to_details, bundle)
    }

    fun navigateToUmpireDetails(navController: NavController, umpireId: String) {
        val bundle = Bundle().apply { putString("umpireId", umpireId) }
        navController.navigate(R.id.action_umpires_to_details, bundle)
    }

    fun navigateToBookingForm(navController: NavController, itemId: String, itemType: String) {
        val bundle = Bundle().apply {
            putString("itemId", itemId)
            putString("itemType", itemType)
        }
        navController.navigate(R.id.bookingFormFragment, bundle)
    }

    fun navigateToTournamentDetails(navController: NavController, tournamentId: String) {
        val bundle = Bundle().apply { putString("tournamentId", tournamentId) }
        navController.navigate(R.id.tournamentDetailsFragment, bundle)
    }
}