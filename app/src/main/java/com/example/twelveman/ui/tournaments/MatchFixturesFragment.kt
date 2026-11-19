package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.data.model.Match
import com.example.twelveman.databinding.FragmentMatchFixturesBinding

class MatchFixturesFragment : Fragment() {
    private var _binding: FragmentMatchFixturesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private lateinit var adapter: MatchAdapter
    private var tournamentId: String = ""
    private var allMatches: List<Match> = emptyList()
    private var activeFilter = "UPCOMING"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchFixturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            tournamentId = it.getString("tournamentId", "")
        }

        adapter = MatchAdapter(emptyList()) { match ->
            val bundle = Bundle().apply { putString("matchId", match.id) }
            findNavController().navigate(R.id.action_fixtures_to_match_details, bundle)
        }

        binding.rvMatchFixtures.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@MatchFixturesFragment.adapter
        }

        viewModel.fetchMatches(tournamentId)
        viewModel.matchesList.observe(viewLifecycleOwner) { matches ->
            allMatches = matches
            applyTabFilter(activeFilter)
        }

        // Tab click listeners
        binding.tabUpcoming.setOnClickListener {
            activeFilter = "UPCOMING"
            applyTabFilter("UPCOMING")
        }

        binding.tabLive.setOnClickListener {
            activeFilter = "LIVE"
            applyTabFilter("LIVE")
        }

        binding.tabCompleted.setOnClickListener {
            activeFilter = "COMPLETED"
            applyTabFilter("COMPLETED")
        }

        // Search icon
        binding.ivMatchSearch.setOnClickListener {
            val etSearch = EditText(requireContext()).apply {
                hint = "Enter team name or venue"
            }
            AlertDialog.Builder(requireContext())
                .setTitle("Search Matches")
                .setView(etSearch)
                .setPositiveButton("Search") { _, _ ->
                    val query = etSearch.text.toString().trim()
                    val filtered = allMatches.filter {
                        it.teamA.contains(query, ignoreCase = true) ||
                        it.teamB.contains(query, ignoreCase = true) ||
                        it.venue.contains(query, ignoreCase = true)
                    }
                    adapter.updateData(filtered)
                    Toast.makeText(requireContext(), "Found ${filtered.size} matches", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Reset") { _, _ ->
                    applyTabFilter(activeFilter)
                }
                .show()
        }

        // Filter icon
        binding.ivMatchFilter.setOnClickListener {
            val stages = arrayOf("All Stages", "Group Stage", "Quarter Final", "Semi Final", "Final")
            AlertDialog.Builder(requireContext())
                .setTitle("Filter Matches by Stage")
                .setItems(stages) { _, which ->
                    val selectedStage = stages[which]
                    if (which == 0) {
                        applyTabFilter(activeFilter)
                    } else {
                        val filtered = allMatches.filter { it.stage.equals(selectedStage, ignoreCase = true) }
                        adapter.updateData(filtered)
                        Toast.makeText(requireContext(), "Showing $selectedStage matches (${filtered.size})", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }
    }

    private fun applyTabFilter(filter: String) {
        val white = ContextCompat.getColor(requireContext(), R.color.pure_white)
        val textSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        // Reset tab styles
        binding.tabUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabUpcoming.setTextColor(textSecondary)
        binding.tabLive.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabLive.setTextColor(textSecondary)
        binding.tabCompleted.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
        binding.tabCompleted.setTextColor(textSecondary)

        val filteredList = when (filter) {
            "UPCOMING" -> {
                binding.tabUpcoming.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabUpcoming.setTextColor(white)
                allMatches.filter { it.status.equals("Upcoming", ignoreCase = true) || it.status.equals("Scheduled", ignoreCase = true) }
            }
            "LIVE" -> {
                binding.tabLive.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabLive.setTextColor(white)
                allMatches.filter { it.status.equals("Live", ignoreCase = true) || it.status.equals("In Progress", ignoreCase = true) }
            }
            "COMPLETED" -> {
                binding.tabCompleted.setBackgroundResource(R.drawable.bg_badge_pill_active)
                binding.tabCompleted.setTextColor(white)
                allMatches.filter { it.status.equals("Completed", ignoreCase = true) || it.status.equals("Result", ignoreCase = true) }
            }
            else -> allMatches
        }

        adapter.updateData(if (filteredList.isNotEmpty()) filteredList else allMatches)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
