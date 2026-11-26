package com.example.twelveman.ui.umpires

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.data.model.Booking
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.databinding.FragmentUmpireDashboardBinding

class UmpireDashboardFragment : Fragment() {

    private var _binding: FragmentUmpireDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UmpiresViewModel by viewModels()

    private lateinit var umpireAdapter: UmpireDashboardAdapter
    private lateinit var bookingAdapter: UmpireBookingAdapter

    private var activeTab: Int = 1 // 1: Browse Umpires, 2: My Bookings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUmpireDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupTabs()
        setupFilterChips()
        setupSearch()
        setupListeners()
        observeViewModel()
    }

    private fun setupAdapters() {
        umpireAdapter = UmpireDashboardAdapter(
            umpires = emptyList(),
            onBookClick = { umpire -> showBookUmpireDialog(umpire) },
            onProfileClick = { umpire ->
                val bundle = Bundle().apply { putString("umpireId", umpire.id) }
                findNavController().navigate(R.id.action_umpire_dashboard_to_details, bundle)
            }
        )
        binding.rvDashboardUmpires.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = umpireAdapter
        }

        bookingAdapter = UmpireBookingAdapter(
            bookings = emptyList(),
            onContactClick = { booking -> contactUmpire(booking) },
            onCancelClick = { booking -> confirmCancelBooking(booking) }
        )
        binding.rvMyUmpireBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupTabs() {
        binding.tabBrowseUmpires.setOnClickListener { switchTab(1) }
        binding.tabMyUmpireBookings.setOnClickListener { switchTab(2) }
        binding.btnBookUmpireEmptyCta.setOnClickListener { switchTab(1) }
    }

    private fun switchTab(tabIndex: Int) {
        activeTab = tabIndex
        if (tabIndex == 1) {
            binding.tabBrowseUmpires.setBackgroundResource(R.drawable.bg_tab_selected_indicator)
            binding.tabBrowseUmpires.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
            binding.tabMyUmpireBookings.background = null
            binding.tabMyUmpireBookings.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            binding.llBrowseUmpiresSection.visibility = View.VISIBLE
            binding.llMyBookingsSection.visibility = View.GONE
        } else {
            binding.tabMyUmpireBookings.setBackgroundResource(R.drawable.bg_tab_selected_indicator)
            binding.tabMyUmpireBookings.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
            binding.tabBrowseUmpires.background = null
            binding.tabBrowseUmpires.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))

            binding.llBrowseUmpiresSection.visibility = View.GONE
            binding.llMyBookingsSection.visibility = View.VISIBLE
        }
    }

    private fun setupFilterChips() {
        fun clearChipSelections() {
            binding.chipFilterAll.isChecked = false
            binding.chipFilterICC.isChecked = false
            binding.chipFilterNational.isChecked = false
            binding.chipFilterState.isChecked = false
            binding.chipFilterAffordable.isChecked = false
        }

        binding.chipFilterAll.setOnClickListener {
            clearChipSelections()
            binding.chipFilterAll.isChecked = true
            viewModel.filterByCertification("ALL")
        }

        binding.chipFilterICC.setOnClickListener {
            clearChipSelections()
            binding.chipFilterICC.isChecked = true
            viewModel.filterByCertification("ICC ACCREDITED")
        }

        binding.chipFilterNational.setOnClickListener {
            clearChipSelections()
            binding.chipFilterNational.isChecked = true
            viewModel.filterByCertification("NATIONAL LEVEL")
        }

        binding.chipFilterState.setOnClickListener {
            clearChipSelections()
            binding.chipFilterState.isChecked = true
            viewModel.filterByCertification("STATE LEVEL")
        }

        binding.chipFilterAffordable.setOnClickListener {
            clearChipSelections()
            binding.chipFilterAffordable.isChecked = true
            viewModel.filterByCertification("UNDER ₹2,000")
        }
    }

    private fun setupSearch() {
        binding.etSearchDashboardUmpires.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.fetchUmpires(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.ivUmpireDashboardBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivRefreshUmpireDashboard.setOnClickListener {
            viewModel.startRealtimeSync()
            viewModel.loadMyUmpireBookings()
            Toast.makeText(requireContext(), "Refreshing Umpire availability...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.umpiresList.observe(viewLifecycleOwner) { umpires ->
            umpireAdapter.updateList(umpires)
            val available = umpires.count { it.availabilityStatus.equals("Available", ignoreCase = true) }
            binding.tvStatAvailableCount.text = available.toString()

            if (umpires.isEmpty()) {
                binding.llEmptyUmpiresNotice.visibility = View.VISIBLE
                binding.rvDashboardUmpires.visibility = View.GONE
            } else {
                binding.llEmptyUmpiresNotice.visibility = View.GONE
                binding.rvDashboardUmpires.visibility = View.VISIBLE
            }
        }

        viewModel.myUmpireBookings.observe(viewLifecycleOwner) { bookings ->
            bookingAdapter.updateList(bookings)
            binding.tvStatActiveBookings.text = bookings.count {
                it.status.equals("CONFIRMED", ignoreCase = true) || it.status.equals("PENDING", ignoreCase = true)
            }.toString()
            binding.tabMyUmpireBookings.text = "My Bookings (${bookings.size})"

            if (bookings.isEmpty()) {
                binding.llEmptyBookingsState.visibility = View.VISIBLE
                binding.rvMyUmpireBookings.visibility = View.GONE
            } else {
                binding.llEmptyBookingsState.visibility = View.GONE
                binding.rvMyUmpireBookings.visibility = View.VISIBLE
            }
        }

        viewModel.bookingActionSuccess.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearActionMessage()
            }
        }
    }

    private fun showBookUmpireDialog(umpire: Umpire) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_book_umpire, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvName = dialogView.findViewById<TextView>(R.id.tvPreviewUmpireName)
        val tvDetails = dialogView.findViewById<TextView>(R.id.tvPreviewUmpireDetails)
        val tvFee = dialogView.findViewById<TextView>(R.id.tvPreviewUmpireFee)
        val tvSummaryFee = dialogView.findViewById<TextView>(R.id.tvSummaryMatchFee)
        val tvSummaryTotal = dialogView.findViewById<TextView>(R.id.tvSummaryTotalPayable)

        val etMatchTitle = dialogView.findViewById<EditText>(R.id.etMatchTitle)
        val etDate = dialogView.findViewById<EditText>(R.id.etBookingDate)
        val etTimeSlot = dialogView.findViewById<EditText>(R.id.etBookingTimeSlot)
        val etVenue = dialogView.findViewById<EditText>(R.id.etBookingVenue)
        val etPhone = dialogView.findViewById<EditText>(R.id.etBookingPhone)
        val etNotes = dialogView.findViewById<EditText>(R.id.etBookingNotes)

        val btnFormatT20 = dialogView.findViewById<Button>(R.id.btnFormatT20)
        val btnFormatT10 = dialogView.findViewById<Button>(R.id.btnFormatT10)
        val btnFormatODI = dialogView.findViewById<Button>(R.id.btnFormatODI)
        val btnFormatBox = dialogView.findViewById<Button>(R.id.btnFormatBox)

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmUmpireBooking)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelUmpireDialog)
        val ivClose = dialogView.findViewById<ImageView>(R.id.ivCloseBookUmpireDialog)

        // Populate umpire preview
        tvName.text = umpire.name
        tvDetails.text = "${umpire.certificationLevel} • ${umpire.experienceYears} Yrs • ${umpire.rating} ★"
        tvFee.text = "₹${umpire.pricePerMatch}"
        tvSummaryFee.text = "₹${umpire.pricePerMatch}"
        tvSummaryTotal.text = "₹${umpire.pricePerMatch}"

        var selectedFormat = "T20"
        fun updateFormatButtons(active: Button) {
            listOf(btnFormatT20, btnFormatT10, btnFormatODI, btnFormatBox).forEach { b ->
                if (b == active) {
                    b.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
                } else {
                    b.setTextColor(ContextCompat.getColor(requireContext(), R.color.header_black))
                }
            }
        }

        btnFormatT20.setOnClickListener { selectedFormat = "T20"; updateFormatButtons(btnFormatT20) }
        btnFormatT10.setOnClickListener { selectedFormat = "T10"; updateFormatButtons(btnFormatT10) }
        btnFormatODI.setOnClickListener { selectedFormat = "50-Over"; updateFormatButtons(btnFormatODI) }
        btnFormatBox.setOnClickListener { selectedFormat = "Box Cricket"; updateFormatButtons(btnFormatBox) }

        ivClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val title = etMatchTitle.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTimeSlot.text.toString().trim()
            val venue = etVenue.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val notes = etNotes.text.toString().trim()

            if (title.isBlank() || date.isBlank() || venue.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in Match Title, Date, and Venue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.bookUmpire(
                umpire = umpire,
                matchTitle = title,
                date = date,
                timeSlot = time,
                venue = venue,
                matchType = selectedFormat,
                notes = notes,
                contactPhone = phone
            )

            dialog.dismiss()
            switchTab(2) // Switch to My Bookings tab immediately
        }

        dialog.show()
    }

    private fun contactUmpire(booking: Booking) {
        val phoneNumber = if (booking.notes.contains("Contact:")) {
            booking.notes.substringAfter("Contact:").substringBefore(";").trim()
        } else {
            "+919820011223"
        }

        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Calling $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmCancelBooking(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Umpire Booking")
            .setMessage("Are you sure you want to cancel the booking for ${booking.playerName} on ${booking.date}?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                viewModel.cancelUmpireBooking(booking.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
