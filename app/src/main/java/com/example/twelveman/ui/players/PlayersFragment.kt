package com.example.twelveman.ui.players

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentPlayersBinding

class PlayersFragment : Fragment() {
    private var _binding: FragmentPlayersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayersViewModel by viewModels()
    private lateinit var adapter: PlayerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlayerAdapter(emptyList()) { player ->
            val bundle = Bundle().apply { putString("playerId", player.id) }
            findNavController().navigate(R.id.action_players_to_details, bundle)
        }

        binding.rvPlayers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@PlayersFragment.adapter
        }

        viewModel.playersList.observe(viewLifecycleOwner) { players ->
            adapter.updateData(players)
        }

        binding.etSearchPlayers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupRoles.setOnCheckedStateChangeListener { _, checkedIds ->
            val role = when {
                checkedIds.contains(R.id.chipBatsman) -> "Batsman"
                checkedIds.contains(R.id.chipBowler) -> "Bowler"
                checkedIds.contains(R.id.chipAllRounder) -> "All-Rounder"
                checkedIds.contains(R.id.chipWicketKeeper) -> "Wicket Keeper"
                else -> "All"
            }
            viewModel.filterByRole(role)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}