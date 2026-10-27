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
    private var tournamentId: String = ""

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
            tournamentId = it.getString("tournamentId", "T301")
        }

        binding.btnSubmitTeamReg.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            val captainName = binding.etCaptainName.text.toString().trim()
            val phone = binding.etCaptainPhone.text.toString().trim()
            val playerListStr = binding.etPlayerList.text.toString().trim()

            if (teamName.isNotBlank() && captainName.isNotBlank()) {
                val players = playerListStr.split(",").map { it.trim() }
                viewModel.registerTeam(tournamentId, teamName, captainName, phone, "captain@example.com", players)
                Toast.makeText(requireContext(), "Team Registered Successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Please fill Team & Captain Name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}