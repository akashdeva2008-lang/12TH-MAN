package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.btnOpenScoring.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", matchId) }
            findNavController().navigate(R.id.action_match_details_to_scoring, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}