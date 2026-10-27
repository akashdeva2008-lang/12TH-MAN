package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.fabCreateTournament.setOnClickListener {
            findNavController().navigate(R.id.action_tournaments_to_create)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}