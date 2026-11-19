package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentMatchScoringBinding

class MatchScoringFragment : Fragment() {
    private var _binding: FragmentMatchScoringBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var matchId: String = ""
    private lateinit var commentaryAdapter: CommentaryAdapter

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
                binding.tvScoringMatchTitle.text = "Match ${m.matchNumber} • ${m.stage}"
                binding.tvScoringStatus.text = m.status
                binding.tvScoringTeams.text = "${m.teamA} vs ${m.teamB}"

                val activeScore = if (m.currentInnings == 1) m.scoreA else m.scoreB
                binding.tvCurrentScoreDisplay.text = activeScore
                
                val battingTeam = if (m.currentInnings == 1) m.teamA else m.teamB
                binding.tvInningsIndicator.text = "Batting: $battingTeam (Innings ${m.currentInnings})"

                commentaryAdapter.updateData(m.commentary)
            }
        }

        // Bowling Angle Selector Handlers
        binding.layoutRoundWicket.setOnClickListener {
            updateBowlingAngle(1)
        }
        binding.layoutBetweenWicket.setOnClickListener {
            updateBowlingAngle(2)
        }
        binding.layoutOverWicket.setOnClickListener {
            updateBowlingAngle(3)
        }

        // Top Settings icon
        binding.ivScoringSettings.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Match Scoring Settings")
                .setMessage("Match ID: $matchId\n\n• Scoring System: Official T20 ICC Standard\n• Powerplay: Overs 1 - 6\n• Wide Penalty: +1 Run, Re-bowl\n• No-Ball Penalty: +1 Run, Free Hit\n• Overs per Bowler Limit: 4.0")
                .setPositiveButton("OK", null)
                .show()
        }

        // Scoring Keypad
        binding.btnRun0.setOnClickListener { viewModel.recordBall(matchId, 0, false, "") }
        binding.btnRun1.setOnClickListener { viewModel.recordBall(matchId, 1, false, "") }
        binding.btnRun2.setOnClickListener { viewModel.recordBall(matchId, 2, false, "") }
        binding.btnRun3.setOnClickListener { viewModel.recordBall(matchId, 3, false, "") }
        binding.btnRun4.setOnClickListener { viewModel.recordBall(matchId, 4, false, "BOUNDARY (4)") }
        binding.btnRun6.setOnClickListener { viewModel.recordBall(matchId, 6, false, "SIXER! (6)") }

        binding.btnWicket.setOnClickListener {
            val dismissalTypes = arrayOf("Bowled", "Caught", "LBW", "Run Out", "Stumped", "Hit Wicket")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Wicket Dismissal Type")
                .setItems(dismissalTypes) { _, which ->
                    val dismissal = dismissalTypes[which]
                    viewModel.recordBall(matchId, 0, true, "Wicket ($dismissal)")
                    Toast.makeText(requireContext(), "Wicket Fall: $dismissal", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.btnWide.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Wide (+1)")
            Toast.makeText(requireContext(), "Wide ball recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        binding.btnNoBall.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "No Ball (+1)")
            Toast.makeText(requireContext(), "No Ball recorded (+1) • FREE HIT!", Toast.LENGTH_SHORT).show()
        }

        binding.btnBye.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Bye (+1)")
            Toast.makeText(requireContext(), "Bye recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        binding.btnLegBye.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Leg Bye (+1)")
            Toast.makeText(requireContext(), "Leg Bye recorded (+1)", Toast.LENGTH_SHORT).show()
        }

        binding.ivScoringBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnUndo.setOnClickListener {
            val success = viewModel.undoLastBall(matchId)
            if (success) {
                Toast.makeText(requireContext(), "Last delivery undone successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Nothing to undo in this match", Toast.LENGTH_SHORT).show()
            }
        }

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
