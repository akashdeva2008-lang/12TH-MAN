package com.example.twelveman.ui.bookings

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.repository.PlayerRepository
import com.example.twelveman.data.repository.UmpireRepository
import com.example.twelveman.databinding.FragmentBookingFormBinding
import java.util.Calendar

class BookingFormFragment : Fragment() {
    private var _binding: FragmentBookingFormBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingsViewModel by viewModels()

    private var itemId: String = ""
    private var itemType: String = "PLAYER"
    private var itemName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            itemId = it.getString("itemId", "P101")
            itemType = it.getString("itemType", "PLAYER")
        }

        val resolvedName = if (itemType == "UMPIRE") {
            val umpire = UmpireRepository().getUmpireById(itemId)
            if (umpire != null) "${umpire.name} (${umpire.certificationLevel})" else "Suresh Menon (Umpire)"
        } else {
            val player = PlayerRepository().getPlayerById(itemId)
            if (player != null) "${player.name} (${player.role})" else "Virat Sharma (Player)"
        }
        itemName = resolvedName

        binding.tvTargetItemName.text = "Booking: $itemName"

        binding.ivBookingFormBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Date Picker
        val openDatePicker = View.OnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val dateFormatted = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                    binding.etBookingDate.setText(dateFormatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        binding.etBookingDate.setOnClickListener(openDatePicker)

        // Time Slot Picker
        val openTimeSlotPicker = View.OnClickListener {
            val slots = arrayOf(
                "07:00 AM - 11:00 AM (Morning Session)",
                "11:00 AM - 03:00 PM (Afternoon Session)",
                "04:00 PM - 08:00 PM (Evening Floodlights)",
                "08:00 PM - 12:00 AM (Night Floodlights)"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Select Time Slot")
                .setItems(slots) { _, which ->
                    binding.etTimeSlot.setText(slots[which])
                }
                .show()
        }
        binding.etTimeSlot.setOnClickListener(openTimeSlotPicker)

        // Match Type Picker
        val openMatchTypePicker = View.OnClickListener {
            val types = arrayOf("T20 Limited Overs", "50 Overs (ODI)", "Box Cricket", "Corporate Tournament", "Friendly Practice")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Match Type")
                .setItems(types) { _, which ->
                    binding.etMatchType.setText(types[which])
                }
                .show()
        }
        binding.etMatchType.setOnClickListener(openMatchTypePicker)

        // Submit Booking
        binding.btnSubmitBooking.setOnClickListener {
            val date = binding.etBookingDate.text.toString().trim()
            val time = binding.etTimeSlot.text.toString().trim()
            val venue = binding.etVenue.text.toString().trim()
            val matchType = binding.etMatchType.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()

            if (date.isNotBlank() && venue.isNotBlank()) {
                binding.btnSubmitBooking.isEnabled = false
                binding.btnSubmitBooking.text = "Confirming Booking..."

                val amount = if (itemType == "UMPIRE") 3000 else 1800
                viewModel.createBooking(
                    type = itemType,
                    itemId = itemId,
                    itemName = itemName,
                    date = date,
                    time = time,
                    venue = venue,
                    matchType = matchType,
                    amount = amount,
                    notes = notes
                )
                Toast.makeText(requireContext(), "Booking Confirmed Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_booking_form_to_bookings)
            } else {
                Toast.makeText(requireContext(), "Please fill match date and venue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
