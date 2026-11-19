package com.example.twelveman.ui.tournaments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.data.model.Match
import com.example.twelveman.databinding.FragmentMatchScoringBinding
import java.util.Locale

class MatchScoringFragment : Fragment() {
    private var _binding: FragmentMatchScoringBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var matchId: String = ""
    private lateinit var commentaryAdapter: CommentaryAdapter
    private var isTossDialogOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchScoringBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            matchId = it.getString("matchId", "M501")
        }

        commentaryAdapter = CommentaryAdapter(emptyList())
        binding.rvCommentary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentaryAdapter
        }

        viewModel.loadMatchDetails(matchId)

        viewModel.selectedMatch.observe(viewLifecycleOwner) { match ->
            match?.let { m ->
                // Check if toss is recorded; if not, open toss setup dialog
                if (m.tossWinner.isBlank() && !isTossDialogOpen) {
                    showTossSetupDialog(m)
                }

                binding.tvScoringMatchTitle.text = "Match ${m.matchNumber} • ${m.stage} • ${m.format} (${m.totalOvers} ov)"
                binding.tvScoringStatus.text = m.status
                binding.tvScoringTeams.text = "${m.teamA} vs ${m.teamB}"

                val activeScore = if (m.currentInnings == 1) m.scoreA else m.scoreB
                binding.tvCurrentScoreDisplay.text = activeScore

                val battingTeam = if (m.battingTeam.isNotBlank()) m.battingTeam else if (m.currentInnings == 1) m.teamA else m.teamB
                val bowlingTeam = if (m.bowlingTeam.isNotBlank()) m.bowlingTeam else if (m.currentInnings == 1) m.teamB else m.teamA

                if (m.currentInnings == 2 && m.target > 0) {
                    val needed = m.target - m.runsB
                    val totalBalls = m.totalOvers * 6
                    val currentOversInt = m.oversB.toInt()
                    val currentBallsInt = Math.round((m.oversB - currentOversInt) * 10).toInt()
                    val ballsBowled = currentOversInt * 6 + currentBallsInt
                    val remainingBalls = (totalBalls - ballsBowled).coerceAtLeast(0)
                    binding.tvInningsIndicator.text = "Need $needed runs in $remainingBalls balls (Target: ${m.target})"
                } else {
                    binding.tvInningsIndicator.text = "Batting: $battingTeam (Innings ${m.currentInnings})"
                }

                // Striker & Non-Striker
                val sName = if (m.strikerName.isNotBlank()) m.strikerName else "$battingTeam Opener 1"
                val nsName = if (m.nonStrikerName.isNotBlank()) m.nonStrikerName else "$battingTeam Opener 2"
                binding.tvStrikerName.text = "$sName*"
                binding.tvStrikerStats.text = "${m.strikerRuns} (${m.strikerBalls})"
                binding.tvNonStrikerName.text = nsName
                binding.tvNonStrikerStats.text = "${m.nonStrikerRuns} (${m.nonStrikerBalls})"

                // Bowler
                val bName = if (m.bowlerName.isNotBlank()) m.bowlerName else "$bowlingTeam Bowler 1"
                binding.tvBowlerName.text = bName
                binding.tvBowlerStats.text = "${m.bowlerWickets}/${m.bowlerRunsConceded} (${String.format(Locale.US, "%.1f", m.bowlerOvers)})"

                // Partnership & Run Rate
                val currentOversVal = if (m.currentInnings == 1) m.oversA else m.oversB
                val currentRunsVal = if (m.currentInnings == 1) m.runsA else m.runsB
                val crr = if (currentOversVal > 0) {
                    val oversDecimal = currentOversVal.toInt() + ((currentOversVal - currentOversVal.toInt()) * 10 / 6.0)
                    if (oversDecimal > 0) String.format(Locale.US, "%.2f", currentRunsVal / oversDecimal) else "0.00"
                } else "0.00"

                binding.tvPartnershipStats.text = "Partnership: ${m.partnershipRuns} runs (${m.partnershipBalls} b)"
                val totalExtras = m.extrasWide + m.extrasNoBall + m.extrasBye + m.extrasLegBye
                binding.tvRunRateAndExtras.text = "CRR: $crr • Extras: $totalExtras (WD ${m.extrasWide}, NB ${m.extrasNoBall}, B ${m.extrasBye}, LB ${m.extrasLegBye})"

                commentaryAdapter.updateData(m.commentary)
            }
        }

        // Bowling Angle Selector Handlers
        binding.layoutRoundWicket.setOnClickListener { updateBowlingAngle(1) }
        binding.layoutBetweenWicket.setOnClickListener { updateBowlingAngle(2) }
        binding.layoutOverWicket.setOnClickListener { updateBowlingAngle(3) }

        // Top Settings icon
        binding.ivScoringSettings.setOnClickListener {
            val m = viewModel.selectedMatch.value
            val tossInfo = if (m?.tossWinner?.isNotBlank() == true) {
                "${m.tossWinner} won the toss & chose to ${m.tossDecision} first"
            } else "Not yet recorded"

            AlertDialog.Builder(requireContext())
                .setTitle("Match Scoring Settings")
                .setMessage("Match: ${m?.teamA ?: "Team A"} vs ${m?.teamB ?: "Team B"}\nMatch ID: $matchId\n\n• Toss: $tossInfo\n• Format: ${m?.format ?: "T20"} • Overs: ${m?.totalOvers ?: 20}\n• Powerplay: Overs 1 - 6\n• Wide Penalty: +1 Run, Re-bowl\n• No-Ball Penalty: +1 Run, Free Hit\n• Overs per Bowler Limit: ${(m?.totalOvers ?: 20) / 5}.0")
                .setPositiveButton("OK", null)
                .show()
        }

        // Scoring Keypad
        binding.btnRun0.setOnClickListener { viewModel.recordBall(matchId, 0, false, "") }
        binding.btnRun1.setOnClickListener { viewModel.recordBall(matchId, 1, false, "") }
        binding.btnRun2.setOnClickListener { viewModel.recordBall(matchId, 2, false, "") }
        binding.btnRun3.setOnClickListener { viewModel.recordBall(matchId, 3, false, "") }
        binding.btnRun4.setOnClickListener { viewModel.recordBall(matchId, 4, false, "FOUR! (4)") }
        binding.btnRun6.setOnClickListener { viewModel.recordBall(matchId, 6, false, "SIXER! (6)") }

        // Extras
        binding.btnWide.setOnClickListener {
            viewModel.recordBall(matchId, 0, false, "Wide")
            Toast.makeText(requireContext(), "Wide ball recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        binding.btnNoBall.setOnClickListener {
            viewModel.recordBall(matchId, 0, false, "No Ball")
            Toast.makeText(requireContext(), "No Ball recorded (+1) • FREE HIT!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBye.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Bye")
            Toast.makeText(requireContext(), "Bye recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLegBye.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Leg Bye")
            Toast.makeText(requireContext(), "Leg Bye recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        // Wicket Dismissal
        binding.btnWicket.setOnClickListener {
            val dismissalTypes = arrayOf("Bowled", "Caught", "LBW", "Run Out (Striker)", "Run Out (Non-Striker)", "Stumped", "Hit Wicket")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Dismissal Type")
                .setItems(dismissalTypes) { _, which ->
                    val dismissal = dismissalTypes[which]
                    showIncomingBatsmanDialog(dismissal)
                }
                .show()
        }

        // Undo
        binding.btnUndo.setOnClickListener {
            val success = viewModel.undoLastBall(matchId)
            if (success) {
                Toast.makeText(requireContext(), "Last delivery undone successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Nothing to undo in this match", Toast.LENGTH_SHORT).show()
            }
        }

        // Change Strike
        binding.btnChangeStriker.setOnClickListener {
            viewModel.rotateStrike(matchId)
            Toast.makeText(requireContext(), "Strike rotated", Toast.LENGTH_SHORT).show()
        }

        // Retire Batsman
        binding.btnRetireBatsman.setOnClickListener {
            val etNewBatsman = EditText(requireContext()).apply {
                hint = "Enter incoming batsman name"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Retire Striker Batsman")
                .setView(etNewBatsman)
                .setPositiveButton("Retire & Replace") { _, _ ->
                    val newName = etNewBatsman.text.toString().trim()
                    if (newName.isNotBlank()) {
                        viewModel.retireBatsman(matchId, newName)
                        Toast.makeText(requireContext(), "$newName is now on strike", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // End Over
        binding.btnEndOver.setOnClickListener {
            val etNewBowler = EditText(requireContext()).apply {
                hint = "Enter new bowler name"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Conclude Over & Select Bowler")
                .setView(etNewBowler)
                .setPositiveButton("Confirm") { _, _ ->
                    val newBowler = etNewBowler.text.toString().trim()
                    viewModel.endOverManually(matchId)
                    if (newBowler.isNotBlank()) {
                        viewModel.changeBowler(matchId, newBowler)
                    }
                    Toast.makeText(requireContext(), "Over completed. Strike rotated.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Switch Innings
        binding.btnSwitchInnings.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Switch Innings")
                .setMessage("Are you sure you want to conclude the current innings and switch batting teams?")
                .setPositiveButton("Switch Now") { _, _ ->
                    viewModel.switchInnings(matchId)
                    Toast.makeText(requireContext(), "Innings switched successfully", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Conclude Match
        binding.btnCompleteMatch.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Conclude Match")
                .setMessage("Are you sure you want to end this match and declare the final result?")
                .setPositiveButton("Conclude Match") { _, _ ->
                    viewModel.completeMatch(matchId)
                    Toast.makeText(requireContext(), "Match Concluded & Result Declared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.ivScoringBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showTossSetupDialog(match: Match) {
        isTossDialogOpen = true
        val dialogView = layoutInflater.inflate(R.layout.dialog_toss_setup, null)

        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvTossMatchSubtitle)
        val rbTeam1 = dialogView.findViewById<RadioButton>(R.id.rbTossTeam1)
        val rbTeam2 = dialogView.findViewById<RadioButton>(R.id.rbTossTeam2)
        val rbBat = dialogView.findViewById<RadioButton>(R.id.rbDecisionBat)
        val etStriker = dialogView.findViewById<EditText>(R.id.etStrikerName)
        val etNonStriker = dialogView.findViewById<EditText>(R.id.etNonStrikerName)
        val etBowler = dialogView.findViewById<EditText>(R.id.etBowlerName)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmTossStart)

        tvSubtitle.text = "${match.teamA} vs ${match.teamB} • ${match.venue}"
        rbTeam1.text = match.teamA
        rbTeam2.text = match.teamB

        etStriker.setText("${match.teamA} Opener 1")
        etNonStriker.setText("${match.teamA} Opener 2")
        etBowler.setText("${match.teamB} Bowler 1")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnConfirm.setOnClickListener {
            val winner = if (rbTeam1.isChecked) match.teamA else match.teamB
            val decision = if (rbBat.isChecked) "BAT" else "BOWL"
            val batting = if ((rbTeam1.isChecked && rbBat.isChecked) || (!rbTeam1.isChecked && !rbBat.isChecked)) match.teamA else match.teamB
            val bowling = if (batting == match.teamA) match.teamB else match.teamA

            val striker = etStriker.text.toString().trim().ifBlank { "$batting Opener 1" }
            val nonStriker = etNonStriker.text.toString().trim().ifBlank { "$batting Opener 2" }
            val bowler = etBowler.text.toString().trim().ifBlank { "$bowling Bowler 1" }

            viewModel.recordTossAndOpeners(
                matchId = match.id,
                tossWinner = winner,
                tossDecision = decision,
                battingTeam = batting,
                bowlingTeam = bowling,
                striker = striker,
                nonStriker = nonStriker,
                bowler = bowler
            )

            isTossDialogOpen = false
            dialog.dismiss()
            Toast.makeText(requireContext(), "Toss confirmed! $winner elected to $decision first.", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun showIncomingBatsmanDialog(dismissal: String) {
        val etNewBatsman = EditText(requireContext()).apply {
            hint = "Enter incoming batsman name"
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Wicket: $dismissal")
            .setMessage("Select / Enter incoming batsman:")
            .setView(etNewBatsman)
            .setPositiveButton("Next Batsman In") { _, _ ->
                val nextName = etNewBatsman.text.toString().trim().ifBlank { "New Batsman" }
                viewModel.recordBall(matchId, 0, true, "Wicket ($dismissal)")
                val currentMatch = viewModel.selectedMatch.value
                currentMatch?.let {
                    it.strikerName = nextName
                    it.strikerRuns = 0
                    it.strikerBalls = 0
                }
                Toast.makeText(requireContext(), "Wicket recorded. $nextName is on strike.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateBowlingAngle(angleOption: Int) {
        val red = ContextCompat.getColor(requireContext(), R.color.primary_red)
        val black = ContextCompat.getColor(requireContext(), R.color.header_black)
        val secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset all
        binding.ivRoundWicket.setBackgroundResource(R.drawable.bg_circle_light)
        binding.ivRoundWicket.setColorFilter(black)
        binding.tvRoundWicket.setTextColor(secondary)

        binding.ivBetweenWicket.setBackgroundResource(R.drawable.bg_circle_light)
        binding.ivBetweenWicket.setColorFilter(black)
        binding.tvBetweenWicket.setTextColor(secondary)

        binding.ivOverWicket.setBackgroundResource(R.drawable.bg_circle_light)
        binding.ivOverWicket.setColorFilter(black)
        binding.tvOverWicket.setTextColor(secondary)

        when (angleOption) {
            1 -> {
                binding.ivRoundWicket.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.ivRoundWicket.setColorFilter(red)
                binding.tvRoundWicket.setTextColor(red)
                Toast.makeText(requireContext(), "Bowling angle: Round the Wicket", Toast.LENGTH_SHORT).show()
            }
            2 -> {
                binding.ivBetweenWicket.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.ivBetweenWicket.setColorFilter(red)
                binding.tvBetweenWicket.setTextColor(red)
                Toast.makeText(requireContext(), "Bowling angle: Between the Wicket", Toast.LENGTH_SHORT).show()
            }
            3 -> {
                binding.ivOverWicket.setBackgroundResource(R.drawable.bg_badge_red_outline)
                binding.ivOverWicket.setColorFilter(red)
                binding.tvOverWicket.setTextColor(red)
                Toast.makeText(requireContext(), "Bowling angle: Over the Wicket", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
