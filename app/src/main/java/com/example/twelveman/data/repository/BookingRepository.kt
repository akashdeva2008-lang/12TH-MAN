package com.example.twelveman.data.repository

import com.example.twelveman.data.local.MockData
import com.example.twelveman.data.model.Booking
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BookingRepository {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun getBookings(): Flow<List<Booking>> = callbackFlow {
        try {
            val listener = db.collection("bookings").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    if (snapshot != null && snapshot.isEmpty) {
                        seedBookings()
                    }
                    trySend(MockData.bookings)
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)
                }
                if (list.isNotEmpty()) {
                    MockData.bookings.clear()
                    MockData.bookings.addAll(list)
                    trySend(list)
                } else {
                    trySend(MockData.bookings)
                }
            }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(MockData.bookings)
            awaitClose { }
        }
    }

    private fun seedBookings() {
        try {
            for (booking in MockData.bookings) {
                db.collection("bookings").document(booking.id).set(booking)
            }
        } catch (ignored: Exception) { }
    }

    fun addBooking(booking: Booking) {
        MockData.bookings.add(0, booking)
        try {
            db.collection("bookings").document(booking.id).set(booking)
        } catch (ignored: Exception) { }
    }

    fun cancelBooking(bookingId: String): Boolean {
        val index = MockData.bookings.indexOfFirst { it.id == bookingId }
        if (index != -1) {
            val updated = MockData.bookings[index].copy(status = "CANCELLED")
            MockData.bookings[index] = updated
            try {
                db.collection("bookings").document(bookingId).set(updated)
            } catch (ignored: Exception) { }
            return true
        }
        return false
    }
}