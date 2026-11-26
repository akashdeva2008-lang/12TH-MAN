package com.example.twelveman.ui.umpires

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.data.repository.BookingRepository
import com.example.twelveman.data.repository.UmpireRepository
import kotlinx.coroutines.launch
import java.util.UUID

class UmpiresViewModel : ViewModel() {
    private val repository = UmpireRepository()
    private val bookingRepository = BookingRepository()

    private val _umpiresList = MutableLiveData<List<Umpire>>()
    val umpiresList: LiveData<List<Umpire>> = _umpiresList

    private val _selectedUmpire = MutableLiveData<Umpire?>()
    val selectedUmpire: LiveData<Umpire?> = _selectedUmpire

    private val _myUmpireBookings = MutableLiveData<List<Booking>>()
    val myUmpireBookings: LiveData<List<Booking>> = _myUmpireBookings

    private val _bookingActionSuccess = MutableLiveData<String?>()
    val bookingActionSuccess: LiveData<String?> = _bookingActionSuccess

    private var allCachedUmpires: List<Umpire> = emptyList()
    private var currentFilterCertification: String = "ALL"
    private var currentSearchQuery: String = ""

    init {
        startRealtimeSync()
        loadMyUmpireBookings()
    }

    fun startRealtimeSync() {
        viewModelScope.launch {
            repository.pollUmpires(intervalMs = 10000L).collect { umpires ->
                allCachedUmpires = umpires.distinctBy { it.id }
                applyFilters()
            }
        }
    }

    fun loadMyUmpireBookings() {
        viewModelScope.launch {
            bookingRepository.getBookings().collect { allBookings ->
                val umpireOnly = allBookings.filter {
                    it.bookingType.equals("UMPIRE", ignoreCase = true) ||
                    it.itemName.contains("Umpire", ignoreCase = true) ||
                    allCachedUmpires.any { u -> u.name.equals(it.playerName, ignoreCase = true) }
                }
                _myUmpireBookings.value = umpireOnly
            }
        }
    }

    fun fetchUmpires(query: String = "") {
        currentSearchQuery = query.trim()
        applyFilters()
    }

    fun filterByCertification(cert: String) {
        currentFilterCertification = cert.trim()
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allCachedUmpires

        if (currentFilterCertification.isNotBlank() && !currentFilterCertification.equals("ALL", ignoreCase = true)) {
            when (currentFilterCertification.uppercase()) {
                "ICC ACCREDITED" -> filtered = filtered.filter { it.certificationLevel.contains("ICC", ignoreCase = true) }
                "NATIONAL LEVEL" -> filtered = filtered.filter { it.certificationLevel.contains("National", ignoreCase = true) }
                "STATE LEVEL" -> filtered = filtered.filter { it.certificationLevel.contains("State", ignoreCase = true) }
                "UNDER ₹2,000" -> filtered = filtered.filter { it.pricePerMatch <= 2000 }
                else -> filtered = filtered.filter { it.certificationLevel.contains(currentFilterCertification, ignoreCase = true) }
            }
        }

        if (currentSearchQuery.isNotBlank()) {
            val q = currentSearchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.certificationLevel.lowercase().contains(q)
            }
        }

        _umpiresList.value = filtered
    }

    fun loadUmpireDetails(id: String) {
        _selectedUmpire.value = repository.getUmpireById(id)
    }

    fun bookUmpire(
        umpire: Umpire,
        matchTitle: String,
        date: String,
        timeSlot: String,
        venue: String,
        matchType: String,
        notes: String,
        contactPhone: String
    ) {
        val bookingId = "BK" + (1000..9999).random()
        val formattedNotes = buildString {
            if (matchTitle.isNotBlank()) append("Match: $matchTitle; ")
            if (contactPhone.isNotBlank()) append("Contact: $contactPhone; ")
            if (notes.isNotBlank()) append(notes)
        }

        val newBooking = Booking(
            id = bookingId,
            bookingType = "UMPIRE",
            itemId = umpire.id,
            itemName = "${umpire.name} (Official Umpire)",
            userEmail = "akashdeva2008@gmail.com",
            date = date,
            timeSlot = timeSlot,
            venue = venue,
            matchType = matchType,
            status = "CONFIRMED",
            amount = umpire.pricePerMatch,
            notes = formattedNotes,
            paymentStatus = "PAID"
        )

        bookingRepository.addBooking(newBooking)
        loadMyUmpireBookings()
        _bookingActionSuccess.value = "Umpire ${umpire.name} successfully booked for $date!"
    }

    fun cancelUmpireBooking(bookingId: String) {
        val success = bookingRepository.cancelBooking(bookingId)
        if (success) {
            loadMyUmpireBookings()
            _bookingActionSuccess.value = "Booking #$bookingId cancelled."
        }
    }

    fun clearActionMessage() {
        _bookingActionSuccess.value = null
    }
}
