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
import com.example.twelveman.databinding.FragmentMatchFixturesBinding

class MatchFixturesFragment : Fragment() {
    private var _binding: FragmentMatchFixturesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private lateinit var adapter: MatchAdapter
    private var tournamentId: String = ""

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
            adapter.updateData(matches)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}