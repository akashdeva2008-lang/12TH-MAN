package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentTournamentsBinding

class TournamentsFragment : Fragment() {
    private var _binding: FragmentTournamentsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private lateinit var adapter: TournamentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TournamentAdapter(emptyList()) { tournament ->
            val bundle = Bundle().apply { putString("tournamentId", tournament.id) }
            findNavController().navigate(R.id.action_tournaments_to_details, bundle)
        }

        binding.rvTournaments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@TournamentsFragment.adapter
        }

        viewModel.tournamentsList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        // Host / Create Tournament FAB
        binding.fabCreateTournament.setOnClickListener {
            findNavController().navigate(R.id.action_tournaments_to_create)
        }

        // Notifications Bell
        binding.ivTournamentsNotification.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Tournament Alerts")
                .setMessage("You're subscribed to Premier League T20 & City Cup 2025 fixtures.\nNo delays or schedule changes reported.")
                .setPositiveButton("OK", null)
                .show()
        }

        // Featured Tournament Card Click
        binding.cardFeaturedTournament.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", "T301") }
            findNavController().navigate(R.id.action_tournaments_to_details, bundle)
        }

        // City Cup Sample Card Click
        binding.cardSampleCityCup.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", "T302") }
            findNavController().navigate(R.id.action_tournaments_to_details, bundle)
        }

        // Champions Trophy Sample Card Click
        binding.cardSampleChampionsTrophy.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", "T303") }
            findNavController().navigate(R.id.action_tournaments_to_details, bundle)
        }

        // View All links
        binding.tvViewAllMyTournaments.setOnClickListener {
            Toast.makeText(requireContext(), "Displaying your registered tournaments", Toast.LENGTH_SHORT).show()
        }
        binding.tvViewAllUpcomingTournaments.setOnClickListener {
            Toast.makeText(requireContext(), "Displaying all upcoming sanctioned leagues", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
