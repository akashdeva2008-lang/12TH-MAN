package com.example.twelveman.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.databinding.FragmentTeamRegistrationBinding

class TeamRegistrationFragment : Fragment() {
    private var _binding: FragmentTeamRegistrationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()
    private var tournamentId: String = "T301"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val tid = it.getString("tournamentId")
            if (!tid.isNullOrBlank()) tournamentId = tid
        }

        binding.ivTeamRegBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSubmitTeamReg.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            val captainName = binding.etCaptainName.text.toString().trim()
            val phone = binding.etCaptainPhone.text.toString().trim()
            val viceCaptain = binding.etViceCaptain.text.toString().trim()
            val manager = binding.etManagerName.text.toString().trim()
            val description = binding.etTeamBio.text.toString().trim()
            val playerListStr = binding.etPlayerList.text.toString().trim()

            if (teamName.isBlank() || captainName.isBlank()) {
                Toast.makeText(requireContext(), "Team name and captain name are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val players = if (playerListStr.isNotBlank()) {
                playerListStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf(captainName)
            }

            binding.btnSubmitTeamReg.isEnabled = false

            viewModel.addOrganizerTeam(
                tournamentId = tournamentId,
                teamName = teamName,
                captainName = captainName,
                phone = phone,
                email = "captain@12thman.com",
                viceCaptain = viceCaptain,
                manager = manager,
                description = description,
                players = players
            ) { success ->
                binding.btnSubmitTeamReg.isEnabled = true
                if (success) {
                    Toast.makeText(requireContext(), "Team '$teamName' registered successfully!", Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(requireContext(), "Registration failed or duplicate team name in this tournament", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
