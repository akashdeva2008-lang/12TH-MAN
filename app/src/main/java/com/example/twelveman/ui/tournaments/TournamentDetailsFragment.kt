package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentTournamentDetailsBinding

class TournamentDetailsFragment : Fragment() {
    private var _binding: FragmentTournamentDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var tournamentId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            tournamentId = it.getString("tournamentId", "T301")
        }

        viewModel.loadTournamentDetails(tournamentId)
        viewModel.selectedTournament.observe(viewLifecycleOwner) { t ->
            t?.let {
                binding.tvTournamentDetailTitle.text = it.name
                binding.tvTournamentDetailLocation.text = it.location
                binding.tvDetailFormat.text = it.format
                binding.tvDetailTeams.text = "${it.registeredTeamsCount}/${it.totalTeams}"
                binding.tvDetailPrize.text = "₹${it.prizeMoney}"
                binding.tvTournamentDetailDesc.text = it.description
            }
        }

        binding.btnRegisterTeam.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", tournamentId) }
            findNavController().navigate(R.id.action_tournament_details_to_register_team, bundle)
        }

        binding.btnViewFixtures.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", tournamentId) }
            findNavController().navigate(R.id.action_tournament_details_to_fixtures, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}