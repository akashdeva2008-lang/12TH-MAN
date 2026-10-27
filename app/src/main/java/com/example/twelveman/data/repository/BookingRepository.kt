package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Booking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookingRepository {
    fun getBookings(): Flow<List<Booking>> = flow {
        emit(MockData.bookings)
    }

    fun addBooking(booking: Booking) {
        MockData.bookings.add(0, booking)
    }

    fun cancelBooking(bookingId: String): Boolean {
        val index = MockData.bookings.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            val updated = MockData.bookings[index].copy(status = "CANCELLED")
            MockData.bookings[index] = updated
            return true
        }
        return false
    }
}