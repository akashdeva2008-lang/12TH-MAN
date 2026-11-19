package com.example.twelveman.ui.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.data.model.Booking
import com.example.twelveman.databinding.FragmentBookingsBinding

class BookingsFragment : Fragment() {
    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingsViewModel by viewModels()
    private lateinit var adapter: BookingAdapter
    private var allBookings: List<Booking> = emptyList()
    private var activeFilter = "UPCOMING"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookingAdapter(emptyList()) { booking ->
            showBookingDetailsDialog(booking)
        }

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@BookingsFragment.adapter
        }

        viewModel.bookings.observe(viewLifecycleOwner) { list ->
            allBookings = list
            applyFilter(activeFilter)
        }

        // Tab click listeners
        binding.tabBookingUpcoming.setOnClickListener {
            activeFilter = "UPCOMING"
            applyFilter("UPCOMING")
        }

        binding.tabBookingLive.setOnClickListener {
            activeFilter = "LIVE"
            applyFilter("LIVE")
        }

        binding.tabBookingCompleted.setOnClickListener {
            activeFilter = "COMPLETED"
            applyFilter("COMPLETED")
        }

        // Search icon
        binding.ivBookingSearch.setOnClickListener {
            val etSearch = EditText(requireContext()).apply {
                hint = "Search by player, umpire or venue"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Search Bookings")
                .setView(etSearch)
                .setPositiveButton("Search") { _, _ ->
                    val query = etSearch.text.toString().trim()
                    val filtered = allBookings.filter {
                        it.itemName.contains(query, ignoreCase = true) ||
                        it.venue.contains(query, ignoreCase = true) ||
                        it.bookingType.contains(query, ignoreCase = true)
                    }
                    adapter.updateData(filtered)
                    Toast.makeText(requireContext(), "Found ${filtered.size} bookings", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Reset") { _, _ ->
                    applyFilter(activeFilter)
                }
                .show()
        }

        // Filter icon
        binding.ivBookingFilter.setOnClickListener {
            val types = arrayOf("All Bookings", "Players Only", "Umpires Only")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter by Category")
                .setItems(types) { _, which ->
                    when (which) {
                        0 -> applyFilter(activeFilter)
                        1 -> adapter.updateData(allBookings.filter { it.bookingType.equals("PLAYER", ignoreCase = true) })
                        2 -> adapter.updateData(allBookings.filter { it.bookingType.equals("UMPIRE", ignoreCase = true) })
                    }
                }
                .show()
        }
    }

    private fun applyFilter(filter: String) {
        val white = ContextCompat.getColor(requireContext(), R.color.pure_white)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset tab styles
        binding.tabBookingUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingUpcoming.setTextColor(textSecondary)
        binding.tabBookingLive.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingLive.setTextColor(textSecondary)
        binding.tabBookingCompleted.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingCompleted.setTextColor(textSecondary)

        val filteredList = when (filter) {
            "UPCOMING" -> {
                binding.tabBookingUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingUpcoming.setTextColor(white)
                allBookings.filter { it.status.equals("CONFIRMED", ignoreCase = true) || it.status.equals("UPCOMING", ignoreCase = true) }
            }
            "LIVE" -> {
                binding.tabBookingLive.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingLive.setTextColor(white)
                allBookings.filter { it.status.equals("IN PROGRESS", ignoreCase = true) || it.status.equals("LIVE", ignoreCase = true) }
            }
            "COMPLETED" -> {
                binding.tabBookingCompleted.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingCompleted.setTextColor(white)
                allBookings.filter { it.status.equals("COMPLETED", ignoreCase = true) || it.status.equals("CANCELLED", ignoreCase = true) }
            }
            else -> allBookings
        }

        adapter.updateData(if (filteredList.isNotEmpty()) filteredList else allBookings)
    }

    private fun showBookingDetailsDialog(booking: Booking) {
        val isCancellable = !booking.status.equals("CANCELLED", ignoreCase = true)

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Booking #${booking.id}")
            .setMessage(
                "Category: ${booking.bookingType}\n" +
                "Item / Name: ${booking.itemName}\n" +
                "Date: ${booking.date}\n" +
                "Time Slot: ${booking.timeSlot}\n" +
                "Venue: ${booking.venue}\n" +
                "Match Type: ${booking.matchType}\n" +
                "Total Amount: ₹${booking.amount}\n" +
                "Current Status: ${booking.status}\n" +
                (if (booking.notes.isNotBlank()) "Notes: ${booking.notes}\n" else "")
            )
            .setPositiveButton("OK", null)

        if (isCancellable) {
            builder.setNegativeButton("Cancel Booking") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Cancel Booking Confirmation")
                    .setMessage("Are you sure you want to cancel booking #${booking.id} (${booking.itemName})?")
                    .setPositiveButton("Yes, Cancel") { _, _ ->
                        viewModel.cancelBooking(booking.id)
                        Toast.makeText(requireContext(), "Booking #${booking.id} cancelled successfully", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Keep Booking", null)
                    .show()
            }
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
