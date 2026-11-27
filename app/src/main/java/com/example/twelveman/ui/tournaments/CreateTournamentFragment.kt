package com.example.twelveman.ui.tournaments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.example.twelveman.data.model.Match
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.FragmentCreateTournamentBinding
import java.util.Calendar

class CreateTournamentFragment : Fragment() {
    private var _binding: FragmentCreateTournamentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()

    private var selectedTeam1: Team? = null
    private var selectedTeam2: Team? = null

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

        binding.ivCreateMatchBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupModeSwitching()
        setupTournamentOrganizingControls()
        setupQuickMatchControls()

        val isTournamentMode = arguments?.getBoolean("isTournament", true) ?: true
        switchMode(isTournamentMode)
    }

    private fun setupModeSwitching() {
        binding.tabModeTournament.setOnClickListener {
            switchMode(true)
        }
        binding.tabModeQuickMatch.setOnClickListener {
            switchMode(false)
        }
    }

    private fun switchMode(isTournament: Boolean) {
        val activeBg = R.drawable.bg_badge_pill_active
        val inactiveBg = R.drawable.bg_badge_pill_inactive
        val white = resources.getColor(R.color.pure_white, null)
        val grey = resources.getColor(R.color.header_dark_grey, null)

        if (isTournament) {
            binding.tabModeTournament.setBackgroundResource(activeBg)
            binding.tabModeTournament.setTextColor(white)
            binding.tabModeQuickMatch.setBackgroundResource(inactiveBg)
            binding.tabModeQuickMatch.setTextColor(grey)

            binding.layoutTournamentModeForm.visibility = View.VISIBLE
            binding.layoutQuickMatchModeForm.visibility = View.GONE
        } else {
            binding.tabModeTournament.setBackgroundResource(inactiveBg)
            binding.tabModeTournament.setTextColor(grey)
            binding.tabModeQuickMatch.setBackgroundResource(activeBg)
            binding.tabModeQuickMatch.setTextColor(white)

            binding.layoutTournamentModeForm.visibility = View.GONE
            binding.layoutQuickMatchModeForm.visibility = View.VISIBLE
        }
    }

    private fun setupTournamentOrganizingControls() {
        // Tournament Format picker
        binding.etTournFormatPicker.setOnClickListener {
            val formats = arrayOf("Knockout", "Round Robin", "League", "Group Stage + Knockout", "Custom")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Tournament Format")
                .setItems(formats) { _, which ->
                    binding.etTournFormatPicker.setText(formats[which])
                }
                .show()
        }

        // Tournament Match Type picker
        binding.etTournType.setOnClickListener {
            val types = arrayOf("T10", "T20", "ODI", "Box Cricket", "Custom Overs")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Match Type")
                .setItems(types) { _, which ->
                    binding.etTournType.setText(types[which])
                }
                .show()
        }

        // Date Pickers
        binding.etTournStartDate.setOnClickListener {
            showDatePicker { binding.etTournStartDate.setText(it) }
        }
        binding.etTournEndDate.setOnClickListener {
            showDatePicker { binding.etTournEndDate.setText(it) }
        }
        binding.etTournDeadline.setOnClickListener {
            showDatePicker { binding.etTournDeadline.setText(it) }
        }

        // Save as Draft
        binding.btnSaveDraft.setOnClickListener {
            handleCreateTournament(isDraft = 1)
        }

        // Publish Tournament
        binding.btnPublishTournament.setOnClickListener {
            handleCreateTournament(isDraft = 0)
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%02d %s %d", dayOfMonth, months[month], year)
                onDateSelected(dateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun handleCreateTournament(isDraft: Int) {
        val name = binding.etTournName.text.toString().trim()
        val description = binding.etTournDescription.text.toString().trim()
        val format = binding.etTournType.text.toString().trim().ifBlank { "T20" }
        val tournamentFormat = binding.etTournFormatPicker.text.toString().trim().ifBlank { "Knockout" }
        val city = binding.etTournCity.text.toString().trim().ifBlank { "Mumbai" }
        val ground = binding.etTournGround.text.toString().trim().ifBlank { "Wankhede Cricket Ground" }
        val startDate = binding.etTournStartDate.text.toString().trim().ifBlank { "20 Mar 2026" }
        val endDate = binding.etTournEndDate.text.toString().trim().ifBlank { "28 Mar 2026" }
        val deadline = binding.etTournDeadline.text.toString().trim().ifBlank { "18 Mar 2026" }
        val maxTeams = binding.etTournMaxTeams.text.toString().toIntOrNull() ?: 16
        val entryFee = binding.etTournEntryFee.text.toString().toIntOrNull() ?: 10000
        val prizeMoney = binding.etTournTotalPrize.text.toString().toIntOrNull() ?: 100000
        val prize1 = binding.etTournPrize1.text.toString().trim().ifBlank { "₹50,000" }
        val prize2 = binding.etTournPrize2.text.toString().trim().ifBlank { "₹25,000" }
        val prize3 = binding.etTournPrize3.text.toString().trim().ifBlank { "₹10,000" }
        val rules = binding.etTournRules.text.toString().trim().ifBlank { "Standard ICC / Board tournament rules apply." }
        val contact = binding.etTournContact.text.toString().trim().ifBlank { "+91 98200 11223" }

        if (name.isBlank()) {
            Toast.makeText(requireContext(), "Please enter Tournament Name", Toast.LENGTH_SHORT).show()
            return
        }

        val overs = when {
            format.contains("10", ignoreCase = true) -> 10
            format.contains("ODI", ignoreCase = true) -> 50
            format.contains("Test", ignoreCase = true) -> 90
            else -> 20
        }

        viewModel.createTournament(
            name = name,
            format = format,
            location = city,
            startDate = startDate,
            endDate = endDate,
            totalTeams = maxTeams,
            entryFee = entryFee,
            prizeMoney = prizeMoney,
            organizerName = "Tournament Organizer",
            organizerContact = contact,
            description = description.ifBlank { "Official cricket tournament." },
            tournamentFormat = tournamentFormat.uppercase().replace(" ", "_"),
            groundName = ground,
            registrationDeadline = deadline,
            firstPrize = prize1,
            secondPrize = prize2,
            thirdPrize = prize3,
            rules = rules,
            overs = overs,
            isDraft = isDraft
        )

        val msg = if (isDraft == 1) "Tournament saved as Draft in Organizer Dashboard" else "Tournament Published Successfully!"
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

        findNavController().navigateUp()
    }

    private fun setupQuickMatchControls() {
        // Observe allTeams from live database
        viewModel.allTeams.observe(viewLifecycleOwner) { teams ->
            if (teams.isNotEmpty()) {
                if (selectedTeam1 == null) {
                    updateTeam1Ui(teams[0])
                }
                if (selectedTeam2 == null && teams.size > 1) {
                    updateTeam2Ui(teams[1])
                }
            }
        }

        // Team 1 Selection Click
        binding.rowTeam1Picker.setOnClickListener {
            showTeamSelectionDialog(side = 1)
        }

        // Team 2 Selection Click
        binding.rowTeam2Picker.setOnClickListener {
            showTeamSelectionDialog(side = 2)
        }

        // Date & Time Picker
        binding.rowDateTimeSelector.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            val amPm = if (hourOfDay >= 12) "PM" else "AM"
                            val hourFormatted = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                            val timeStr = String.format("%02d:%02d %s", hourFormatted, minute, amPm)
                            val dateStr = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
                            binding.tvDateTimeValue.text = "$dateStr • $timeStr"
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Ball Type Selector
        binding.rowBallTypeSelector.setOnClickListener {
            val ballTypes = arrayOf("Leather Ball (White)", "Leather Ball (Red)", "Tennis Ball (Heavy)", "Tennis Ball (Light)", "Other")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Ball Type")
                .setItems(ballTypes) { _, which ->
                    binding.tvBallTypeValue.text = "Ball Type: ${ballTypes[which]}"
                }
                .show()
        }

        // Primary Schedule Match Button
        binding.btnScheduleQuickMatch.setOnClickListener {
            val team1 = selectedTeam1
            val team2 = selectedTeam2

            if (team1 == null || team2 == null) {
                Toast.makeText(requireContext(), "Please select both Team 1 and Team 2 from your teams", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (team1.id == team2.id || team1.teamName.equals(team2.teamName, ignoreCase = true)) {
                Toast.makeText(requireContext(), "Cannot schedule match between the same team! Please choose two different teams.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val location = binding.etQuickVenue.text.toString().trim().ifBlank { "Green Park Stadium" }
            val dateTimeParts = binding.tvDateTimeValue.text.toString().split("•").map { it.trim() }
            val matchDate = if (dateTimeParts.isNotEmpty()) dateTimeParts[0] else "Today"
            val matchTime = if (dateTimeParts.size > 1) dateTimeParts[1] else "5:00 PM"
            val ballType = binding.tvBallTypeValue.text.toString().removePrefix("Ball Type:").trim().ifBlank { "Leather Ball" }

            val match = viewModel.scheduleMatch(
                teamA = team1.teamName,
                teamB = team2.teamName,
                format = "T20",
                overs = 20,
                venue = location,
                date = matchDate,
                time = matchTime,
                ballType = ballType
            )

            showScheduleSuccessDialog(match)
        }
    }

    private fun updateTeam1Ui(team: Team) {
        selectedTeam1 = team
        binding.tvTeam1Name.text = team.teamName
        val short = if (team.shortName.isNotBlank()) team.shortName else team.teamName.take(3).uppercase()
        val capt = if (team.captainName.isNotBlank()) team.captainName else "Not Set"
        binding.tvTeam1Subtitle.text = "Capt: $capt • $short • ${team.city}"
        binding.ivTeam1Logo.setColorFilter(getTeamColor(team.logoUrl))
    }

    private fun updateTeam2Ui(team: Team) {
        selectedTeam2 = team
        binding.tvTeam2Name.text = team.teamName
        val short = if (team.shortName.isNotBlank()) team.shortName else team.teamName.take(3).uppercase()
        val capt = if (team.captainName.isNotBlank()) team.captainName else "Not Set"
        binding.tvTeam2Subtitle.text = "Capt: $capt • $short • ${team.city}"
        binding.ivTeam2Logo.setColorFilter(getTeamColor(team.logoUrl))
    }

    private fun getTeamColor(logoUrl: String?): Int {
        return when (logoUrl) {
            "BLUE" -> Color.parseColor("#1565C0")
            "GOLD" -> Color.parseColor("#FFB300")
            "GREEN" -> Color.parseColor("#2E7D32")
            "DARK" -> Color.parseColor("#212121")
            else -> Color.parseColor("#E10600")
        }
    }

    // =========================================================================
    // TEAM SELECTION DIALOG (Professional Dialog with Create & My Teams)
    // =========================================================================
    private fun showTeamSelectionDialog(side: Int) {
        viewModel.loadAllTeams()
        val dialogView = layoutInflater.inflate(R.layout.dialog_team_selection, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTeamSelectionTitle)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnCloseSelection)
        val btnCreateNew = dialogView.findViewById<Button>(R.id.btnCreateNewTeam)
        val tvCount = dialogView.findViewById<TextView>(R.id.tvTeamsCountBadge)
        val layoutEmpty = dialogView.findViewById<View>(R.id.layoutEmptyTeams)
        val btnEmptyCreate = dialogView.findViewById<Button>(R.id.btnEmptyCreateFirstTeam)
        val rvTeams = dialogView.findViewById<RecyclerView>(R.id.rvTeamsSelection)

        val sideTitle = if (side == 1) "Select Team 1 (Batting)" else "Select Team 2 (Bowling)"
        tvTitle.text = sideTitle

        val selectedId = if (side == 1) selectedTeam1?.id else selectedTeam2?.id
        val disabledId = if (side == 1) selectedTeam2?.id else selectedTeam1?.id
        val oppositeLabel = if (side == 1) "Team 2" else "Team 1"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener { dialog.dismiss() }

        var currentTeams = viewModel.allTeams.value ?: emptyList()

        val adapter = TeamSelectionAdapter(
            teams = currentTeams,
            selectedTeamId = selectedId,
            disabledTeamId = disabledId,
            oppositeSideLabel = oppositeLabel,
            onTeamSelect = { chosenTeam ->
                if (side == 1) {
                    updateTeam1Ui(chosenTeam)
                } else {
                    updateTeam2Ui(chosenTeam)
                }
                dialog.dismiss()
            },
            onTeamEdit = { teamToEdit ->
                showCreateOrEditTeamDialog(teamToEdit) { updatedTeam ->
                    if (side == 1 && selectedTeam1?.id == updatedTeam.id) updateTeam1Ui(updatedTeam)
                    if (side == 2 && selectedTeam2?.id == updatedTeam.id) updateTeam2Ui(updatedTeam)
                }
            },
            onTeamDelete = { teamToDelete ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Team")
                    .setMessage("Are you sure you want to delete '${teamToDelete.teamName}'? This will remove the team permanently from your database.")
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteTeam(teamToDelete.id) { success, msg ->
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )

        rvTeams.layoutManager = LinearLayoutManager(requireContext())
        rvTeams.adapter = adapter

        fun updateList(list: List<Team>) {
            currentTeams = list
            tvCount.text = list.size.toString()
            if (list.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvTeams.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvTeams.visibility = View.VISIBLE
                adapter.updateTeams(list)
            }
        }

        updateList(currentTeams)

        viewModel.allTeams.observe(viewLifecycleOwner) { updatedList ->
            if (dialog.isShowing) {
                updateList(updatedList)
            }
        }

        btnCreateNew.setOnClickListener {
            showCreateOrEditTeamDialog(null) { newlyCreatedTeam ->
                if (side == 1) updateTeam1Ui(newlyCreatedTeam) else updateTeam2Ui(newlyCreatedTeam)
                dialog.dismiss()
            }
        }

        btnEmptyCreate.setOnClickListener {
            showCreateOrEditTeamDialog(null) { newlyCreatedTeam ->
                if (side == 1) updateTeam1Ui(newlyCreatedTeam) else updateTeam2Ui(newlyCreatedTeam)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =========================================================================
    // CREATE / EDIT TEAM DIALOG
    // =========================================================================
    private fun showCreateOrEditTeamDialog(existingTeam: Team?, onSaved: ((Team) -> Unit)? = null) {
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

        val isEdit = (existingTeam != null)
        tvTitle.text = if (isEdit) "Edit Team: ${existingTeam!!.teamName}" else "Create New Team"

        var selectedLogoColor = existingTeam?.logoUrl ?: "RED"

        fun applyLogoColor(colorTag: String) {
            selectedLogoColor = colorTag
            ivLogoPreview.setColorFilter(getTeamColor(colorTag))
        }

        applyLogoColor(selectedLogoColor)

        pRed.setOnClickListener { applyLogoColor("RED") }
        pBlue.setOnClickListener { applyLogoColor("BLUE") }
        pGold.setOnClickListener { applyLogoColor("GOLD") }
        pGreen.setOnClickListener { applyLogoColor("GREEN") }
        pDark.setOnClickListener { applyLogoColor("DARK") }

        // Squad players list
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
                squadPlayers.addAll(existingTeam.players)
            } else if (existingTeam.playerNames.isNotEmpty()) {
                existingTeam.playerNames.forEachIndexed { i, pName ->
                    squadPlayers.add(
                        TeamPlayerItem(
                            playerName = pName,
                            role = if (i == 0) "Batsman (C)" else "Batsman",
                            jerseyNumber = (i + 1).toString(),
                            isCaptain = (i == 0)
                        )
                    )
                }
            }
        } else {
            val user = com.example.twelveman.data.local.SessionManager.getInstance(requireContext()).getCurrentUser()
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

        playersAdapter = TeamSquadFormAdapter(squadPlayers) { removedPosition ->
            squadPlayers.removeAt(removedPosition)
            updatePlayersUi()
        }

        rvPlayers.layoutManager = LinearLayoutManager(requireContext())
        rvPlayers.adapter = playersAdapter
        updatePlayersUi()

        val layoutSaving = formView.findViewById<View>(R.id.layoutSavingIndicator)
        val tvSavingMsg = formView.findViewById<TextView>(R.id.tvSavingMessage)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(formView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnAddPlayer.setOnClickListener {
            showAddPlayerDialog { newPlayer ->
                if (newPlayer.isCaptain) {
                    squadPlayers.forEach { it.isCaptain = false }
                }
                if (newPlayer.isViceCaptain) {
                    squadPlayers.forEach { it.isViceCaptain = false }
                }
                squadPlayers.add(newPlayer)
                updatePlayersUi()
            }
        }

        btnSave.setOnClickListener {
            val name = etTeamName.text.toString().trim()
            val captain = etCaptainName.text.toString().trim()
            val phone = etCaptainPhone.text.toString().trim()
            var short = etShortName.text.toString().trim().uppercase()
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
            if (short.isBlank()) {
                short = name.take(3).uppercase()
            }

            // Disable buttons and show loading to prevent duplicate requests
            btnSave.isEnabled = false
            btnCancel.isEnabled = false
            layoutSaving?.visibility = View.VISIBLE
            tvSavingMsg?.text = "Saving team '$name'..."
            btnSave.text = "Saving..."

            // Build a non-destructive clean squad list
            val finalSquad = mutableListOf<TeamPlayerItem>()
            finalSquad.addAll(squadPlayers)

            // Ensure captain exists and is properly designated
            val existingCapIdx = finalSquad.indexOfFirst { it.isCaptain || it.playerName.equals(captain, ignoreCase = true) }
            if (existingCapIdx != -1) {
                finalSquad[existingCapIdx].isCaptain = true
            } else {
                finalSquad.add(
                    0,
                    TeamPlayerItem(
                        id = java.util.UUID.randomUUID().toString(),
                        playerName = captain,
                        role = "Batsman",
                        jerseyNumber = "7",
                        contact = phone,
                        isCaptain = true,
                        isViceCaptain = false
                    )
                )
            }

            // Ensure vice-captain flag if vice-captain name is provided
            if (viceCap.isNotBlank()) {
                val vcIdx = finalSquad.indexOfFirst { it.playerName.equals(viceCap, ignoreCase = true) }
                if (vcIdx != -1) {
                    finalSquad[vcIdx].isViceCaptain = true
                }
            }

            val currentUserEmail = com.example.twelveman.data.local.SessionManager.getInstance(requireContext()).getCurrentUser()?.email
            val teamToSave = Team(
                id = existingTeam?.id ?: "",
                tournamentId = existingTeam?.tournamentId ?: "",
                teamName = name,
                shortName = short,
                captainName = captain,
                captainPhone = phone,
                captainEmail = existingTeam?.captainEmail?.takeIf { it.isNotBlank() } ?: currentUserEmail ?: "captain@12thman.com",
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

            viewModel.createOrUpdateTeam(teamToSave, isEdit) { success, msg, savedTeam ->
                if (!isAdded) return@createOrUpdateTeam

                layoutSaving?.visibility = View.GONE
                btnSave.isEnabled = true
                btnCancel.isEnabled = true
                btnSave.text = "💾  Save Team"

                if (success) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    val resultTeam = savedTeam ?: teamToSave
                    onSaved?.invoke(resultTeam)
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

    // =========================================================================
    // ADD PLAYER DIALOG
    // =========================================================================
    private fun showAddPlayerDialog(onPlayerAdded: (TeamPlayerItem) -> Unit) {
        val playerView = layoutInflater.inflate(R.layout.dialog_add_player, null)

        val etName = playerView.findViewById<EditText>(R.id.etDialogPlayerName)
        val etRole = playerView.findViewById<EditText>(R.id.etDialogPlayerRole)
        val etJersey = playerView.findViewById<EditText>(R.id.etDialogPlayerJersey)
        val etContact = playerView.findViewById<EditText>(R.id.etDialogPlayerContact)
        val cbIsCaptain = playerView.findViewById<android.widget.CheckBox>(R.id.cbIsCaptain)
        val cbIsViceCaptain = playerView.findViewById<android.widget.CheckBox>(R.id.cbIsViceCaptain)
        val btnCancel = playerView.findViewById<Button>(R.id.btnCancelAddPlayer)
        val btnConfirm = playerView.findViewById<Button>(R.id.btnConfirmAddPlayer)

        val roles = arrayOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper")
        etRole.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Player Role")
                .setItems(roles) { _, which ->
                    etRole.setText(roles[which])
                }
                .show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(playerView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val pName = etName.text.toString().trim()
            val pRole = etRole.text.toString().trim().ifBlank { "Batsman" }
            val pJersey = etJersey.text.toString().trim()
            val pContact = etContact.text.toString().trim()

            if (pName.isBlank()) {
                etName.error = "Player Name is required"
                return@setOnClickListener
            }

            val item = TeamPlayerItem(
                id = java.util.UUID.randomUUID().toString(),
                playerName = pName,
                role = pRole,
                jerseyNumber = pJersey,
                contact = pContact,
                isCaptain = cbIsCaptain?.isChecked == true,
                isViceCaptain = cbIsViceCaptain?.isChecked == true
            )

            onPlayerAdded(item)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showScheduleSuccessDialog(match: Match) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_match_scheduled_success, null)

        val tvTeams = dialogView.findViewById<TextView>(R.id.tvSuccessTeams)
        val tvDateTime = dialogView.findViewById<TextView>(R.id.tvSuccessDateTime)
        val tvVenue = dialogView.findViewById<TextView>(R.id.tvSuccessVenue)
        val tvFormatOvers = dialogView.findViewById<TextView>(R.id.tvSuccessFormatOvers)
        val tvBallType = dialogView.findViewById<TextView>(R.id.tvSuccessBallType)
        val btnViewMatch = dialogView.findViewById<Button>(R.id.btnSuccessViewMatch)
        val btnStartScoring = dialogView.findViewById<Button>(R.id.btnSuccessStartScoring)

        tvTeams.text = "${match.teamA} vs ${match.teamB}"
        tvDateTime.text = "${match.date} • ${match.time}"
        tvVenue.text = match.venue
        tvFormatOvers.text = "${match.format} • ${match.totalOvers} Overs"
        tvBallType.text = match.ballType

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnViewMatch.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("matchId", match.id) }
            findNavController().navigate(R.id.action_create_to_match_details, bundle)
        }

        btnStartScoring.setOnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("matchId", match.id) }
            findNavController().navigate(R.id.action_create_to_scoring, bundle)
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
