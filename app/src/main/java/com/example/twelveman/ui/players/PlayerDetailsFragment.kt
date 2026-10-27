package com.example.twelveman.ui.players

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            }
        }

        binding.btnBookPlayer.setOnClickListener {
            val bundle = Bundle().apply {
                putString("itemId", playerId)
                putString("itemType", "PLAYER")
            }
            findNavController().navigate(R.id.action_player_details_to_booking, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}