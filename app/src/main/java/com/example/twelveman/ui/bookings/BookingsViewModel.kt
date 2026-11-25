package com.example.twelveman.ui.bookings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.repository.BookingRepository
import com.example.twelveman.data.repository.TournamentRepository
import kotlinx.coroutines.launch

class BookingsViewModel(application: Application) : AndroidViewModel(application) {
    private val bookingRepository = BookingRepository()
    private val tournamentRepository = TournamentRepository()
    private val sessionManager = SessionManager.getInstance(application)

    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings

    private val _matches = MutableLiveData<List<Match>>()
    val matches: LiveData<List<Match>> = _matches

    private val _bookingResult = MutableLiveData<Boolean>()
    val bookingResult: LiveData<Boolean> = _bookingResult

    init {
        startRealtimeSync()
    }

    fun startRealtimeSync() {
        // Real-time polling for Bookings
        viewModelScope.launch {
            bookingRepository.pollBookings(intervalMs = 5000L).collect { list ->
                _bookings.value = list.distinctBy { it.id }
            }
        }

        // Real-time polling for Scheduled Matches
        viewModelScope.launch {
            tournamentRepository.pollDashboardMatches(intervalMs = 8000L).collect { list ->
                _matches.value = list.distinctBy { it.id }
            }
        }
    }

    fun fetchBookings() {
        viewModelScope.launch {
            bookingRepository.getBookings().collect { list ->
                _bookings.value = list.distinctBy { it.id }
            }
        }
    }

    fun createBooking(
        type: String,
        itemId: String,
        itemName: String,
        date: String,
        time: String,
        venue: String,
        matchType: String,
        amount: Int,
        notes: String
    ) {
        val userEmail = sessionManager.getCurrentUser()?.email ?: "user@example.com"
        val nextId = "B" + (404..999).random()
        val newBooking = Booking(
            id = nextId,
            bookingType = type,
            itemId = itemId,
            itemName = itemName,
            userEmail = userEmail,
            date = date,
            timeSlot = time,
            venue = venue,
            matchType = matchType,
            status = "CONFIRMED",
            amount = amount,
            notes = notes
        )
        bookingRepository.addBooking(newBooking)
        _bookingResult.value = true
        fetchBookings()
    }

    fun cancelBooking(bookingId: String) {
        val success = bookingRepository.cancelBooking(bookingId)
        if (success) {
            fetchBookings()
        }
    }
}
