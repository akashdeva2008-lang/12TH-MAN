package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class BookingRepository {

    private val apiService = RetrofitClient.getApiService()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getBookings(): Flow<List<Booking>> = flow {
        // Emit cached data immediately
        emit(MockData.bookings)

        try {
            val response = apiService.getBookings()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteBookings = response.body()!!.data!!
                if (remoteBookings.isNotEmpty()) {
                    MockData.bookings.clear()
                    MockData.bookings.addAll(remoteBookings)
                    emit(remoteBookings)
                }
            }
        } catch (e: Exception) {
            // Retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun addBooking(booking: Booking) {
        MockData.bookings.add(0, booking)
        scope.launch {
            try {
                apiService.createBooking(booking)
            } catch (ignored: Exception) { }
        }
    }

    fun cancelBooking(bookingId: String): Boolean {
        val index = MockData.bookings.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            val updated = MockData.bookings[index].copy(status = "CANCELLED")
            MockData.bookings[index] = updated
            scope.launch {
                try {
                    apiService.cancelBooking(mapOf("id" to bookingId))
                } catch (ignored: Exception) { }
            }
            return true
        }
        return false
    }
}
