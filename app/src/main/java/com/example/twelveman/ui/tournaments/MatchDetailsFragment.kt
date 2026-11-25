package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentMatchDetailsBinding

class MatchDetailsFragment : Fragment() {
    private var _binding: FragmentMatchDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var matchId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            matchId = it.getString("matchId", "M503")
        }

        viewModel.loadMatchDetails(matchId)
        viewModel.selectedMatch.observe(viewLifecycleOwner) { m ->
            m?.let {
                binding.tvMatchDetailStage.text = "Match ${it.matchNumber} • ${it.stage}"
                binding.tvMatchDetailTeamA.text = it.teamA
                binding.tvMatchDetailTeamB.text = it.teamB
                binding.tvMatchDetailResult.text = if (it.result.isNotBlank()) it.result else "Scheduled"
                binding.tvScoreA.text = if (it.scoreA.isNotBlank()) it.scoreA else "N/A"
                binding.tvScoreB.text = if (it.scoreB.isNotBlank()) it.scoreB else "N/A"
                binding.tvMatchVenueInfo.text = "Venue: ${it.venue}\nDate: ${it.date} • ${it.time}"
            }
        }

        // Edit match action
        val editAction = View.OnClickListener {
            val currentMatch = viewModel.selectedMatch.value
            val options = arrayOf("Edit Match Venue & Timing", "Delete Match")
            AlertDialog.Builder(requireContext())
                .setTitle("Match Actions • $matchId")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showEditMatchDialog(currentMatch?.venue ?: "", currentMatch?.date ?: "", currentMatch?.time ?: "")
                        1 -> showDeleteConfirmationDialog()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.ivMatchDetailEdit.setOnClickListener(editAction)
        binding.btnEditDetails.setOnClickListener(editAction)

        // Start Scoring Button
        binding.btnOpenScoring.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", matchId) }
            findNavController().navigate(R.id.action_match_details_to_scoring, bundle)
        }

        binding.ivMatchDetailBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Toss info card
        binding.cardToss.setOnClickListener {
            val m = viewModel.selectedMatch.value
            val teamA = m?.teamA ?: "Team A"
            val tossMsg = if (!m?.tossWinner.isNullOrBlank()) {
                "${m?.tossWinner} won the coin toss and elected to ${m?.tossDecision?.lowercase() ?: "bat"} first."
            } else {
                "Coin toss not yet recorded for this fixture."
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Toss Decision")
                .setMessage("Toss Result:\n\n$tossMsg\nMatch Venue: ${m?.venue ?: "Scheduled Ground"}")
                .setPositiveButton("OK", null)
                .show()
        }

        // Squad info card
        binding.cardSquad.setOnClickListener {
            val m = viewModel.selectedMatch.value
            val teamA = m?.teamA ?: "Team A"
            val teamB = m?.teamB ?: "Team B"
            AlertDialog.Builder(requireContext())
                .setTitle("Match Squads")
                .setMessage(
                    "Playing Squads:\n\n" +
                    "• $teamA: Playing XI registered\n" +
                    "• $teamB: Playing XI registered\n\n" +
                    "Format: ${m?.format ?: "T20"} • Ball: ${m?.ballType ?: "Leather"}"
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun showEditMatchDialog(currentVenue: String, currentDate: String, currentTime: String) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etVenue = EditText(context).apply {
            hint = "Venue Name"
            setText(currentVenue)
        }
        val etDate = EditText(context).apply {
            hint = "Date (e.g. Apr 26, 2025)"
            setText(currentDate)
        }
        val etTime = EditText(context).apply {
            hint = "Time (e.g. 5:00 PM)"
            setText(currentTime)
        }

        layout.addView(etVenue)
        layout.addView(etDate)
        layout.addView(etTime)

        AlertDialog.Builder(context)
            .setTitle("Edit Match Information")
            .setView(layout)
            .setPositiveButton("Save Changes") { _, _ ->
                val newVenue = etVenue.text.toString().trim()
                val newDate = etDate.text.toString().trim()
                val newTime = etTime.text.toString().trim()
                if (newVenue.isNotBlank()) {
                    viewModel.updateMatchDetails(matchId, newVenue, newDate, newTime)
                    Toast.makeText(context, "Match details updated successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Match Confirmation")
            .setMessage("Are you sure you want to delete this match ($matchId)? This operation cannot be undone.")
            .setPositiveButton("Delete Permanently") { _, _ ->
                val deleted = viewModel.deleteMatch(matchId)
                if (deleted) {
                    Toast.makeText(requireContext(), "Match $matchId deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Unable to delete match", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
