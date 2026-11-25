package com.example.twelveman.data.repository

import com.example.twelveman.data.api.RetrofitClient
import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class BookingRepository {

    private val apiService = RetrofitClient.getApiService()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getBookings(): Flow<List<Booking>> = flow {
        // Emit cached data immediately without duplicates
        emit(MockData.bookings.distinctBy { it.id })

        try {
            val response = apiService.getBookings()
            if (response.isSuccessful && response.body()?.data != null) {
                val remoteBookings = response.body()!!.data!!.distinctBy { it.id }
                if (remoteBookings.isNotEmpty()) {
                    for (remote in remoteBookings) {
                        val existingIndex = MockData.bookings.indexOfFirst { it.id.equals(remote.id, ignoreCase = true) }
                        if (existingIndex != -1) {
                            MockData.bookings[existingIndex] = remote
                        } else {
                            MockData.bookings.add(remote)
                        }
                    }
                    emit(MockData.bookings.distinctBy { it.id })
                }
            }
        } catch (e: Exception) {
            // Retain cache on network exception
        }
    }.flowOn(Dispatchers.IO)

    fun pollBookings(intervalMs: Long = 5000L): Flow<List<Booking>> = flow {
        emit(MockData.bookings.distinctBy { it.id })

        while (true) {
            try {
                val response = apiService.getBookings()
                if (response.isSuccessful && response.body()?.data != null) {
                    val remoteBookings = response.body()!!.data!!.distinctBy { it.id }
                    if (remoteBookings.isNotEmpty()) {
                        for (remote in remoteBookings) {
                            val existingIndex = MockData.bookings.indexOfFirst { it.id.equals(remote.id, ignoreCase = true) }
                            if (existingIndex != -1) {
                                MockData.bookings[existingIndex] = remote
                            } else {
                                MockData.bookings.add(remote)
                            }
                        }
                        emit(MockData.bookings.distinctBy { it.id })
                    }
                }
            } catch (ignored: Exception) { }
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    fun addBooking(booking: Booking) {
        val existing = MockData.bookings.find {
            it.id == booking.id || (it.itemId == booking.itemId && it.date == booking.date && it.venue == booking.venue)
        }
        if (existing == null) {
            MockData.bookings.add(0, booking)
        }
        scope.launch {
            try {
                apiService.createBooking(booking)
            } catch (ignored: Exception) { }
        }
    }

    fun cancelBooking(bookingId: String): Boolean {
        val index = MockData.bookings.indexOfFirst { it.id.equals(bookingId, ignoreCase = true) }
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
