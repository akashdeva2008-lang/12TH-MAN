package com.example.twelveman.ui.tournaments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.twelveman.databinding.FragmentCreateTournamentBinding
import java.util.Calendar

class CreateTournamentFragment : Fragment() {
    private var _binding: FragmentCreateTournamentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTournamentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivCreateMatchBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Team 1 Selection Dialog
        binding.rowTeam1Picker.setOnClickListener {
            val teams = arrayOf("Red Warriors", "Mumbai Superstars", "Chennai Kings", "Delhi Titans", "Ahmedabad Strikers")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Batting / Home Team")
                .setItems(teams) { _, which ->
                    val selected = teams[which]
                    binding.tvTeam1Name.text = selected
                    Toast.makeText(requireContext(), "Team 1: $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Team 2 Selection Dialog
        binding.rowTeam2Picker.setOnClickListener {
            val teams = arrayOf("Blue Strikers", "Bangalore Royals", "Kolkata Knights", "Gujarat Lions", "Pune Warriors")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Bowling / Away Team")
                .setItems(teams) { _, which ->
                    val selected = teams[which]
                    binding.tvTeam2Name.text = selected
                    Toast.makeText(requireContext(), "Team 2: $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Date & Time Picker
        binding.rowDateTimeSelector.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            val amPm = if (hourOfDay >= 12) "PM" else "AM"
                            val hourFormatted = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                            val timeStr = String.format("%02d:%02d %s", hourFormatted, minute, amPm)
                            val dateStr = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                            binding.tvDateTimeValue.text = "$dateStr • $timeStr"
                            Toast.makeText(requireContext(), "Scheduled: $dateStr at $timeStr", Toast.LENGTH_SHORT).show()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Ball Type Selector
        binding.rowBallTypeSelector.setOnClickListener {
            val ballTypes = arrayOf("Leather Ball (White)", "Leather Ball (Red)", "Tennis Ball (Heavy)", "Tennis Ball (Light)", "Other")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Ball Type")
                .setItems(ballTypes) { _, which ->
                    val selected = ballTypes[which]
                    binding.tvBallTypeValue.text = "Ball Type: $selected"
                    Toast.makeText(requireContext(), "Ball Type set to $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        // Primary Schedule Match Button
        binding.btnPublishTournament.setOnClickListener {
            val name = binding.etTournamentName.text.toString().trim()
            val format = binding.etFormat.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val entryFee = binding.etEntryFee.text.toString().toIntOrNull() ?: 10000
            val prize = binding.etPrizeMoney.text.toString().toIntOrNull() ?: 100000

            if (name.isNotBlank() && location.isNotBlank()) {
                // Prevent duplicate click
                binding.btnPublishTournament.isEnabled = false

                val team1 = binding.tvTeam1Name.text.toString()
                val team2 = binding.tvTeam2Name.text.toString()
                val dateTime = binding.tvDateTimeValue.text.toString()

                viewModel.createTournament(
                    name = "$team1 vs $team2 • $name",
                    format = format,
                    location = location,
                    startDate = dateTime,
                    endDate = dateTime,
                    totalTeams = 2,
                    entryFee = entryFee,
                    prizeMoney = prize,
                    organizerName = "12THMAN Official",
                    organizerContact = "+91 98200 11223",
                    description = "Official match scheduled between $team1 and $team2."
                )

                Toast.makeText(requireContext(), "Match Scheduled Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_create_to_tournaments)
            } else {
                Toast.makeText(requireContext(), "Please enter match name and venue location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
