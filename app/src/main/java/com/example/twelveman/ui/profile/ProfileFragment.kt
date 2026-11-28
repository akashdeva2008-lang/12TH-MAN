package com.example.twelveman.ui.profile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.DialogMyRegisteredTeamsBinding
import com.example.twelveman.databinding.FragmentProfileBinding
import com.example.twelveman.ui.tournaments.TeamSquadFormAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.UUID

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvProfileName.text = user.name
                binding.tvProfileRole.text = "Role: ${user.role}"
                binding.tvProfileEmail.text = "Email: ${user.email}"
                binding.tvProfilePhone.text = if (user.phone.isNotBlank()) "Phone: ${user.phone}" else "Phone: Not provided"
            } else {
                binding.tvProfileName.text = "Guest User"
                binding.tvProfileRole.text = "Role: Guest"
                binding.tvProfileEmail.text = "Email: Not signed in"
                binding.tvProfilePhone.text = "Phone: -"
            }
        }

        binding.ivProfileBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Settings navigation
        val goToSettings = View.OnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        binding.btnGoToSettings.setOnClickListener(goToSettings)
        binding.ivProfileTopSettings.setOnClickListener(goToSettings)

        // My Teams -> Show actual registered teams from DB with View, Edit, Delete, and Create
        binding.rowMyTeams.setOnClickListener {
            showMyRegisteredTeamsDialog()
        }

        // My Statistics / Role Dashboard
        binding.rowMyStatistics.setOnClickListener {
            val user = viewModel.user.value
            when (user?.role?.uppercase()) {
                "PLAYER" -> findNavController().navigate(R.id.action_profile_to_player_dashboard)
                "UMPIRE" -> findNavController().navigate(R.id.action_profile_to_umpire_dashboard)
                else -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Join as Player or Umpire")
                        .setMessage("Sign in as a Player or Umpire to view your dedicated role dashboard and booking requests.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }

        // My Matches
        binding.rowMyMatches.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_bookings)
        }

        // Help and Support
        binding.rowHelpSupport.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("12THMAN Support & Helpdesk")
                .setMessage(
                    "Need assistance with booking players, umpires, or tournament scoring?\n\n" +
                    "• Email Support: support@12thman.cricket\n" +
                    "• WhatsApp Hotline: +91 98200 11223\n" +
                    "• Office Hours: Monday - Saturday (9:00 AM - 8:00 PM)\n" +
                    "• Dispute Resolution: grievances@12thman.cricket\n\n" +
                    "Version 1.0.0 (Official Stable Release)"
                )
                .setPositiveButton("OK", null)
                .show()
        }

        // Logout with Confirmation Dialog
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to sign out of 12THMAN?")
                .setPositiveButton("Logout") { _, _ ->
                    viewModel.logout()
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_profile_to_login)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showMyRegisteredTeamsDialog() {
        viewModel.fetchMyRegisteredTeams()

        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogMyRegisteredTeamsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val adapter = MyRegisteredTeamAdapter(
            teams = emptyList(),
            onEditTeam = { teamToEdit ->
                showCreateOrEditTeamDialog(teamToEdit)
            },
            onDeleteTeam = { teamToDelete ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Team")
                    .setMessage("Are you sure you want to delete '${teamToDelete.teamName}'? Teams linked to matches cannot be deleted.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTeam(teamToDelete.id) { success, msg ->
                            if (!isAdded) return@deleteTeam
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        dialogBinding.rvMyRegisteredTeams.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvMyRegisteredTeams.adapter = adapter

        viewModel.isLoadingTeams.observe(viewLifecycleOwner) { loading ->
            dialogBinding.pbLoadingTeams.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.myRegisteredTeams.observe(viewLifecycleOwner) { teams ->
            adapter.updateData(teams)
            val count = teams.size
            dialogBinding.tvMyTeamsCount.text = if (count > 0) "$count Registered Teams" else "No Registered Teams"
            dialogBinding.tvFilterBadge.text = if (count > 0) "Showing $count Teams from Database" else "No Teams Found"

            if (teams.isEmpty() && viewModel.isLoadingTeams.value != true) {
                dialogBinding.layoutEmptyTeams.visibility = View.VISIBLE
                dialogBinding.rvMyRegisteredTeams.visibility = View.GONE
            } else {
                dialogBinding.layoutEmptyTeams.visibility = View.GONE
                dialogBinding.rvMyRegisteredTeams.visibility = View.VISIBLE
            }
        }

        val createTeamClick = View.OnClickListener {
            showCreateOrEditTeamDialog(null)
        }
        dialogBinding.btnRegisterNewTeamDialog.setOnClickListener(createTeamClick)
        dialogBinding.btnEmptyRegisterTeam.setOnClickListener(createTeamClick)
        dialogBinding.ivCloseMyTeamsDialog.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showCreateOrEditTeamDialog(existingTeam: Team?) {
        val formView = layoutInflater.inflate(R.layout.dialog_create_team, null)

        val tvTitle = formView.findViewById<TextView>(R.id.tvCreateTeamTitle)
        val btnClose = formView.findViewById<ImageView>(R.id.btnCloseCreateTeam)
        val ivLogoPreview = formView.findViewById<ImageView>(R.id.ivFormLogoPreview)
        val tvBadgePreview = formView.findViewById<TextView>(R.id.tvFormBadgePreview)

        val pRed = formView.findViewById<View>(R.id.presetColorRed)
        val pBlue = formView.findViewById<View>(R.id.presetColorBlue)
        val pGold = formView.findViewById<View>(R.id.presetColorGold)
        val pGreen = formView.findViewById<View>(R.id.presetColorGreen)
        val pDark = formView.findViewById<View>(R.id.presetColorDark)

        val etTeamName = formView.findViewById<EditText>(R.id.etFormTeamName)
        val etShortName = formView.findViewById<EditText>(R.id.etFormShortName)
        val etCaptainName = formView.findViewById<EditText>(R.id.etFormCaptainName)
        val etCaptainPhone = formView.findViewById<EditText>(R.id.etFormCaptainPhone)
        val etViceCaptain = formView.findViewById<EditText>(R.id.etFormViceCaptain)
        val etLocation = formView.findViewById<EditText>(R.id.etFormLocation)
        val etDescription = formView.findViewById<EditText>(R.id.etFormDescription)

        val tvPlayersTitle = formView.findViewById<TextView>(R.id.tvFormPlayersCountTitle)
        val btnAddPlayer = formView.findViewById<Button>(R.id.btnAddPlayerToForm)
        val rvPlayers = formView.findViewById<RecyclerView>(R.id.rvFormPlayers)
        val btnCancel = formView.findViewById<Button>(R.id.btnCancelTeamForm)
        val btnSave = formView.findViewById<Button>(R.id.btnSaveTeamForm)
        val layoutSaving = formView.findViewById<View>(R.id.layoutSavingIndicator)
        val tvSavingMsg = formView.findViewById<TextView>(R.id.tvSavingMessage)

        val isEdit = (existingTeam != null)
        tvTitle.text = if (isEdit) "Edit Team: ${existingTeam!!.teamName}" else "Create New Team"

        var selectedLogoColor = existingTeam?.logoUrl ?: "RED"
        fun applyLogoColor(tag: String) {
            selectedLogoColor = tag
            val color = when (tag) {
                "BLUE" -> Color.parseColor("#1565C0")
                "GOLD" -> Color.parseColor("#FFB300")
                "GREEN" -> Color.parseColor("#2E7D32")
                "DARK" -> Color.parseColor("#212121")
                else -> Color.parseColor("#E50920")
            }
            ivLogoPreview.setColorFilter(color)
        }
        applyLogoColor(selectedLogoColor)
        pRed.setOnClickListener { applyLogoColor("RED") }
        pBlue.setOnClickListener { applyLogoColor("BLUE") }
        pGold.setOnClickListener { applyLogoColor("GOLD") }
        pGreen.setOnClickListener { applyLogoColor("GREEN") }
        pDark.setOnClickListener { applyLogoColor("DARK") }

        val squadPlayers = mutableListOf<TeamPlayerItem>()
        if (existingTeam != null) {
            etTeamName.setText(existingTeam.teamName)
            etShortName.setText(existingTeam.shortName)
            etCaptainName.setText(existingTeam.captainName)
            etCaptainPhone.setText(existingTeam.captainPhone)
            etViceCaptain.setText(existingTeam.viceCaptain)
            etLocation.setText(existingTeam.city)
            etDescription.setText(existingTeam.description)
            tvBadgePreview.text = existingTeam.shortName.take(3)
            if (existingTeam.players.isNotEmpty()) {
                squadPlayers.addAll(existingTeam.players.map { it.copy() })
            } else if (existingTeam.playerNames.isNotEmpty()) {
                existingTeam.playerNames.forEachIndexed { i, pName ->
                    squadPlayers.add(
                        TeamPlayerItem(
                            playerName = pName,
                            role = "Batsman",
                            jerseyNumber = (i + 1).toString(),
                            isCaptain = (i == 0)
                        )
                    )
                }
            }
        } else {
            val user = SessionManager.getInstance(requireContext()).getCurrentUser()
            if (user != null) {
                if (user.name.isNotBlank()) etCaptainName.setText(user.name)
                if (user.phone.isNotBlank()) etCaptainPhone.setText(user.phone)
            }
            etLocation.setText("Mumbai")
        }

        lateinit var playersAdapter: TeamSquadFormAdapter
        fun updatePlayersUi() {
            tvPlayersTitle.text = "Squad Players (${squadPlayers.size})"
            playersAdapter.notifyDataSetChanged()
        }
        playersAdapter = TeamSquadFormAdapter(squadPlayers) { removedPos ->
            squadPlayers.removeAt(removedPos)
            updatePlayersUi()
        }
        rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        rvPlayers.adapter = playersAdapter
        updatePlayersUi()

        val dialog = AlertDialog.Builder(requireContext()).setView(formView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnAddPlayer.setOnClickListener {
            val pView = layoutInflater.inflate(R.layout.dialog_add_player, null)
            val etPName = pView.findViewById<EditText>(R.id.etDialogPlayerName)
            val etPRole = pView.findViewById<EditText>(R.id.etDialogPlayerRole)
            val etPJersey = pView.findViewById<EditText>(R.id.etDialogPlayerJersey)
            val etPContact = pView.findViewById<EditText>(R.id.etDialogPlayerContact)
            val cbCap = pView.findViewById<CheckBox>(R.id.cbIsCaptain)
            val cbVc = pView.findViewById<CheckBox>(R.id.cbIsViceCaptain)
            val roles = arrayOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper")
            etPRole.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Select Player Role")
                    .setItems(roles) { _, which -> etPRole.setText(roles[which]) }
                    .show()
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Add Squad Player")
                .setView(pView)
                .setPositiveButton("Add") { _, _ ->
                    val pName = etPName.text.toString().trim()
                    if (pName.isBlank()) {
                        Toast.makeText(requireContext(), "Player name is required", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (squadPlayers.any { it.playerName.equals(pName, ignoreCase = true) }) {
                        Toast.makeText(requireContext(), "Player '$pName' is already in the squad", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (cbCap.isChecked) squadPlayers.forEach { it.isCaptain = false }
                    if (cbVc.isChecked) squadPlayers.forEach { it.isViceCaptain = false }
                    squadPlayers.add(
                        TeamPlayerItem(
                            id = UUID.randomUUID().toString(),
                            playerName = pName,
                            role = etPRole.text.toString().trim().ifBlank { "Batsman" },
                            jerseyNumber = etPJersey.text.toString().trim(),
                            contact = etPContact.text.toString().trim(),
                            isCaptain = cbCap.isChecked,
                            isViceCaptain = cbVc.isChecked
                        )
                    )
                    updatePlayersUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnSave.setOnClickListener {
            val name = etTeamName.text.toString().trim()
            val captain = etCaptainName.text.toString().trim()
            val phone = etCaptainPhone.text.toString().trim()
            val short = etShortName.text.toString().trim().uppercase().ifBlank { name.take(3).uppercase() }
            val viceCap = etViceCaptain.text.toString().trim()
            val city = etLocation.text.toString().trim().ifBlank { "Mumbai" }
            val desc = etDescription.text.toString().trim()

            if (name.isBlank()) {
                etTeamName.error = "Team Name is required"
                return@setOnClickListener
            }
            if (captain.isBlank()) {
                etCaptainName.error = "Captain Name is required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            layoutSaving?.visibility = View.VISIBLE
            tvSavingMsg?.text = "Saving team '$name'..."
            btnSave.text = "Saving..."

            val finalSquad = squadPlayers.toMutableList()
            val capIdx = finalSquad.indexOfFirst { it.isCaptain || it.playerName.equals(captain, ignoreCase = true) }
            if (capIdx != -1) {
                finalSquad[capIdx].isCaptain = true
            } else {
                finalSquad.add(
                    0,
                    TeamPlayerItem(
                        id = UUID.randomUUID().toString(),
                        playerName = captain,
                        role = "Batsman",
                        jerseyNumber = "7",
                        contact = phone,
                        isCaptain = true
                    )
                )
            }
            if (viceCap.isNotBlank()) {
                val vcIdx = finalSquad.indexOfFirst { it.playerName.equals(viceCap, ignoreCase = true) }
                if (vcIdx != -1) finalSquad[vcIdx].isViceCaptain = true
            }

            val userEmail = SessionManager.getInstance(requireContext()).getCurrentUser()?.email
            val teamToSave = Team(
                id = existingTeam?.id ?: "",
                tournamentId = existingTeam?.tournamentId ?: "",
                teamName = name,
                shortName = short,
                captainName = captain,
                captainPhone = phone,
                captainEmail = existingTeam?.captainEmail?.takeIf { it.isNotBlank() } ?: userEmail ?: "captain@12thman.com",
                viceCaptain = viceCap,
                managerName = existingTeam?.managerName ?: "",
                description = desc,
                city = city,
                logoUrl = selectedLogoColor,
                approvalStatus = existingTeam?.approvalStatus ?: "APPROVED",
                groupName = existingTeam?.groupName ?: "Group A",
                playerNames = finalSquad.map { it.playerName },
                players = finalSquad,
                playerCount = finalSquad.size,
                registrationDate = existingTeam?.registrationDate ?: "Today",
                isCustomCreated = true
            )

            viewModel.createOrUpdateTeam(teamToSave, isEdit) { success, msg, _ ->
                if (!isAdded) return@createOrUpdateTeam
                layoutSaving?.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                btnSave.text = "Save Team"

                if (success) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Unable to Save Team")
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
