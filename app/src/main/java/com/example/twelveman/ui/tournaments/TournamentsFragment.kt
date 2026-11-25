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

            // Dynamically bind live tournaments from DB into the top cards
            val featured = list.getOrNull(0)
            if (featured != null) {
                binding.cardFeaturedTournament.visibility = View.VISIBLE
                binding.tvFeaturedTournamentTitle.text = featured.name
                binding.tvFeaturedTournamentSub.text = "${featured.format} • ${featured.totalTeams} Teams • ${featured.location}"
                binding.cardFeaturedTournament.setOnClickListener {
                    val bundle = Bundle().apply { putString("tournamentId", featured.id) }
                    findNavController().navigate(R.id.action_tournaments_to_details, bundle)
                }
            } else {
                binding.cardFeaturedTournament.visibility = View.GONE
            }

            val t1 = list.getOrNull(1)
            if (t1 != null) {
                binding.cardSampleCityCup.visibility = View.VISIBLE
                binding.tvMyTournament1Title.text = t1.name
                binding.tvMyTournament1Sub.text = "${t1.format} • ${t1.totalTeams} Teams • Status: ${t1.status}"
                binding.cardSampleCityCup.setOnClickListener {
                    val bundle = Bundle().apply { putString("tournamentId", t1.id) }
                    findNavController().navigate(R.id.action_tournaments_to_details, bundle)
                }
            } else {
                binding.cardSampleCityCup.visibility = View.GONE
            }

            val t2 = list.getOrNull(2)
            if (t2 != null) {
                binding.cardSampleChampionsTrophy.visibility = View.VISIBLE
                binding.tvUpcomingTournament1Title.text = t2.name
                binding.tvUpcomingTournament1Sub.text = "${t2.format} • ${t2.totalTeams} Teams • Status: ${t2.status}"
                binding.cardSampleChampionsTrophy.setOnClickListener {
                    val bundle = Bundle().apply { putString("tournamentId", t2.id) }
                    findNavController().navigate(R.id.action_tournaments_to_details, bundle)
                }
            } else {
                binding.cardSampleChampionsTrophy.visibility = View.GONE
            }
        }

        // Tournament Organizer Hub Navigation
        binding.cardOrganizerHub.setOnClickListener {
            findNavController().navigate(R.id.action_tournaments_to_organizer)
        }
        binding.btnOpenOrganizerDashboard.setOnClickListener {
            findNavController().navigate(R.id.action_tournaments_to_organizer)
        }
        binding.btnCreateNewTournamentQuick.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isTournament", true) }
            findNavController().navigate(R.id.action_tournaments_to_create, bundle)
        }

        // Host Tournament Floating Action Button (FAB)
        binding.fabCreateTournament.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isTournament", true) }
            findNavController().navigate(R.id.action_tournaments_to_create, bundle)
        }

        // Notifications
        binding.ivTournamentsNotification.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Tournament Notifications")
                .setMessage("No unread alerts. You will receive live toss results, fixture schedules, and match score updates here.")
                .setPositiveButton("OK", null)
                .show()
        }

        // View All links - scroll to list
        binding.tvViewAllMyTournaments.setOnClickListener {
            binding.rvTournaments.smoothScrollToPosition(0)
            Toast.makeText(requireContext(), "Showing all tournaments", Toast.LENGTH_SHORT).show()
        }
        binding.tvViewAllUpcomingTournaments.setOnClickListener {
            binding.rvTournaments.smoothScrollToPosition(0)
            Toast.makeText(requireContext(), "Showing upcoming sanctioned tournaments", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
