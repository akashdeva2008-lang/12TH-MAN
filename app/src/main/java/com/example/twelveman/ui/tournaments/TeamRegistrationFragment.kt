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
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.FragmentTeamRegistrationBinding
import java.util.UUID

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

        val currentUser = SessionManager.getInstance(requireContext()).getCurrentUser()
        if (currentUser != null) {
            if (binding.etCaptainName.text.isNullOrBlank() && currentUser.name.isNotBlank()) {
                binding.etCaptainName.setText(currentUser.name)
            }
            if (binding.etCaptainPhone.text.isNullOrBlank() && currentUser.phone.isNotBlank()) {
                binding.etCaptainPhone.setText(currentUser.phone)
            }
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

            if (teamName.isBlank()) {
                binding.etTeamName.error = "Team name is required"
                return@setOnClickListener
            }
            if (captainName.isBlank()) {
                binding.etCaptainName.error = "Captain name is required"
                return@setOnClickListener
            }

            val rawNames = if (playerListStr.isNotBlank()) {
                playerListStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
            } else {
                listOf(captainName)
            }

            val squadItems = rawNames.mapIndexed { idx, pName ->
                val isCap = pName.equals(captainName, ignoreCase = true) || (idx == 0 && rawNames.none { it.equals(captainName, ignoreCase = true) })
                val isVc = viceCaptain.isNotBlank() && pName.equals(viceCaptain, ignoreCase = true)
                TeamPlayerItem(
                    id = UUID.randomUUID().toString(),
                    playerName = pName,
                    role = "Batsman",
                    jerseyNumber = (idx + 1).toString(),
                    contact = if (isCap) phone else "",
                    isCaptain = isCap,
                    isViceCaptain = isVc
                )
            }.toMutableList()

            if (squadItems.none { it.isCaptain }) {
                squadItems.add(
                    0,
                    TeamPlayerItem(
                        id = UUID.randomUUID().toString(),
                        playerName = captainName,
                        role = "Batsman",
                        jerseyNumber = "7",
                        contact = phone,
                        isCaptain = true,
                        isViceCaptain = false
                    )
                )
            }

            val shortName = teamName.split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .let { parts ->
                    if (parts.size > 1) (parts[0].take(1) + parts[1].take(2)).uppercase()
                    else teamName.take(3).uppercase()
                }

            val email = currentUser?.email?.takeIf { it.isNotBlank() } ?: "captain@12thman.com"

            val newTeam = Team(
                id = "",
                tournamentId = tournamentId,
                teamName = teamName,
                shortName = shortName,
                captainName = captainName,
                captainPhone = phone,
                captainEmail = email,
                viceCaptain = viceCaptain,
                managerName = manager,
                description = description,
                city = "Mumbai",
                logoUrl = "RED",
                approvalStatus = "APPROVED",
                playerNames = squadItems.map { it.playerName },
                players = squadItems,
                playerCount = squadItems.size,
                isCustomCreated = true
            )

            binding.btnSubmitTeamReg.isEnabled = false
            binding.btnSubmitTeamReg.text = "Saving Team..."

            viewModel.createOrUpdateTeam(newTeam, isEdit = false) { success, message, _ ->
                if (!isAdded) return@createOrUpdateTeam
                binding.btnSubmitTeamReg.isEnabled = true
                binding.btnSubmitTeamReg.text = "Submit Team Registration"
                if (success) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Unable to Save Team")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
