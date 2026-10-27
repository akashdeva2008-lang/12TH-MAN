package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentCreateTournamentBinding

class CreateTournamentFragment : Fragment() {
    private var _binding: FragmentCreateTournamentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTournamentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPublishTournament.setOnClickListener {
            val name = binding.etTournamentName.text.toString().trim()
            val format = binding.etFormat.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val entryFee = binding.etEntryFee.text.toString().toIntOrNull() ?: 10000
            val prize = binding.etPrizeMoney.text.toString().toIntOrNull() ?: 100000

            if (name.isNotBlank() && location.isNotBlank()) {
                viewModel.createTournament(
                    name = name,
                    format = format,
                    location = location,
                    startDate = "15 Mar 2026",
                    endDate = "22 Mar 2026",
                    totalTeams = 16,
                    entryFee = entryFee,
                    prizeMoney = prize,
                    organizerName = "Mumbai Cricket Club",
                    organizerContact = "+91 98200 11223",
                    description = "Official T20 corporate cup organized via 12THMAN."
                )
                Toast.makeText(requireContext(), "Tournament Published Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_create_to_tournaments)
            } else {
                Toast.makeText(requireContext(), "Please enter tournament name and location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}