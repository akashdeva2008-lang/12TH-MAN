package com.example.twelveman.ui.bookings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.repository.BookingRepository
import kotlinx.coroutines.launch

class BookingsViewModel : ViewModel() {
    private val repository = BookingRepository()

    private val _bookings = MutableLiveData<List<Booking>>()
    val bookings: LiveData<List<Booking>> = _bookings

    private val _bookingResult = MutableLiveData<Boolean>()
    val bookingResult: LiveData<Boolean> = _bookingResult

    init {
        fetchBookings()
    }

    fun fetchBookings() {
        viewModelScope.launch {
            repository.getBookings().collect { list ->
                _bookings.value = list
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
        val newBooking = Booking(
            id = "B" + (100..999).random(),
            bookingType = type,
            itemId = itemId,
            itemName = itemName,
            userEmail = "user@example.com",
            date = date,
            timeSlot = time,
            venue = venue,
            matchType = matchType,
            status = "CONFIRMED",
            amount = amount,
            notes = notes
        )
        repository.addBooking(newBooking)
        _bookingResult.value = true
        fetchBookings()
    }

    fun cancelBooking(bookingId: String) {
        repository.cancelBooking(bookingId)
        fetchBookings()
    }
}