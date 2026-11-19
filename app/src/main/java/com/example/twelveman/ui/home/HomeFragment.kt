package com.example.twelveman.ui.home

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
import com.example.twelveman.databinding.FragmentHomeBinding
import com.example.twelveman.ui.players.PlayerAdapter
import com.example.twelveman.ui.umpires.UmpireAdapter

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var umpireAdapter: UmpireAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupRecyclerViews() {
        playerAdapter = PlayerAdapter(emptyList()) { player ->
            val bundle = Bundle().apply { putString("playerId", player.id) }
            findNavController().navigate(R.id.action_home_to_player_details, bundle)
        }
        binding.rvHomePlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playerAdapter
        }

        umpireAdapter = UmpireAdapter(emptyList()) { umpire ->
            val bundle = Bundle().apply { putString("umpireId", umpire.id) }
            findNavController().navigate(R.id.action_home_to_umpire_details, bundle)
        }
        binding.rvHomeUmpires.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = umpireAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.featuredPlayers.observe(viewLifecycleOwner) { players ->
            playerAdapter.updateData(players)
        }
        viewModel.topUmpires.observe(viewLifecycleOwner) { umpires ->
            umpireAdapter.updateData(umpires)
        }
    }

    private fun setupClickListeners() {
        // Profile icon
        binding.ivHeaderProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }

        // Notification Bell icon
        binding.ivHeaderNotification.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Notifications")
                .setMessage("No new match notifications right now.\nYou're all caught up with live scores and team updates!")
                .setPositiveButton("OK", null)
                .show()
        }

        // Live Match Hero Button (View Live)
        binding.btnQuickHost.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M501") }
            findNavController().navigate(R.id.action_home_to_match_scoring, bundle)
        }

        // Quick Actions
        // 1. Start Match -> Open Create Match screen
        binding.cardTournaments.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_create_tournament)
        }

        // 2. Live Scoring -> Open Live Scoring screen
        binding.cardHireUmpire.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M501") }
            findNavController().navigate(R.id.action_home_to_match_scoring, bundle)
        }

        // 3. Find Players -> Open Players list screen
        binding.cardBookPlayer.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_players)
        }

        // Upcoming Matches section
        binding.tvSeeAllUpcoming.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_bookings)
        }
        binding.cardFeaturedUpcomingMatch.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M502") }
            findNavController().navigate(R.id.action_home_to_match_details, bundle)
        }

        // Recent Results section
        binding.tvSeeAllResults.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_fixtures)
        }
        binding.cardRecentResult.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M501") }
            findNavController().navigate(R.id.action_home_to_match_details, bundle)
        }

        // See All links
        binding.tvSeeAllPlayers.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_players)
        }
        binding.tvSeeAllUmpires.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_umpires)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
