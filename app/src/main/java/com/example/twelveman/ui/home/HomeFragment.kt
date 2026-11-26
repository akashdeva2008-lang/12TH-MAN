package com.example.twelveman.ui.home

import android.os.Bundle
import android.os.SystemClock
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
    private var lastNavTime: Long = 0L

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

    private fun safeNavigate(actionId: Int, bundle: Bundle? = null) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavTime < 600L) return
        lastNavTime = now
        try {
            findNavController().navigate(actionId, bundle)
        } catch (e: Exception) {
            // Prevent crash on multiple quick clicks or if destination state has changed
        }
    }

    private fun setupRecyclerViews() {
        playerAdapter = PlayerAdapter(emptyList()) { player ->
            val bundle = Bundle().apply { putString("playerId", player.id) }
            safeNavigate(R.id.action_home_to_player_details, bundle)
        }
        binding.rvHomePlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playerAdapter
        }

        umpireAdapter = UmpireAdapter(emptyList()) { umpire ->
            val bundle = Bundle().apply { putString("umpireId", umpire.id) }
            safeNavigate(R.id.action_home_to_umpire_details, bundle)
        }
        binding.rvHomeUmpires.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = umpireAdapter
        }
    }

    private fun observeViewModel() {
        // 1. Featured Players
        viewModel.featuredPlayers.observe(viewLifecycleOwner) { players ->
            playerAdapter.updateData(players)
        }

        // 2. Top Umpires
        viewModel.topUmpires.observe(viewLifecycleOwner) { umpires ->
            umpireAdapter.updateData(umpires)
        }

        // 3. Live Match Hero Card
        viewModel.liveMatch.observe(viewLifecycleOwner) { match ->
            if (match != null) {
                binding.cardLiveMatchHero.visibility = View.VISIBLE
                binding.tvLiveMatchTeams.text = "${match.teamA} vs ${match.teamB}"
                binding.tvLiveMatchScore.text = if (match.scoreA.isNotBlank()) "Score: ${match.scoreA}" else ""
                binding.tvLiveMatchVenue.text = "${match.format} • ${match.venue}"
                binding.btnQuickHost.setOnClickListener {
                    val bundle = Bundle().apply { putString("matchId", match.id) }
                    safeNavigate(R.id.action_home_to_match_scoring, bundle)
                }
            } else {
                binding.tvLiveMatchTeams.text = "No Live Matches Now"
                binding.tvLiveMatchScore.text = "Matches will appear live once started"
                binding.tvLiveMatchVenue.text = "Check Upcoming Matches below"
            }
        }

        // 4. Featured Upcoming Match Card
        viewModel.featuredUpcomingMatch.observe(viewLifecycleOwner) { match ->
            if (match != null) {
                binding.cardFeaturedUpcomingMatch.visibility = View.VISIBLE
                binding.tvUpcomingTeamA.text = match.teamA
                binding.tvUpcomingTeamB.text = match.teamB
                binding.tvUpcomingDateTime.text = "${match.date} • ${match.time}"
                binding.tvUpcomingVenue.text = match.venue
                binding.cardFeaturedUpcomingMatch.setOnClickListener {
                    val bundle = Bundle().apply { putString("matchId", match.id) }
                    safeNavigate(R.id.action_home_to_match_details, bundle)
                }
            } else {
                binding.cardFeaturedUpcomingMatch.visibility = View.GONE
            }
        }

        // 5. Recent Result Card
        viewModel.recentResult.observe(viewLifecycleOwner) { match ->
            if (match != null) {
                binding.cardRecentResult.visibility = View.VISIBLE
                binding.tvRecentTeamA.text = match.teamA
                binding.tvRecentTeamB.text = match.teamB
                binding.tvRecentResultText.text = if (match.result.isNotBlank() && !match.result.equals("Scheduled", ignoreCase = true)) match.result else "${match.scoreA} vs ${match.scoreB}"
                binding.cardRecentResult.setOnClickListener {
                    val bundle = Bundle().apply { putString("matchId", match.id) }
                    safeNavigate(R.id.action_home_to_match_details, bundle)
                }
            } else {
                binding.cardRecentResult.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        // Profile icon
        binding.ivHeaderProfile.setOnClickListener {
            safeNavigate(R.id.action_home_to_profile)
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
            safeNavigate(R.id.action_home_to_match_scoring, bundle)
        }

        // Quick Actions
        // 1. Start Match -> Open Create Match screen
        binding.cardTournaments.setOnClickListener {
            safeNavigate(R.id.action_home_to_create_tournament)
        }

        // 2. Live Scoring -> Open Live Scoring screen
        binding.cardLiveScoring.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M501") }
            safeNavigate(R.id.action_home_to_match_scoring, bundle)
        }

        // 3. Find Players -> Open Players list screen
        binding.cardBookPlayer.setOnClickListener {
            safeNavigate(R.id.action_home_to_players)
        }

        // 4. Book Umpires -> Open Dedicated Umpire Booking Dashboard
        binding.cardHireUmpire.setOnClickListener {
            safeNavigate(R.id.action_home_to_umpire_dashboard)
        }

        // 5. 12THMAN GPT-1 Cricket AI Assistant
        binding.cardAiAssistant.setOnClickListener {
            safeNavigate(R.id.action_home_to_ai_assistant)
        }

        // Upcoming Matches section -> Navigate to My Matches Screen with Upcoming Tab & Scheduled Matches
        binding.tvSeeAllUpcoming.setOnClickListener {
            val bundle = Bundle().apply {
                putString("targetCategory", "MATCHES")
                putString("targetTab", "UPCOMING")
            }
            safeNavigate(R.id.action_home_to_bookings, bundle)
        }
        binding.cardFeaturedUpcomingMatch.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M502") }
            safeNavigate(R.id.action_home_to_match_details, bundle)
        }

        // Recent Results section -> Navigate to My Matches Screen with Completed Tab & Scheduled Matches
        binding.tvSeeAllResults.setOnClickListener {
            val bundle = Bundle().apply {
                putString("targetCategory", "MATCHES")
                putString("targetTab", "COMPLETED")
            }
            safeNavigate(R.id.action_home_to_bookings, bundle)
        }
        binding.cardRecentResult.setOnClickListener {
            val bundle = Bundle().apply { putString("matchId", "M501") }
            safeNavigate(R.id.action_home_to_match_details, bundle)
        }

        // See All links
        binding.tvSeeAllPlayers.setOnClickListener {
            safeNavigate(R.id.action_home_to_players)
        }
        binding.tvSeeAllUmpires.setOnClickListener {
            safeNavigate(R.id.action_home_to_umpire_dashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
