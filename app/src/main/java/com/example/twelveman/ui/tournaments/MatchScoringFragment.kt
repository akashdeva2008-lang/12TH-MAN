package com.example.twelveman.ui.tournaments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.InningsScorecard
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.FragmentMatchScoringBinding
import com.example.twelveman.databinding.ItemScorecardBattingRowBinding
import com.example.twelveman.databinding.ItemScorecardBowlingRowBinding
import java.util.Locale
import java.util.UUID

class MatchScoringFragment : Fragment() {
    private var _binding: FragmentMatchScoringBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var matchId: String = ""
    private lateinit var commentaryAdapter: CommentaryAdapter
    private var isTossDialogOpen = false
    private var lastPromptedOverNumber = -1

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

                // 1. Header & Teams
                binding.tvScoringTeams.text = "${m.teamA} vs ${m.teamB}"
                binding.tvScoringStatus.text = m.status
                val tossText = if (m.tossWinner.isNotBlank()) {
                    "${m.tossWinner} won toss & elected to ${m.tossDecision.lowercase(Locale.ROOT)}"
                } else {
                    "Toss pending"
                }
                binding.tvScoringMatchTitle.text = "$tossText • ${m.format} (${m.totalOvers} ov)"

                // 2. Score & Overs Display
                val activeRuns = m.activeRuns
                val activeWickets = m.activeWickets
                binding.tvCurrentScoreDisplay.text = "$activeRuns/$activeWickets"
                binding.tvOversDisplay.text = "${m.activeOversDisplay} Overs (${m.totalOvers} ov)"

                // 3. Batting Team & Innings Indicator
                val battingTeam = if (m.battingTeam.isNotBlank()) m.battingTeam else if (m.currentInnings == 1) m.teamA else m.teamB
                val bowlingTeam = if (m.bowlingTeam.isNotBlank()) m.bowlingTeam else if (m.currentInnings == 1) m.teamB else m.teamA

                // Current Run Rate (CRR)
                val legalBalls = m.activeLegalBalls
                val crr = if (legalBalls > 0) (activeRuns.toDouble() * 6.0 / legalBalls) else 0.0
                binding.tvCRR.text = String.format(Locale.US, "CRR: %.2f", crr)

                // Required Run Rate (RRR) and Target in 2nd Innings
                if (m.currentInnings == 2 && m.target > 0) {
                    val needed = (m.target - m.runsB).coerceAtLeast(0)
                    val totalBalls = m.totalOvers * 6
                    val remainingBalls = (totalBalls - m.legalBallsB).coerceAtLeast(0)
                    binding.tvInningsIndicator.text = "Need $needed runs in $remainingBalls balls (Target: ${m.target})"

                    val rrr = if (remainingBalls > 0) (needed.toDouble() * 6.0 / remainingBalls) else 0.0
                    binding.tvRRR.visibility = View.VISIBLE
                    binding.tvRRR.text = String.format(Locale.US, "RRR: %.2f", rrr)
                } else {
                    binding.tvInningsIndicator.text = "1st Innings • $battingTeam Batting"
                    binding.tvRRR.visibility = View.GONE
                }

                // 4. Over Deliveries Strip (e.g. [0] [1] [4] [Wd] [W] [6])
                renderOverDeliveriesStrip(m.activeInnings)

                // 5. Batters Stats (Striker & Non-Striker)
                val strikerScore = m.getStrikerScore()
                val nonStrikerScore = m.getNonStrikerScore()

                binding.tvStrikerName.text = "${strikerScore.name}*"
                binding.tvStrikerRuns.text = strikerScore.runs.toString()
                binding.tvStrikerBalls.text = strikerScore.balls.toString()
                binding.tvStrikerFours.text = strikerScore.fours.toString()
                binding.tvStrikerSixes.text = strikerScore.sixes.toString()
                binding.tvStrikerSR.text = strikerScore.strikeRateDisplay

                binding.tvNonStrikerName.text = nonStrikerScore.name
                binding.tvNonStrikerRuns.text = nonStrikerScore.runs.toString()
                binding.tvNonStrikerBalls.text = nonStrikerScore.balls.toString()
                binding.tvNonStrikerFours.text = nonStrikerScore.fours.toString()
                binding.tvNonStrikerSixes.text = nonStrikerScore.sixes.toString()
                binding.tvNonStrikerSR.text = nonStrikerScore.strikeRateDisplay

                // 6. Current Bowler Stats
                val bowlerScore = m.getCurrentBowlerScore()
                binding.tvBowlerName.text = bowlerScore.name.ifBlank { "$bowlingTeam Bowler" }
                binding.tvBowlerOvers.text = bowlerScore.oversDisplay
                binding.tvBowlerMaidens.text = bowlerScore.maidens.toString()
                binding.tvBowlerRuns.text = bowlerScore.runsConceded.toString()
                binding.tvBowlerWickets.text = bowlerScore.wickets.toString()
                binding.tvBowlerEconomy.text = bowlerScore.economyDisplay

                // 7. Partnership & Extras
                val ext = m.activeInnings.extras
                binding.tvPartnershipStats.text = "Partnership: ${m.partnershipRuns} runs (${m.partnershipBalls} balls)"
                binding.tvRunRateAndExtras.text = "Extras: ${ext.total} (WD ${ext.wides}, NB ${ext.noBalls}, B ${ext.byes}, LB ${ext.legByes}, P ${ext.penalty})"

                // 8. Commentary Feed
                commentaryAdapter.updateData(m.commentary)

                // 9. Over Completed Trigger (Ask for next bowler)
                val completedOvers = m.activeLegalBalls / 6
                if (m.activeLegalBalls > 0 && m.activeLegalBalls % 6 == 0 && completedOvers != lastPromptedOverNumber && m.status == "LIVE") {
                    lastPromptedOverNumber = completedOvers
                    showOverCompletedDialog(m, completedOvers)
                }
            }
        }

        setupKeypadListeners()
        setupTopActions()
    }

    private fun renderOverDeliveriesStrip(inn: InningsScorecard) {
        binding.llOverDeliveries.removeAllViews()
        val deliveries = inn.currentOverDeliveries

        if (deliveries.isEmpty()) {
            binding.tvNoBallsInOverYet.visibility = View.VISIBLE
            binding.tvOverTotalRuns.text = "= 0"
            return
        }
        binding.tvNoBallsInOverYet.visibility = View.GONE

        var overRunsSum = 0

        deliveries.forEach { item ->
            val tvBubble = TextView(requireContext()).apply {
                val params = LinearLayout.LayoutParams(
                    (32 * resources.displayMetrics.density).toInt(),
                    (32 * resources.displayMetrics.density).toInt()
                ).apply {
                    setMargins(
                        (3 * resources.displayMetrics.density).toInt(),
                        0,
                        (3 * resources.displayMetrics.density).toInt(),
                        0
                    )
                }
                layoutParams = params
                gravity = Gravity.CENTER
                textSize = 11f
                text = item

                when {
                    item == "4" || item == "6" -> {
                        setBackgroundResource(R.drawable.bg_ball_bubble_boundary)
                        setTextColor(Color.WHITE)
                    }
                    item.contains("W") && !item.contains("Wd") -> {
                        setBackgroundResource(R.drawable.bg_ball_bubble_wicket)
                        setTextColor(ContextCompat.getColor(context, R.color.primary_red))
                    }
                    item.contains("Wd") || item.contains("Nb") || item.startsWith("P") -> {
                        setBackgroundResource(R.drawable.bg_ball_bubble_extra)
                        setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    }
                    else -> {
                        setBackgroundResource(R.drawable.bg_ball_bubble_default)
                        setTextColor(ContextCompat.getColor(context, R.color.header_black))
                    }
                }
            }
            binding.llOverDeliveries.addView(tvBubble)

            // Approximate sum for quick display
            val cleanDigits = item.filter { it.isDigit() }
            if (cleanDigits.isNotBlank()) {
                overRunsSum += cleanDigits.toIntOrNull() ?: 0
            }
        }
        binding.tvOverTotalRuns.text = "= $overRunsSum"
    }

    private fun setupKeypadListeners() {
        // Runs 0..6
        binding.btnRun0.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 0) }
        binding.btnRun1.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 1) }
        binding.btnRun2.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 2) }
        binding.btnRun3.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 3) }
        binding.btnRun4.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 4) }
        binding.btnRun5.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 5) }
        binding.btnRun6.setOnClickListener { viewModel.recordBall(matchId = matchId, runs = 6) }

        // Extras
        binding.btnWide.setOnClickListener { showWideRunsDialog() }
        binding.btnNoBall.setOnClickListener { showNoBallRunsDialog() }
        binding.btnBye.setOnClickListener { showByesDialog(isLegBye = false) }
        binding.btnLegBye.setOnClickListener { showByesDialog(isLegBye = true) }
        binding.btnPenalty.setOnClickListener {
            viewModel.recordBall(matchId = matchId, runs = 0, extraType = "Penalty", penaltyRuns = 5)
            Toast.makeText(requireContext(), "+5 Penalty runs awarded", Toast.LENGTH_SHORT).show()
        }

        // Wicket
        binding.btnWicket.setOnClickListener { showWicketEntryDialog() }

        // Quick Controls
        binding.btnUndo.setOnClickListener { performUndo() }
        binding.btnChangeStriker.setOnClickListener {
            viewModel.rotateStrike(matchId)
            Toast.makeText(requireContext(), "Strike rotated", Toast.LENGTH_SHORT).show()
        }

        binding.btnActionsMenu.setOnClickListener { showActionsBottomSheet() }
        binding.btnViewScorecard.setOnClickListener { showFullScorecardDialog() }
        binding.ivScorecardQuickBtn.setOnClickListener { showFullScorecardDialog() }

        binding.btnChangeBowlerQuick.setOnClickListener { showChangeBowlerDialog() }
        binding.btnEndOver.setOnClickListener {
            viewModel.endOverManually(matchId)
            Toast.makeText(requireContext(), "Over ended manually. Strike rotated.", Toast.LENGTH_SHORT).show()
            showChangeBowlerDialog()
        }

        // Direct Table Row Tap Listeners
        binding.rowStriker.setOnClickListener {
            val m = viewModel.selectedMatch.value ?: return@setOnClickListener
            openPlayerSelectionDialog(
                title = "Select Striker (${m.battingTeam})",
                teamName = m.battingTeam,
                excludedPlayerNames = listOf(m.nonStrikerName),
                initiallySelectedName = m.strikerName
            ) { selectedPlayer ->
                viewModel.changeStriker(matchId, selectedPlayer.playerName)
                Toast.makeText(requireContext(), "Striker changed to ${selectedPlayer.playerName}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rowNonStriker.setOnClickListener {
            val m = viewModel.selectedMatch.value ?: return@setOnClickListener
            openPlayerSelectionDialog(
                title = "Select Non-Striker (${m.battingTeam})",
                teamName = m.battingTeam,
                excludedPlayerNames = listOf(m.strikerName),
                initiallySelectedName = m.nonStrikerName
            ) { selectedPlayer ->
                viewModel.changeNonStriker(matchId, selectedPlayer.playerName)
                Toast.makeText(requireContext(), "Non-Striker changed to ${selectedPlayer.playerName}", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rowCurrentBowler.setOnClickListener {
            showChangeBowlerDialog()
        }

        // Tap on Score Displays to view Scorecard
        binding.tvCurrentScoreDisplay.setOnClickListener { showFullScorecardDialog() }
        binding.tvOversDisplay.setOnClickListener { showFullScorecardDialog() }
        binding.tvScoringTeams.setOnClickListener { showFullScorecardDialog() }

        // Live Feed refresh
        binding.tvClearOrRefreshCommentary.setOnClickListener {
            viewModel.loadMatchDetails(matchId)
            Toast.makeText(requireContext(), "Live scoring feed refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTopActions() {
        binding.ivScoringBack.setOnClickListener { findNavController().navigateUp() }
        binding.ivScoringSettings.setOnClickListener { showActionsBottomSheet() }
    }

    private fun performUndo() {
        val success = viewModel.undoLastBall(matchId)
        if (success) {
            Toast.makeText(requireContext(), "Last delivery undone successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No deliveries left to undo in this match", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWideRunsDialog() {
        val options = arrayOf("Wide (+1)", "Wide + 1 run (+2)", "Wide + 2 runs (+3)", "Wide + 3 runs (+4)", "Wide + 4 Boundary (+5)")
        AlertDialog.Builder(requireContext())
            .setTitle("Wide Ball Options")
            .setItems(options) { _, which ->
                val additionalRuns = which
                viewModel.recordBall(matchId = matchId, runs = additionalRuns, extraType = "Wide")
                Toast.makeText(requireContext(), "Wide ball (+${additionalRuns + 1} runs) recorded", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNoBallRunsDialog() {
        val options = arrayOf("No Ball (+1)", "No Ball + 1 off bat (+2)", "No Ball + 2 off bat (+3)", "No Ball + 3 off bat (+4)", "No Ball + 4 Boundary (+5)", "No Ball + 6 Maximum (+7)")
        val runValues = intArrayOf(0, 1, 2, 3, 4, 6)
        AlertDialog.Builder(requireContext())
            .setTitle("No Ball (Free Hit!)")
            .setItems(options) { _, which ->
                val batRuns = runValues[which]
                viewModel.recordBall(matchId = matchId, runs = batRuns, extraType = "No Ball")
                Toast.makeText(requireContext(), "No Ball (+${batRuns + 1} runs) recorded • FREE HIT!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showByesDialog(isLegBye: Boolean) {
        val label = if (isLegBye) "Leg Bye" else "Bye"
        val options = arrayOf("1 $label", "2 ${label}s", "3 ${label}s", "4 ${label}s (Boundary)")
        val runValues = intArrayOf(1, 2, 3, 4)
        AlertDialog.Builder(requireContext())
            .setTitle("Select $label Runs")
            .setItems(options) { _, which ->
                val byeRuns = runValues[which]
                viewModel.recordBall(matchId = matchId, runs = byeRuns, extraType = label)
                Toast.makeText(requireContext(), "$byeRuns $label recorded", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWicketEntryDialog() {
        val match = viewModel.selectedMatch.value ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_record_wicket, null)

        val rbStriker = dialogView.findViewById<RadioButton>(R.id.rbStrikerOut)
        val rbNonStriker = dialogView.findViewById<RadioButton>(R.id.rbNonStrikerOut)
        val spDismissal = dialogView.findViewById<Spinner>(R.id.spDismissalType)
        val tilFielder = dialogView.findViewById<View>(R.id.tilFielderName)
        val etFielder = dialogView.findViewById<EditText>(R.id.etFielderName)
        val cbCreditBowler = dialogView.findViewById<CheckBox>(R.id.cbCreditBowler)
        val btnPickIncoming = dialogView.findViewById<View>(R.id.btnPickIncomingFromSquad)
        val etIncoming = dialogView.findViewById<EditText>(R.id.etIncomingBatter)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmWicket)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelWicket)
        val ivClose = dialogView.findViewById<ImageView>(R.id.ivCloseWicketDialog)

        rbStriker.text = "Striker (${match.strikerName}*)"
        rbNonStriker.text = "Non-Striker (${match.nonStrikerName})"

        val dismissals = arrayOf(
            "Bowled",
            "Caught",
            "Caught and Bowled",
            "LBW",
            "Run Out",
            "Stumped",
            "Hit Wicket",
            "Retired Out",
            "Retired Hurt"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, dismissals)
        spDismissal.adapter = adapter

        spDismissal.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = dismissals[position]
                val needsFielder = selected == "Caught" || selected == "Run Out" || selected == "Stumped"
                tilFielder.visibility = if (needsFielder) View.VISIBLE else View.GONE
                cbCreditBowler.isChecked = selected != "Run Out" && selected != "Retired Out" && selected != "Retired Hurt"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Auto-suggest next eligible batter from batting team squad
        val eligibleBatters = viewModel.getEligibleIncomingBatters(match.battingTeam, match)
        val defaultIncoming = eligibleBatters.firstOrNull()?.playerName
            ?: "${match.battingTeam} Batter ${match.activeWickets + 3}"
        etIncoming.setText(defaultIncoming)

        btnPickIncoming?.setOnClickListener {
            val currentStriker = if (rbStriker.isChecked) match.strikerName else match.nonStrikerName
            val currentNonStriker = if (rbStriker.isChecked) match.nonStrikerName else match.strikerName
            val alreadyDismissed = match.activeInnings.battingList.filter { it.isOut }.map { it.name }
            val excluded = (alreadyDismissed + listOf(currentStriker, currentNonStriker)).distinct()

            openPlayerSelectionDialog(
                title = "Select Incoming Batter",
                teamName = match.battingTeam,
                excludedPlayerNames = excluded,
                initiallySelectedName = etIncoming.text.toString().trim()
            ) { selectedPlayer ->
                etIncoming.setText(selectedPlayer.playerName)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }
        ivClose.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val dismissedBatter = if (rbStriker.isChecked) match.strikerName else match.nonStrikerName
            val selectedDismissal = spDismissal.selectedItem.toString()
            val fielder = etFielder.text.toString().trim()
            val incoming = etIncoming.text.toString().trim().ifBlank { defaultIncoming }

            viewModel.recordBall(
                matchId = matchId,
                runs = 0,
                isWicket = true,
                wicketType = selectedDismissal,
                dismissedPlayer = dismissedBatter,
                incomingBatsman = incoming,
                fielderName = fielder
            )

            dialog.dismiss()
            Toast.makeText(requireContext(), "OUT! $dismissedBatter is dismissed ($selectedDismissal). $incoming walks in.", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showOverCompletedDialog(match: Match, overNumber: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_over_complete, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvOverCompleteTitle)
        val tvSummary = dialogView.findViewById<TextView>(R.id.tvOverCompleteSummary)
        val tvStriker = dialogView.findViewById<TextView>(R.id.tvCurrentStrikerNotice)
        val btnPickBowler = dialogView.findViewById<View>(R.id.btnPickNextBowlerFromSquad)
        val etBowler = dialogView.findViewById<EditText>(R.id.etNextBowler)
        val btnUndo = dialogView.findViewById<Button>(R.id.btnUndoInOverDialog)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmNextBowler)
        val ivCloseOver = dialogView.findViewById<ImageView?>(R.id.ivCloseOverCompleteDialog)

        tvTitle.text = "Over $overNumber Completed!"
        tvSummary.text = "6 legal deliveries completed. Over score recorded."
        tvStriker.text = "New Striker: ${match.strikerName}*"

        val bowlingSquad = viewModel.getSquadForTeam(match.bowlingTeam)
        val eligibleBowlers = bowlingSquad.filter { !it.playerName.equals(match.bowlerName, ignoreCase = true) }
        val defaultNextBowler = eligibleBowlers.firstOrNull()?.playerName ?: "${match.bowlingTeam} Bowler ${(overNumber % 5) + 2}"
        etBowler.setText(defaultNextBowler)

        btnPickBowler?.setOnClickListener {
            openPlayerSelectionDialog(
                title = "Select Next Bowler (Over ${overNumber + 1})",
                teamName = match.bowlingTeam,
                excludedPlayerNames = listOf(match.bowlerName), // Previous bowler cannot bowl consecutive overs
                initiallySelectedName = etBowler.text.toString().trim()
            ) { selectedPlayer ->
                etBowler.setText(selectedPlayer.playerName)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        ivCloseOver?.setOnClickListener { dialog.dismiss() }

        btnUndo.setOnClickListener {
            dialog.dismiss()
            performUndo()
        }

        btnConfirm.setOnClickListener {
            val nextBowler = etBowler.text.toString().trim().ifBlank { defaultNextBowler }
            val allowed = viewModel.changeBowler(matchId, nextBowler)
            if (!allowed) {
                Toast.makeText(requireContext(), "Previous bowler ($nextBowler) cannot bowl consecutive overs!", Toast.LENGTH_LONG).show()
            } else {
                dialog.dismiss()
                Toast.makeText(requireContext(), "Over $overNumber concluded. $nextBowler is bowling now.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showChangeBowlerDialog() {
        val match = viewModel.selectedMatch.value ?: return
        openPlayerSelectionDialog(
            title = "Change Bowler (${match.bowlingTeam})",
            teamName = match.bowlingTeam,
            excludedPlayerNames = listOf(match.previousBowlerName.ifBlank { "" }),
            initiallySelectedName = match.bowlerName
        ) { selectedBowler ->
            val allowed = viewModel.changeBowler(matchId, selectedBowler.playerName)
            if (!allowed) {
                Toast.makeText(requireContext(), "Previous bowler cannot bowl consecutive overs!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Bowler changed to ${selectedBowler.playerName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showActionsBottomSheet() {
        val match = viewModel.selectedMatch.value ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_scoring_actions, null)

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<ImageView>(R.id.ivCloseActionsDialog).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.actionUndoLastBall).setOnClickListener {
            dialog.dismiss()
            performUndo()
        }

        dialogView.findViewById<View>(R.id.actionRotateStrike).setOnClickListener {
            dialog.dismiss()
            viewModel.rotateStrike(matchId)
            Toast.makeText(requireContext(), "Strike rotated", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<View>(R.id.actionChangeBowler).setOnClickListener {
            dialog.dismiss()
            showChangeBowlerDialog()
        }

        dialogView.findViewById<View>(R.id.actionChangeBatter).setOnClickListener {
            dialog.dismiss()
            showChangeBatterDialog(match)
        }

        dialogView.findViewById<View>(R.id.actionRetireBatter).setOnClickListener {
            dialog.dismiss()
            showRetireBatterDialog()
        }

        dialogView.findViewById<View>(R.id.actionEndOver).setOnClickListener {
            dialog.dismiss()
            viewModel.endOverManually(matchId)
            Toast.makeText(requireContext(), "Over concluded manually. Strike rotated.", Toast.LENGTH_SHORT).show()
        }

        dialogView.findViewById<View>(R.id.actionViewScorecard).setOnClickListener {
            dialog.dismiss()
            showFullScorecardDialog()
        }

        dialogView.findViewById<View>(R.id.actionSwitchInnings).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Switch Innings")
                .setMessage("Are you sure you want to conclude the current innings and start Innings 2?")
                .setPositiveButton("Switch Now") { _, _ ->
                    viewModel.switchInnings(matchId)
                    Toast.makeText(requireContext(), "Innings 1 concluded. Target set!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialogView.findViewById<View>(R.id.actionEndMatch).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Conclude Match")
                .setMessage("Are you sure you want to conclude this match and declare the winner?")
                .setPositiveButton("Conclude Match") { _, _ ->
                    viewModel.completeMatch(matchId)
                    Toast.makeText(requireContext(), "Match Concluded & Result Declared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }

    private fun showChangeBatterDialog(match: Match) {
        val items = arrayOf("Change Striker (${match.strikerName}*)", "Change Non-Striker (${match.nonStrikerName})")
        AlertDialog.Builder(requireContext())
            .setTitle("Change / Replace Batter")
            .setItems(items) { _, which ->
                if (which == 0) {
                    openPlayerSelectionDialog(
                        title = "Select New Striker",
                        teamName = match.battingTeam,
                        excludedPlayerNames = listOf(match.nonStrikerName),
                        initiallySelectedName = match.strikerName
                    ) { selectedPlayer ->
                        viewModel.changeStriker(matchId, selectedPlayer.playerName)
                        Toast.makeText(requireContext(), "Striker changed to ${selectedPlayer.playerName}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    openPlayerSelectionDialog(
                        title = "Select New Non-Striker",
                        teamName = match.battingTeam,
                        excludedPlayerNames = listOf(match.strikerName),
                        initiallySelectedName = match.nonStrikerName
                    ) { selectedPlayer ->
                        viewModel.changeNonStriker(matchId, selectedPlayer.playerName)
                        Toast.makeText(requireContext(), "Non-Striker changed to ${selectedPlayer.playerName}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRetireBatterDialog() {
        val match = viewModel.selectedMatch.value ?: return
        val dismissed = match.activeInnings.battingList.filter { it.isOut }.map { it.name }
        val excluded = (dismissed + listOf(match.strikerName, match.nonStrikerName)).distinct()

        openPlayerSelectionDialog(
            title = "Select Incoming Batter for Retired Batter",
            teamName = match.battingTeam,
            excludedPlayerNames = excluded
        ) { selectedPlayer ->
            viewModel.retireBatsman(matchId, selectedPlayer.playerName)
            Toast.makeText(requireContext(), "${match.strikerName} retired. ${selectedPlayer.playerName} is now on strike.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFullScorecardDialog() {
        val match = viewModel.selectedMatch.value ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_full_scorecard, null)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        dialog.setContentView(dialogView)

        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        }

        // Apply system window insets so content respects status bar and navigation bar
        val headerBar = dialogView.findViewById<View>(R.id.llScorecardHeaderBar)
        val nsvContent = dialogView.findViewById<View>(R.id.nsvScorecardContent)
        ViewCompat.setOnApplyWindowInsetsListener(dialogView) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            headerBar.setPadding(
                headerBar.paddingLeft,
                statusBarInset.top,
                headerBar.paddingRight,
                headerBar.paddingBottom
            )
            nsvContent.setPadding(
                nsvContent.paddingLeft,
                nsvContent.paddingTop,
                nsvContent.paddingRight,
                navBarInset.bottom + (28 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val ivBack = dialogView.findViewById<ImageView>(R.id.ivScorecardBack)
        val tabInn1 = dialogView.findViewById<TextView>(R.id.tabInnings1)
        val tabInn2 = dialogView.findViewById<TextView>(R.id.tabInnings2)
        val tvTeam = dialogView.findViewById<TextView>(R.id.tvScorecardInningsTeam)
        val tvBadge = dialogView.findViewById<TextView>(R.id.tvScorecardInningsBadge)
        val tvScore = dialogView.findViewById<TextView>(R.id.tvScorecardInningsScore)
        val tvOvers = dialogView.findViewById<TextView>(R.id.tvScorecardInningsOvers)
        val tvCrr = dialogView.findViewById<TextView>(R.id.tvScorecardInningsCrr)
        val tvRrr = dialogView.findViewById<TextView>(R.id.tvScorecardInningsRrr)
        val llBatting = dialogView.findViewById<LinearLayout>(R.id.llBattingRows)
        val tvExtrasBadge = dialogView.findViewById<TextView>(R.id.tvScorecardExtrasTotalBadge)
        val tvExtras = dialogView.findViewById<TextView>(R.id.tvScorecardExtras)
        val llBowling = dialogView.findViewById<LinearLayout>(R.id.llBowlingRows)
        val tvFow = dialogView.findViewById<TextView>(R.id.tvFallOfWickets)

        ivBack.setOnClickListener { dialog.dismiss() }

        var selectedTab = if (match.currentInnings == 2) 2 else 1

        fun populateInnings(innNum: Int) {
            val inn = if (innNum == 1) match.innings1 else match.innings2
            val teamName = if (innNum == 1) {
                if (match.tossDecision.equals("BAT", ignoreCase = true)) match.tossWinner.ifBlank { match.teamA } else if (match.tossWinner == match.teamA) match.teamB else match.teamA
            } else {
                if (match.tossDecision.equals("BAT", ignoreCase = true)) (if (match.tossWinner == match.teamA) match.teamB else match.teamA) else match.tossWinner.ifBlank { match.teamB }
            }
            val runs = if (innNum == 1) match.runsA else match.runsB
            val wickets = if (innNum == 1) match.wicketsA else match.wicketsB
            val oversStr = if (innNum == 1) match.oversADisplay else match.oversBDisplay
            val legalBalls = if (innNum == 1) match.legalBallsA else match.legalBallsB
            val crrVal = if (legalBalls > 0) (runs.toDouble() * 6.0 / legalBalls) else 0.0

            tvTeam.text = teamName
            tvBadge.text = if (innNum == 1) "1ST INNINGS" else "2ND INNINGS"
            tvScore.text = "$runs/$wickets"
            tvOvers.text = "($oversStr / ${match.totalOvers}.0 ov)"
            tvCrr.text = String.format(Locale.US, "Current Run Rate: %.2f", crrVal)

            if (innNum == 2 && match.target > 0) {
                val needed = (match.target - runs).coerceAtLeast(0)
                val remainingBalls = ((match.totalOvers * 6) - legalBalls).coerceAtLeast(0)
                val rrr = if (remainingBalls > 0) (needed.toDouble() * 6.0 / remainingBalls) else 0.0
                tvRrr.visibility = View.VISIBLE
                tvRrr.text = String.format(Locale.US, "Req RR: %.2f (Need %d off %d b)", rrr, needed, remainingBalls)
            } else {
                tvRrr.visibility = View.GONE
            }

            // Populate Batting Rows using responsive XML view binding
            llBatting.removeAllViews()
            if (inn.battingList.isEmpty()) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No batters have batted yet in this innings."
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 12f
                    setPadding(12, 16, 12, 16)
                }
                llBatting.addView(emptyTv)
            } else {
                inn.battingList.forEachIndexed { index, b ->
                    val rowBinding = ItemScorecardBattingRowBinding.inflate(layoutInflater, llBatting, false)
                    val star = if (b.isStriker) "*" else ""
                    rowBinding.tvRowBatterName.text = "${b.name}$star"
                    rowBinding.tvRowBatterDismissal.text = b.dismissalType
                    rowBinding.tvRowBatterR.text = b.runs.toString()
                    rowBinding.tvRowBatterB.text = b.balls.toString()
                    rowBinding.tvRowBatter4s.text = b.fours.toString()
                    rowBinding.tvRowBatter6s.text = b.sixes.toString()
                    rowBinding.tvRowBatterSR.text = b.strikeRateDisplay

                    llBatting.addView(rowBinding.root)

                    // Add subtle row divider except after the last item
                    if (index < inn.battingList.size - 1) {
                        val divider = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                            setBackgroundColor(ContextCompat.getColor(context, R.color.border_grey))
                        }
                        llBatting.addView(divider)
                    }
                }
            }

            // Extras
            val ext = inn.extras
            tvExtrasBadge.text = ext.total.toString()
            tvExtras.text = "WD ${ext.wides}, NB ${ext.noBalls}, B ${ext.byes}, LB ${ext.legByes}, P ${ext.penalty}"

            // Populate Bowling Rows using responsive XML view binding
            llBowling.removeAllViews()
            if (inn.bowlingList.isEmpty()) {
                val emptyTv = TextView(requireContext()).apply {
                    text = "No bowling figures recorded yet."
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 12f
                    setPadding(12, 16, 12, 16)
                }
                llBowling.addView(emptyTv)
            } else {
                inn.bowlingList.forEachIndexed { index, bw ->
                    val rowBinding = ItemScorecardBowlingRowBinding.inflate(layoutInflater, llBowling, false)
                    rowBinding.tvRowBowlerName.text = bw.name
                    rowBinding.tvRowBowlerO.text = bw.oversDisplay
                    rowBinding.tvRowBowlerM.text = bw.maidens.toString()
                    rowBinding.tvRowBowlerR.text = bw.runsConceded.toString()
                    rowBinding.tvRowBowlerW.text = bw.wickets.toString()
                    rowBinding.tvRowBowlerEcon.text = bw.economyDisplay

                    llBowling.addView(rowBinding.root)

                    // Add subtle row divider except after the last item
                    if (index < inn.bowlingList.size - 1) {
                        val divider = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                            setBackgroundColor(ContextCompat.getColor(context, R.color.border_grey))
                        }
                        llBowling.addView(divider)
                    }
                }
            }

            // Fall of Wickets
            if (inn.fallOfWickets.isEmpty()) {
                tvFow.text = "No wickets fallen yet."
            } else {
                tvFow.text = inn.fallOfWickets.joinToString(separator = "  •  ") { fow ->
                    "${fow.runs}-${fow.wicketNumber} (${fow.batsmanName}, ${fow.overs} ov)"
                }
            }
        }

        tabInn1.setOnClickListener {
            selectedTab = 1
            tabInn1.setBackgroundResource(R.drawable.bg_tab_selected_indicator)
            tabInn1.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
            tabInn2.background = null
            tabInn2.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            populateInnings(1)
        }

        tabInn2.setOnClickListener {
            selectedTab = 2
            tabInn2.setBackgroundResource(R.drawable.bg_tab_selected_indicator)
            tabInn2.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_red))
            tabInn1.background = null
            tabInn1.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            populateInnings(2)
        }

        if (selectedTab == 1) tabInn1.performClick() else tabInn2.performClick()

        dialog.show()
    }

    private fun showTossSetupDialog(match: Match) {
        isTossDialogOpen = true
        val dialogView = layoutInflater.inflate(R.layout.dialog_toss_setup, null)

        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvTossMatchSubtitle)
        val rgWinner = dialogView.findViewById<RadioGroup>(R.id.rgTossWinner)
        val rbTeam1 = dialogView.findViewById<RadioButton>(R.id.rbTossTeam1)
        val rbTeam2 = dialogView.findViewById<RadioButton>(R.id.rbTossTeam2)
        val rgDecision = dialogView.findViewById<RadioGroup>(R.id.rgTossDecision)
        val rbBat = dialogView.findViewById<RadioButton>(R.id.rbDecisionBat)

        val tvBattingSectionTitle = dialogView.findViewById<TextView>(R.id.tvOpeningBatsmenSectionTitle)
        val cardStriker = dialogView.findViewById<View>(R.id.cardSelectStriker)
        val tvStrikerName = dialogView.findViewById<TextView>(R.id.tvSelectedStrikerName)
        val tvStrikerDetails = dialogView.findViewById<TextView>(R.id.tvSelectedStrikerDetails)
        val btnChangeStriker = dialogView.findViewById<Button>(R.id.btnChangeStrikerToss)

        val cardNonStriker = dialogView.findViewById<View>(R.id.cardSelectNonStriker)
        val tvNonStrikerName = dialogView.findViewById<TextView>(R.id.tvSelectedNonStrikerName)
        val tvNonStrikerDetails = dialogView.findViewById<TextView>(R.id.tvSelectedNonStrikerDetails)
        val btnChangeNonStriker = dialogView.findViewById<Button>(R.id.btnChangeNonStrikerToss)

        val tvBowlingSectionTitle = dialogView.findViewById<TextView>(R.id.tvOpeningBowlerSectionTitle)
        val cardBowler = dialogView.findViewById<View>(R.id.cardSelectBowler)
        val tvBowlerName = dialogView.findViewById<TextView>(R.id.tvSelectedBowlerName)
        val tvBowlerDetails = dialogView.findViewById<TextView>(R.id.tvSelectedBowlerDetails)
        val btnChangeBowler = dialogView.findViewById<Button>(R.id.btnChangeBowlerToss)

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmTossStart)
        val ivCloseToss = dialogView.findViewById<ImageView?>(R.id.ivCloseTossDialog)

        tvSubtitle.text = "${match.teamA} vs ${match.teamB} • ${match.venue}"
        rbTeam1.text = match.teamA
        rbTeam2.text = match.teamB

        var currentBattingTeam = ""
        var currentBowlingTeam = ""
        var selectedStriker: TeamPlayerItem? = null
        var selectedNonStriker: TeamPlayerItem? = null
        var selectedBowler: TeamPlayerItem? = null

        fun updateCards() {
            selectedStriker?.let {
                tvStrikerName.text = it.playerName
                val cap = if (it.isCaptain) " (C)" else if (it.isViceCaptain) " (VC)" else ""
                val j = if (it.jerseyNumber.isNotBlank()) "#${it.jerseyNumber} • " else ""
                tvStrikerDetails.text = "$j${it.role}$cap"
                btnChangeStriker.text = "Change"
            }

            selectedNonStriker?.let {
                tvNonStrikerName.text = it.playerName
                val cap = if (it.isCaptain) " (C)" else if (it.isViceCaptain) " (VC)" else ""
                val j = if (it.jerseyNumber.isNotBlank()) "#${it.jerseyNumber} • " else ""
                tvNonStrikerDetails.text = "$j${it.role}$cap"
                btnChangeNonStriker.text = "Change"
            }

            selectedBowler?.let {
                tvBowlerName.text = it.playerName
                val cap = if (it.isCaptain) " (C)" else if (it.isViceCaptain) " (VC)" else ""
                val j = if (it.jerseyNumber.isNotBlank()) "#${it.jerseyNumber} • " else ""
                tvBowlerDetails.text = "$j${it.role}$cap"
                btnChangeBowler.text = "Change"
            }
        }

        fun refreshTeamsAndSquads(preserveExistingIfPossible: Boolean) {
            val winner = if (rbTeam1.isChecked) match.teamA else match.teamB
            val decision = if (rbBat.isChecked) "BAT" else "BOWL"
            val newBattingTeam = if ((rbTeam1.isChecked && rbBat.isChecked) || (!rbTeam1.isChecked && !rbBat.isChecked)) match.teamA else match.teamB
            val newBowlingTeam = if (newBattingTeam == match.teamA) match.teamB else match.teamA

            val battingChanged = newBattingTeam != currentBattingTeam
            val bowlingChanged = newBowlingTeam != currentBowlingTeam

            currentBattingTeam = newBattingTeam
            currentBowlingTeam = newBowlingTeam

            tvBattingSectionTitle.text = "3. Opening Batsmen ($currentBattingTeam Squad)"
            tvBowlingSectionTitle.text = "4. Opening Bowler ($currentBowlingTeam Squad)"

            val battingSquad = viewModel.getSquadForTeam(currentBattingTeam)
            val bowlingSquad = viewModel.getSquadForTeam(currentBowlingTeam)

            if (battingChanged || !preserveExistingIfPossible || selectedStriker == null) {
                selectedStriker = battingSquad.getOrNull(0) ?: TeamPlayerItem(
                    id = "striker_auto",
                    playerName = "$currentBattingTeam Opener 1",
                    role = "Batsman",
                    jerseyNumber = "1"
                )
                selectedNonStriker = battingSquad.getOrNull(1) ?: TeamPlayerItem(
                    id = "nonstriker_auto",
                    playerName = "$currentBattingTeam Opener 2",
                    role = "Batsman",
                    jerseyNumber = "2"
                )
            }

            if (bowlingChanged || !preserveExistingIfPossible || selectedBowler == null) {
                selectedBowler = bowlingSquad.firstOrNull { it.role.contains("Bowl", ignoreCase = true) || it.role.contains("All", ignoreCase = true) }
                    ?: bowlingSquad.getOrNull(0)
                    ?: TeamPlayerItem(
                        id = "bowler_auto",
                        playerName = "$currentBowlingTeam Bowler 1",
                        role = "Bowler",
                        jerseyNumber = "11"
                    )
            }

            updateCards()
        }

        rgWinner.setOnCheckedChangeListener { _, _ -> refreshTeamsAndSquads(preserveExistingIfPossible = false) }
        rgDecision.setOnCheckedChangeListener { _, _ -> refreshTeamsAndSquads(preserveExistingIfPossible = false) }

        // Initial setup
        refreshTeamsAndSquads(preserveExistingIfPossible = false)

        val chooseStrikerAction = {
            val excluded = selectedNonStriker?.playerName?.let { listOf(it) } ?: emptyList()
            openPlayerSelectionDialog(
                title = "Select Striker ($currentBattingTeam)",
                teamName = currentBattingTeam,
                excludedPlayerNames = excluded,
                initiallySelectedName = selectedStriker?.playerName ?: ""
            ) { player ->
                selectedStriker = player
                updateCards()
            }
        }

        val chooseNonStrikerAction = {
            val excluded = selectedStriker?.playerName?.let { listOf(it) } ?: emptyList()
            openPlayerSelectionDialog(
                title = "Select Non-Striker ($currentBattingTeam)",
                teamName = currentBattingTeam,
                excludedPlayerNames = excluded,
                initiallySelectedName = selectedNonStriker?.playerName ?: ""
            ) { player ->
                selectedNonStriker = player
                updateCards()
            }
        }

        val chooseBowlerAction = {
            openPlayerSelectionDialog(
                title = "Select Opening Bowler ($currentBowlingTeam)",
                teamName = currentBowlingTeam,
                initiallySelectedName = selectedBowler?.playerName ?: ""
            ) { player ->
                selectedBowler = player
                updateCards()
            }
        }

        cardStriker.setOnClickListener { chooseStrikerAction() }
        btnChangeStriker.setOnClickListener { chooseStrikerAction() }

        cardNonStriker.setOnClickListener { chooseNonStrikerAction() }
        btnChangeNonStriker.setOnClickListener { chooseNonStrikerAction() }

        cardBowler.setOnClickListener { chooseBowlerAction() }
        btnChangeBowler.setOnClickListener { chooseBowlerAction() }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        ivCloseToss?.setOnClickListener {
            isTossDialogOpen = false
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val strikerName = selectedStriker?.playerName?.trim() ?: ""
            val nonStrikerName = selectedNonStriker?.playerName?.trim() ?: ""
            val bowlerName = selectedBowler?.playerName?.trim() ?: ""

            if (strikerName.isBlank() || nonStrikerName.isBlank() || bowlerName.isBlank()) {
                Toast.makeText(requireContext(), "Please select Striker, Non-Striker, and Opening Bowler", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (strikerName.equals(nonStrikerName, ignoreCase = true)) {
                Toast.makeText(requireContext(), "Striker and Non-Striker cannot be the same player!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val winner = if (rbTeam1.isChecked) match.teamA else match.teamB
            val decision = if (rbBat.isChecked) "BAT" else "BOWL"

            viewModel.recordTossAndOpeners(
                matchId = match.id,
                tossWinner = winner,
                tossDecision = decision,
                battingTeam = currentBattingTeam,
                bowlingTeam = currentBowlingTeam,
                striker = strikerName,
                nonStriker = nonStrikerName,
                bowler = bowlerName
            )

            isTossDialogOpen = false
            dialog.dismiss()
            Toast.makeText(requireContext(), "Toss confirmed! $winner elected to $decision first.", Toast.LENGTH_LONG).show()
        }

        dialog.show()
    }

    private fun openPlayerSelectionDialog(
        title: String,
        teamName: String,
        excludedPlayerNames: List<String> = emptyList(),
        initiallySelectedName: String = "",
        onPlayerSelected: (TeamPlayerItem) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_player_selection, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvPlayerSelectionTitle)
        val tvTeam = dialogView.findViewById<TextView>(R.id.tvPlayerSelectionTeam)
        val ivClose = dialogView.findViewById<ImageView>(R.id.ivClosePlayerSelection)
        val etSearch = dialogView.findViewById<EditText>(R.id.etSearchPlayerPicker)
        val rvList = dialogView.findViewById<RecyclerView>(R.id.rvPlayerPickerList)
        val llEmpty = dialogView.findViewById<View>(R.id.llEmptySquadNotice)
        val btnAddPlayer = dialogView.findViewById<Button>(R.id.btnAddNewPlayerToSquad)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmPlayerSelection)

        tvTitle.text = title
        tvTeam.text = "$teamName Squad"

        val excludedLower = excludedPlayerNames.map { it.trim().lowercase(Locale.ROOT) }

        fun getFilteredSquad(): List<TeamPlayerItem> {
            val squad = viewModel.getSquadForTeam(teamName)
            return squad.filter { p ->
                val pName = p.playerName.trim().lowercase(Locale.ROOT)
                pName !in excludedLower || (initiallySelectedName.isNotBlank() && pName == initiallySelectedName.trim().lowercase(Locale.ROOT))
            }
        }

        var currentSquad = getFilteredSquad()
        var selectedPlayer: TeamPlayerItem? = currentSquad.firstOrNull {
            it.playerName.equals(initiallySelectedName, ignoreCase = true) || it.id == initiallySelectedName
        } ?: currentSquad.firstOrNull()

        val adapter = PlayerPickerAdapter(currentSquad, selectedPlayer?.id ?: "") { picked ->
            selectedPlayer = picked
        }

        rvList.layoutManager = LinearLayoutManager(requireContext())
        rvList.adapter = adapter

        if (currentSquad.isEmpty()) {
            rvList.visibility = View.GONE
            llEmpty.visibility = View.VISIBLE
        } else {
            rvList.visibility = View.VISIBLE
            llEmpty.visibility = View.GONE
        }

        etSearch.doAfterTextChanged { text ->
            adapter.filter(text?.toString().orEmpty())
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.BOTTOM)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        ivClose.setOnClickListener { dialog.dismiss() }

        btnAddPlayer.setOnClickListener {
            showAddPlayerToSquadDialog(teamName) { newPlayer ->
                currentSquad = getFilteredSquad()
                selectedPlayer = newPlayer
                adapter.updateList(currentSquad, newPlayer.id)
                rvList.visibility = View.VISIBLE
                llEmpty.visibility = View.GONE
            }
        }

        btnConfirm.setOnClickListener {
            val chosen = adapter.getSelectedPlayer() ?: selectedPlayer
            if (chosen != null) {
                onPlayerSelected(chosen)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a player or add one to the squad", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddPlayerToSquadDialog(
        teamName: String,
        onPlayerAdded: (TeamPlayerItem) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_player, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogPlayerName)
        val etRole = dialogView.findViewById<EditText>(R.id.etDialogPlayerRole)
        val etJersey = dialogView.findViewById<EditText>(R.id.etDialogPlayerJersey)
        val etContact = dialogView.findViewById<EditText>(R.id.etDialogPlayerContact)
        val cbCaptain = dialogView.findViewById<CheckBox>(R.id.cbIsCaptain)
        val cbViceCaptain = dialogView.findViewById<CheckBox>(R.id.cbIsViceCaptain)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelAddPlayer)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmAddPlayer)

        val roles = arrayOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper")
        etRole.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Player Role")
                .setItems(roles) { _, which ->
                    etRole.setText(roles[which])
                }
                .show()
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.BOTTOM)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Player name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = etRole.text.toString().trim().ifBlank { "Batsman" }
            val jersey = etJersey.text.toString().trim().ifBlank { "0" }
            val contact = etContact.text.toString().trim()
            val newPlayer = TeamPlayerItem(
                id = "P_${UUID.randomUUID().toString().take(8)}",
                playerName = name,
                role = role,
                jerseyNumber = jersey,
                isCaptain = cbCaptain.isChecked,
                isViceCaptain = cbViceCaptain.isChecked,
                contact = contact
            )

            viewModel.addPlayerToTeamSquad(teamName, newPlayer) { success, msg, addedPlayer ->
                if (success) {
                    Toast.makeText(requireContext(), "$name added to $teamName squad", Toast.LENGTH_SHORT).show()
                    onPlayerAdded(addedPlayer)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
