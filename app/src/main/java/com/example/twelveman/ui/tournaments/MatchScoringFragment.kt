package com.example.twelveman.ui.tournaments

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.twelveman.databinding.FragmentMatchScoringBinding

class MatchScoringFragment : Fragment() {
    private var _binding: FragmentMatchScoringBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var matchId: String = ""

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

        viewModel.loadMatchDetails(matchId)

        viewModel.selectedMatch.observe(viewLifecycleOwner) { match ->
            match?.let { m ->
                binding.tvScoringMatchTitle.text = "Match ${m.matchNumber} • ${m.stage}"
                binding.tvScoringStatus.text = m.status
                binding.tvScoringTeams.text = "${m.teamA} vs ${m.teamB}"

                val activeScore = if (m.currentInnings == 1) m.scoreA else m.scoreB
                binding.tvCurrentScoreDisplay.text = "Score: $activeScore"
                
                val battingTeam = if (m.currentInnings == 1) m.teamA else m.teamB
                binding.tvInningsIndicator.text = "Batting: $battingTeam (Innings ${m.currentInnings})"

                val commentAdapter = ArrayAdapter(requireContext(), R.layout.simple_list_item_1, m.commentary)
                binding.rvCommentary.adapter = null // simple list adapter or list view
            }
        }

        binding.btnRun0.setOnClickListener { viewModel.recordBall(matchId, 0, false, "") }
        binding.btnRun1.setOnClickListener { viewModel.recordBall(matchId, 1, false, "") }
        binding.btnRun2.setOnClickListener { viewModel.recordBall(matchId, 2, false, "") }
        binding.btnRun3.setOnClickListener { viewModel.recordBall(matchId, 3, false, "") }
        binding.btnRun4.setOnClickListener { viewModel.recordBall(matchId, 4, false, "BOUNDARY (4)") }
        binding.btnRun6.setOnClickListener { viewModel.recordBall(matchId, 6, false, "SIXER! (6)") }

        binding.btnWicket.setOnClickListener {
            viewModel.recordBall(matchId, 0, true, "Wicket")
            Toast.makeText(requireContext(), "Wicket fallen!", Toast.LENGTH_SHORT).show()
        }

        binding.btnWide.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "Wide (+1)")
        }

        binding.btnNoBall.setOnClickListener {
            viewModel.recordBall(matchId, 1, false, "No Ball (+1)")
        }

        binding.btnSwitchInnings.setOnClickListener {
            viewModel.switchInnings(matchId)
            Toast.makeText(requireContext(), "Innings Switched", Toast.LENGTH_SHORT).show()
        }

        binding.btnCompleteMatch.setOnClickListener {
            viewModel.completeMatch(matchId)
            Toast.makeText(requireContext(), "Match Concluded!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}