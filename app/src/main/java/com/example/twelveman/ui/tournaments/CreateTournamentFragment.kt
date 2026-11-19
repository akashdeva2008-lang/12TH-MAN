package com.example.twelveman.ui.tournaments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.model.Match
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
            val overs = when {
                format.contains("10", ignoreCase = true) -> 10
                format.contains("100", ignoreCase = true) -> 16
                format.contains("ODI", ignoreCase = true) -> 50
                format.contains("Test", ignoreCase = true) -> 90
                else -> 20
            }
            val entryFee = binding.etEntryFee.text.toString().toIntOrNull() ?: 10000
            val prize = binding.etPrizeMoney.text.toString().toIntOrNull() ?: 100000

            if (location.isNotBlank()) {
                val team1 = binding.tvTeam1Name.text.toString().ifBlank { "Red Warriors" }
                val team2 = binding.tvTeam2Name.text.toString().ifBlank { "Blue Strikers" }
                val dateTimeParts = binding.tvDateTimeValue.text.toString().split("•").map { it.trim() }
                val matchDate = if (dateTimeParts.isNotEmpty()) dateTimeParts[0] else "Today"
                val matchTime = if (dateTimeParts.size > 1) dateTimeParts[1] else "5:00 PM"
                val ballType = binding.tvBallTypeValue.text.toString().removePrefix("Ball Type:").trim().ifBlank { "Leather Ball" }

                // Create both Tournament and Match in repository
                val match = viewModel.scheduleMatch(
                    teamA = team1,
                    teamB = team2,
                    format = format.ifBlank { "T20" },
                    overs = overs,
                    venue = location,
                    date = matchDate,
                    time = matchTime,
                    ballType = ballType
                )

                viewModel.createTournament(
                    name = "$team1 vs $team2 • ${name.ifBlank { "Challenge Trophy" }}",
                    format = format.ifBlank { "T20" },
                    location = location,
                    startDate = matchDate,
                    endDate = matchDate,
                    totalTeams = 2,
                    entryFee = entryFee,
                    prizeMoney = prize,
                    organizerName = "12THMAN Official",
                    organizerContact = "+91 98200 11223",
                    description = "Official match scheduled between $team1 and $team2."
                )

                // Show professional confirmation dialog with VIEW MATCH and START SCORING options
                showScheduleSuccessDialog(match)
            } else {
                Toast.makeText(requireContext(), "Please enter venue location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showScheduleSuccessDialog(match: Match) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_match_scheduled_success, null)

        val tvTeams = dialogView.findViewById<TextView>(R.id.tvSuccessTeams)
        val tvDateTime = dialogView.findViewById<TextView>(R.id.tvSuccessDateTime)
        val tvVenue = dialogView.findViewById<TextView>(R.id.tvSuccessVenue)
        val tvFormatOvers = dialogView.findViewById<TextView>(R.id.tvSuccessFormatOvers)
        val tvBallType = dialogView.findViewById<TextView>(R.id.tvSuccessBallType)
        val btnViewMatch = dialogView.findViewById<Button>(R.id.btnSuccessViewMatch)
        val btnStartScoring = dialogView.findViewById<Button>(R.id.btnSuccessStartScoring)

        tvTeams.text = "${match.teamA} vs ${match.teamB}"
        tvDateTime.text = "${match.date} • ${match.time}"
        tvVenue.text = match.venue
        tvFormatOvers.text = "${match.format} • ${match.totalOvers} Overs"
        tvBallType.text = match.ballType

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnViewMatch.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("matchId", match.id) }
            findNavController().navigate(R.id.action_create_to_match_details, bundle)
        }

        btnStartScoring.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("matchId", match.id) }
            findNavController().navigate(R.id.action_create_to_scoring, bundle)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
