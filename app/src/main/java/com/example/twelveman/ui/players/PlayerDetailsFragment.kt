package com.example.twelveman.ui.players

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentPlayerDetailsBinding

class PlayerDetailsFragment : Fragment() {
    private var _binding: FragmentPlayerDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayersViewModel by viewModels()
    private var playerId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            playerId = it.getString("playerId", "P101")
        }

        viewModel.loadPlayerDetails(playerId)
        viewModel.selectedPlayer.observe(viewLifecycleOwner) { player ->
            player?.let { p ->
                binding.tvDetailName.text = p.name
                binding.tvDetailRole.text = "${p.role} • ${p.location}"
                binding.tvDetailMatches.text = p.matchesPlayed.toString()
                binding.tvDetailRuns.text = p.runsScored.toString()
                binding.tvDetailWickets.text = p.wicketsTaken.toString()
                binding.tvDetailBio.text = p.bio

                // Share Button Listener
                val shareAction = View.OnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "12THMAN Cricket Profile: ${p.name}"
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "🏏 Check out ${p.name} on 12THMAN Cricket Management App!\n\n" +
                                    "Role: ${p.role}\n" +
                                    "Matches: ${p.matchesPlayed}\n" +
                                    "Runs: ${p.runsScored}\n" +
                                    "Wickets: ${p.wicketsTaken}\n" +
                                    "Fee per Match: ₹${p.pricePerMatch}\n" +
                                    "Location: ${p.location}\n\n" +
                                    "Book or Hire on 12THMAN: https://12thman.cricket/player/${p.id}"
                        )
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Player Profile"))
                }

                binding.btnShareProfile.setOnClickListener(shareAction)
                binding.ivPlayerDetailShare.setOnClickListener(shareAction)

                // View Full Stats
                binding.btnViewFullStats.setOnClickListener {
                    val avgRuns = if (p.matchesPlayed > 0) String.format("%.1f", p.runsScored.toDouble() / p.matchesPlayed) else "0.0"
                    AlertDialog.Builder(requireContext())
                        .setTitle("Full Career Stats • ${p.name}")
                        .setMessage(
                            "Category: ${p.role}\n" +
                            "Matches Played: ${p.matchesPlayed}\n" +
                            "Total Runs: ${p.runsScored}\n" +
                            "Total Wickets: ${p.wicketsTaken}\n\n" +
                            "• Batting Average: $avgRuns\n" +
                            "• Strike Rate: 142.6\n" +
                            "• Highest Score: 114*\n" +
                            "• Centuries (100s): 6\n" +
                            "• Fifties (50s): 28\n" +
                            "• Bowling Economy: 6.95\n" +
                            "• Best Bowling: 4/16\n" +
                            "• Catches / Dismissals: 58\n" +
                            "• Player Rating: ${p.rating} ★"
                        )
                        .setPositiveButton("Close", null)
                        .show()
                }

                // Current Form
                binding.btnCurrentForm.setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Current Form Guide • ${p.name}")
                        .setMessage(
                            "Recent 5 Matches Form:\n\n" +
                            "1. 74* (44 balls) vs Mumbai Superstars • MOM\n" +
                            "2. 58 (36 balls) vs Blue Strikers\n" +
                            "3. 24 (16 balls) & 2/20 vs Delhi Titans\n" +
                            "4. 88* (52 balls) vs Chennai Kings\n" +
                            "5. 42 (28 balls) vs Gujarat Lions\n\n" +
                            "• Current Form Index: 9.4 / 10 (EXCELLENT)\n" +
                            "• Strike Rate (Last 5): 156.2\n" +
                            "• Boundary Frequency: Every 4.2 balls"
                        )
                        .setPositiveButton("Close", null)
                        .show()
                }
            }
        }

        binding.btnBookPlayer.setOnClickListener {
            val bundle = Bundle().apply {
                putString("itemId", playerId)
                putString("itemType", "PLAYER")
            }
            findNavController().navigate(R.id.action_player_details_to_booking, bundle)
        }

        binding.ivPlayerDetailBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
