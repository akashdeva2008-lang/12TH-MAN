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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.model.Match
import com.example.twelveman.databinding.FragmentBookingsBinding
import com.example.twelveman.ui.tournaments.MatchAdapter

class BookingsFragment : Fragment() {
    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingsViewModel by viewModels()

    private lateinit var bookingAdapter: BookingAdapter
    private lateinit var matchAdapter: MatchAdapter

    private var allBookings: List<Booking> = emptyList()
    private var allMatches: List<Match> = emptyList()

    private var activeMainCategory: String = "BOOKINGS" // "BOOKINGS" or "MATCHES"
    private var activeStatusTab: String = "UPCOMING"     // "UPCOMING", "LIVE", "COMPLETED"
    private var activeBookingTypeFilter: String = "ALL"  // "ALL", "PLAYERS", "UMPIRES"

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

        // Read navigation bundle arguments if passed (e.g. from Home "View All" clicks)
        arguments?.getString("targetCategory")?.let {
            if (it.isNotEmpty()) activeMainCategory = it
            arguments?.remove("targetCategory")
        }
        arguments?.getString("targetTab")?.let {
            if (it.isNotEmpty()) activeStatusTab = it
            arguments?.remove("targetTab")
        }

        setupAdapters()
        setupCategoryToggle()
        setupStatusTabs()
        setupSubFilters()
        setupSearchAndFilter()

        // Sync initial UI state with selected category & status tab
        updateCategoryUI()
        updateStatusTabsUI()
        updateSubFiltersUI()

        // Preload cached data if available and apply initial filters
        viewModel.matches.value?.let { allMatches = it.distinctBy { m -> m.id } }
        viewModel.bookings.value?.let { allBookings = it.distinctBy { b -> b.id } }
        applyCurrentFilters()

        observeViewModel()
    }

    private fun setupAdapters() {
        // 1. Booking Adapter
        bookingAdapter = BookingAdapter(emptyList()) { booking ->
            showBookingDetailsDialog(booking)
        }
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }

        // 2. Match Adapter
        matchAdapter = MatchAdapter(
            matches = emptyList(),
            onItemClick = { match ->
                val bundle = Bundle().apply { putString("matchId", match.id) }
                findNavController().navigate(R.id.action_bookings_to_match_details, bundle)
            },
            onStartScoring = { match ->
                val bundle = Bundle().apply { putString("matchId", match.id) }
                findNavController().navigate(R.id.action_bookings_to_scoring, bundle)
            }
        )
        binding.rvMatches.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = matchAdapter
        }
    }

    private fun setupCategoryToggle() {
        binding.btnSelectBookings.setOnClickListener {
            activeMainCategory = "BOOKINGS"
            updateCategoryUI()
            applyCurrentFilters()
        }

        binding.btnSelectMatches.setOnClickListener {
            activeMainCategory = "MATCHES"
            updateCategoryUI()
            applyCurrentFilters()
        }
    }

    private fun updateCategoryUI() {
        val white = ContextCompat.getColor(requireContext(), R.color.pure_white)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.header_dark_grey)

        if (activeMainCategory == "BOOKINGS") {
            binding.btnSelectBookings.setBackgroundResource(R.drawable.bg_badge_pill_active)
            binding.btnSelectBookings.setTextColor(white)
            binding.btnSelectMatches.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
            binding.btnSelectMatches.setTextColor(textSecondary)

            binding.layoutBookingSubFilters.visibility = View.VISIBLE
            binding.rvBookings.visibility = View.VISIBLE
            binding.rvMatches.visibility = View.GONE
        } else {
            binding.btnSelectMatches.setBackgroundResource(R.drawable.bg_badge_pill_active)
            binding.btnSelectMatches.setTextColor(white)
            binding.btnSelectBookings.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
            binding.btnSelectBookings.setTextColor(textSecondary)

            binding.layoutBookingSubFilters.visibility = View.GONE
            binding.rvBookings.visibility = View.GONE
            binding.rvMatches.visibility = View.VISIBLE
        }
    }

    private fun setupStatusTabs() {
        binding.tabBookingUpcoming.setOnClickListener {
            activeStatusTab = "UPCOMING"
            updateStatusTabsUI()
            applyCurrentFilters()
        }

        binding.tabBookingLive.setOnClickListener {
            activeStatusTab = "LIVE"
            updateStatusTabsUI()
            applyCurrentFilters()
        }

        binding.tabBookingCompleted.setOnClickListener {
            activeStatusTab = "COMPLETED"
            updateStatusTabsUI()
            applyCurrentFilters()
        }
    }

    private fun updateStatusTabsUI() {
        val white = ContextCompat.getColor(requireContext(), R.color.pure_white)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.header_dark_grey)

        binding.tabBookingUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingUpcoming.setTextColor(textSecondary)
        binding.tabBookingLive.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingLive.setTextColor(textSecondary)
        binding.tabBookingCompleted.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabBookingCompleted.setTextColor(textSecondary)

        when (activeStatusTab) {
            "UPCOMING" -> {
                binding.tabBookingUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingUpcoming.setTextColor(white)
            }
            "LIVE" -> {
                binding.tabBookingLive.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingLive.setTextColor(white)
            }
            "COMPLETED" -> {
                binding.tabBookingCompleted.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabBookingCompleted.setTextColor(white)
            }
        }
    }

    private fun setupSubFilters() {
        binding.chipAllBookings.setOnClickListener {
            activeBookingTypeFilter = "ALL"
            updateSubFiltersUI()
            applyCurrentFilters()
        }

        binding.chipPlayerBookings.setOnClickListener {
            activeBookingTypeFilter = "PLAYERS"
            updateSubFiltersUI()
            applyCurrentFilters()
        }

        binding.chipUmpireBookings.setOnClickListener {
            activeBookingTypeFilter = "UMPIRES"
            updateSubFiltersUI()
            applyCurrentFilters()
        }
    }

    private fun updateSubFiltersUI() {
        val red = ContextCompat.getColor(requireContext(), R.color.primary_red)
        val secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        binding.chipAllBookings.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.chipAllBookings.setTextColor(secondary)
        binding.chipPlayerBookings.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.chipPlayerBookings.setTextColor(secondary)
        binding.chipUmpireBookings.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.chipUmpireBookings.setTextColor(secondary)

        when (activeBookingTypeFilter) {
            "ALL" -> {
                binding.chipAllBookings.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.chipAllBookings.setTextColor(red)
            }
            "PLAYERS" -> {
                binding.chipPlayerBookings.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.chipPlayerBookings.setTextColor(red)
            }
            "UMPIRES" -> {
                binding.chipUmpireBookings.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.chipUmpireBookings.setTextColor(red)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.bookings.observe(viewLifecycleOwner) { list ->
            allBookings = list.distinctBy { it.id }
            if (activeMainCategory == "BOOKINGS") {
                applyCurrentFilters()
            }
        }

        viewModel.matches.observe(viewLifecycleOwner) { list ->
            allMatches = list.distinctBy { it.id }
            if (activeMainCategory == "MATCHES") {
                applyCurrentFilters()
            }
        }
    }

    private fun applyCurrentFilters() {
        if (activeMainCategory == "BOOKINGS") {
            var filtered = allBookings

            // 1. Filter by Booking Type
            filtered = when (activeBookingTypeFilter) {
                "PLAYERS" -> filtered.filter { it.isPlayerBooking }
                "UMPIRES" -> filtered.filter { !it.isPlayerBooking }
                else -> filtered
            }

            // 2. Filter by Status Tab
            filtered = when (activeStatusTab) {
                "UPCOMING" -> filtered.filter {
                    it.status.equals("CONFIRMED", ignoreCase = true) ||
                    it.status.equals("PENDING", ignoreCase = true) ||
                    it.status.equals("UPCOMING", ignoreCase = true)
                }
                "LIVE" -> filtered.filter {
                    it.status.equals("LIVE", ignoreCase = true) ||
                    it.status.equals("IN PROGRESS", ignoreCase = true)
                }
                "COMPLETED" -> filtered.filter {
                    it.status.equals("COMPLETED", ignoreCase = true) ||
                    it.status.equals("CANCELLED", ignoreCase = true)
                }
                else -> filtered
            }

            bookingAdapter.updateData(filtered)
            toggleEmptyState(
                isEmpty = filtered.isEmpty(),
                title = "No ${activeStatusTab.lowercase().replaceFirstChar { it.uppercase() }} Bookings",
                message = "You have no ${activeStatusTab.lowercase()} bookings matching this filter."
            )
        } else {
            // MATCHES Mode
            val filtered = when (activeStatusTab) {
                "UPCOMING" -> allMatches.filter { it.status.equals("UPCOMING", ignoreCase = true) }
                "LIVE" -> allMatches.filter { it.status.equals("LIVE", ignoreCase = true) }
                "COMPLETED" -> allMatches.filter { it.status.equals("COMPLETED", ignoreCase = true) }
                else -> allMatches
            }

            matchAdapter.updateData(filtered)
            toggleEmptyState(
                isEmpty = filtered.isEmpty(),
                title = "No ${activeStatusTab.lowercase().replaceFirstChar { it.uppercase() }} Matches",
                message = "There are currently no ${activeStatusTab.lowercase()} matches scheduled."
            )
        }
    }

    private fun toggleEmptyState(isEmpty: Boolean, title: String, message: String) {
        if (isEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = title
            binding.tvEmptyMessage.text = message
        } else {
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun setupSearchAndFilter() {
        binding.ivBookingSearch.setOnClickListener {
            val etSearch = EditText(requireContext()).apply {
                hint = if (activeMainCategory == "BOOKINGS") "Search player, umpire, or venue" else "Search team or venue"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Search ${if (activeMainCategory == "BOOKINGS") "Bookings" else "Matches"}")
                .setView(etSearch)
                .setPositiveButton("Search") { _, _ ->
                    val query = etSearch.text.toString().trim()
                    if (activeMainCategory == "BOOKINGS") {
                        val result = allBookings.filter {
                            it.playerName.contains(query, ignoreCase = true) ||
                            it.playerRole.contains(query, ignoreCase = true) ||
                            it.venue.contains(query, ignoreCase = true) ||
                            it.id.contains(query, ignoreCase = true)
                        }
                        bookingAdapter.updateData(result)
                        toggleEmptyState(result.isEmpty(), "No Results", "No bookings match '$query'")
                    } else {
                        val result = allMatches.filter {
                            it.teamA.contains(query, ignoreCase = true) ||
                            it.teamB.contains(query, ignoreCase = true) ||
                            it.venue.contains(query, ignoreCase = true)
                        }
                        matchAdapter.updateData(result)
                        toggleEmptyState(result.isEmpty(), "No Results", "No matches match '$query'")
                    }
                }
                .setNegativeButton("Reset") { _, _ ->
                    applyCurrentFilters()
                }
                .show()
        }

        binding.ivBookingFilter.setOnClickListener {
            if (activeMainCategory == "BOOKINGS") {
                val types = arrayOf("All Bookings", "Players Only", "Umpires Only")
                AlertDialog.Builder(requireContext())
                    .setTitle("Filter Bookings")
                    .setItems(types) { _, which ->
                        when (which) {
                            0 -> activeBookingTypeFilter = "ALL"
                            1 -> activeBookingTypeFilter = "PLAYERS"
                            2 -> activeBookingTypeFilter = "UMPIRES"
                        }
                        updateSubFiltersUI()
                        applyCurrentFilters()
                    }
                    .show()
            } else {
                val statuses = arrayOf("All Statuses", "Upcoming", "Live", "Completed")
                AlertDialog.Builder(requireContext())
                    .setTitle("Filter Matches")
                    .setItems(statuses) { _, which ->
                        when (which) {
                            1 -> activeStatusTab = "UPCOMING"
                            2 -> activeStatusTab = "LIVE"
                            3 -> activeStatusTab = "COMPLETED"
                        }
                        updateStatusTabsUI()
                        applyCurrentFilters()
                    }
                    .show()
            }
        }
    }

    private fun showBookingDetailsDialog(booking: Booking) {
        val isCancellable = !booking.status.equals("CANCELLED", ignoreCase = true) &&
                            !booking.status.equals("COMPLETED", ignoreCase = true)

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Booking #${booking.id}")
            .setMessage(
                "Category: ${if (booking.isPlayerBooking) "Player Booking" else "Umpire Booking"}\n" +
                "Name: ${booking.playerName}\n" +
                "Role / Spec: ${booking.playerRole}\n" +
                "Date: ${booking.date}\n" +
                "Time Range: ${booking.timeSlot}\n" +
                "Venue: ${booking.venue}\n" +
                "Match Format: ${booking.matchType}\n" +
                "Amount: ₹${booking.amount}\n" +
                "Payment Status: ${booking.paymentStatus}\n" +
                "Confirmation Status: ${booking.status}\n" +
                (if (booking.notes.isNotBlank()) "Notes: ${booking.notes}\n" else "")
            )
            .setPositiveButton("OK", null)

        if (isCancellable) {
            builder.setNegativeButton("Cancel Booking") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Cancel Booking Confirmation")
                    .setMessage("Are you sure you want to cancel booking #${booking.id} (${booking.playerName})?")
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
