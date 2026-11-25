package com.example.twelveman.ui.tournaments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentOrganizerDashboardBinding

class OrganizerDashboardFragment : Fragment() {

    private var _binding: FragmentOrganizerDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentsViewModel by viewModels()

    private var currentTournamentId: String = "T301"

    private lateinit var teamAdapter: OrganizerTeamAdapter
    private lateinit var matchAdapter: MatchAdapter
    private lateinit var standingsAdapter: StandingsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrganizerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("tournamentId")?.let {
            if (it.isNotBlank()) currentTournamentId = it
        }

        setupRecyclerViews()
        setupTabSwitching()
        setupActionButtons()
        observeViewModel()

        viewModel.fetchTournaments()
        viewModel.loadOrganizerDashboard(currentTournamentId)
    }

    private fun setupRecyclerViews() {
        // 1. Teams Adapter
        teamAdapter = OrganizerTeamAdapter(
            teams = emptyList(),
            onTeamClick = { team ->
                Toast.makeText(requireContext(), "${team.teamName}: ${team.playerNames.size} registered players", Toast.LENGTH_SHORT).show()
            },
            onRemoveClick = { team ->
                showRemoveTeamConfirmation(team.id, team.teamName)
            }
        )
        binding.rvOrganizerTeams.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrganizerTeams.adapter = teamAdapter

        // 2. Matches Adapter (Existing MatchAdapter)
        matchAdapter = MatchAdapter(
            matches = emptyList(),
            onItemClick = { match ->
                val bundle = Bundle().apply { putString("matchId", match.id) }
                findNavController().navigate(R.id.action_organizer_to_match_details, bundle)
            },
            onStartScoring = { match ->
                val bundle = Bundle().apply { putString("matchId", match.id) }
                findNavController().navigate(R.id.action_organizer_to_scoring, bundle)
            }
        )
        binding.rvOrganizerMatches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrganizerMatches.adapter = matchAdapter

        // 3. Standings Adapter
        standingsAdapter = StandingsAdapter(emptyList())
        binding.rvOrganizerStandings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrganizerStandings.adapter = standingsAdapter
    }

    private fun setupTabSwitching() {
        binding.tabOrganizerTeams.setOnClickListener {
            selectTab(0)
        }
        binding.tabOrganizerFixtures.setOnClickListener {
            selectTab(1)
        }
        binding.tabOrganizerStandings.setOnClickListener {
            selectTab(2)
        }
    }

    private fun selectTab(index: Int) {
        val activeBg = R.drawable.bg_badge_pill_active
        val inactiveBg = R.drawable.bg_badge_pill_inactive
        val white = resources.getColor(R.color.pure_white, null)
        val grey = resources.getColor(R.color.header_dark_grey, null)

        when (index) {
            0 -> {
                binding.tabOrganizerTeams.setBackgroundResource(activeBg)
                binding.tabOrganizerTeams.setTextColor(white)
                binding.tabOrganizerFixtures.setBackgroundResource(inactiveBg)
                binding.tabOrganizerFixtures.setTextColor(grey)
                binding.tabOrganizerStandings.setBackgroundResource(inactiveBg)
                binding.tabOrganizerStandings.setTextColor(grey)

                binding.layoutTeamsContainer.visibility = View.VISIBLE
                binding.layoutFixturesContainer.visibility = View.GONE
                binding.layoutStandingsContainer.visibility = View.GONE
            }
            1 -> {
                binding.tabOrganizerTeams.setBackgroundResource(inactiveBg)
                binding.tabOrganizerTeams.setTextColor(grey)
                binding.tabOrganizerFixtures.setBackgroundResource(activeBg)
                binding.tabOrganizerFixtures.setTextColor(white)
                binding.tabOrganizerStandings.setBackgroundResource(inactiveBg)
                binding.tabOrganizerStandings.setTextColor(grey)

                binding.layoutTeamsContainer.visibility = View.GONE
                binding.layoutFixturesContainer.visibility = View.VISIBLE
                binding.layoutStandingsContainer.visibility = View.GONE
            }
            2 -> {
                binding.tabOrganizerTeams.setBackgroundResource(inactiveBg)
                binding.tabOrganizerTeams.setTextColor(grey)
                binding.tabOrganizerFixtures.setBackgroundResource(inactiveBg)
                binding.tabOrganizerFixtures.setTextColor(grey)
                binding.tabOrganizerStandings.setBackgroundResource(activeBg)
                binding.tabOrganizerStandings.setTextColor(white)

                binding.layoutTeamsContainer.visibility = View.GONE
                binding.layoutFixturesContainer.visibility = View.GONE
                binding.layoutStandingsContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun setupActionButtons() {
        binding.ivOrganizerBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnOrganizerRefresh.setOnClickListener {
            viewModel.loadOrganizerDashboard(currentTournamentId)
            Toast.makeText(requireContext(), "Live sync refreshed from database", Toast.LENGTH_SHORT).show()
        }

        binding.btnQuickAddTeam.setOnClickListener {
            val bundle = Bundle().apply { putString("tournamentId", currentTournamentId) }
            findNavController().navigate(R.id.action_organizer_to_register_team, bundle)
        }

        binding.btnQuickGenerateFixtures.setOnClickListener {
            showGenerateFixturesDialog()
        }

        binding.btnQuickCreateTournament.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isTournament", true) }
            findNavController().navigate(R.id.action_organizer_to_create_tournament, bundle)
        }

        binding.ivCreateTournamentTop.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isTournament", true) }
            findNavController().navigate(R.id.action_organizer_to_create_tournament, bundle)
        }

        binding.ivSwitchTournament.setOnClickListener {
            showTournamentPicker()
        }

        binding.cardOrganizerTournamentHero.setOnClickListener {
            showTournamentPicker()
        }

        // Quick Stats Cards Tab Switching
        binding.cardStatTeams.setOnClickListener { selectTab(0) }
        binding.cardStatMatches.setOnClickListener { selectTab(1) }
        binding.cardStatLive.setOnClickListener { selectTab(1) }
        binding.cardStatCompleted.setOnClickListener { selectTab(1) }

        // Empty state quick triggers
        binding.tvEmptyTeams.setOnClickListener {
            binding.btnQuickAddTeam.performClick()
        }
        binding.tvEmptyMatches.setOnClickListener {
            binding.btnQuickGenerateFixtures.performClick()
        }
    }

    private fun observeViewModel() {
        viewModel.organizerData.observe(viewLifecycleOwner) { data ->
            if (data?.tournament != null) {
                val t = data.tournament
                binding.tvOrganizerSubHeader.text = t.name
                binding.tvOrganizerTournTitle.text = t.name
                binding.tvOrganizerTournFormat.text = "${t.format} • ${t.tournamentFormat}"
                binding.tvOrganizerTournStatus.text = t.status.uppercase()

                val ground = if (t.groundName.isNotBlank()) t.groundName else t.location
                binding.tvOrganizerTournMeta.text = "📍 $ground • ${t.startDate} - ${t.endDate}\n🏆 1st: ${t.firstPrize} • 2nd: ${t.secondPrize} • 3rd: ${t.thirdPrize}"

                val stats = data.stats
                if (stats != null) {
                    binding.tvStatRegisteredTeams.text = "${stats.registeredTeams} / ${stats.maxTeams}"
                    binding.tvStatTotalMatches.text = stats.totalMatches.toString()
                    binding.tvStatLiveMatches.text = stats.liveMatches.toString()
                    binding.tvStatCompletedMatches.text = stats.completedMatches.toString()
                    binding.pbTournamentProgress.progress = stats.progressPercent
                    binding.tvProgressPercent.text = "${stats.progressPercent}% Completed"
                }
            }
        }

        viewModel.tournamentTeams.observe(viewLifecycleOwner) { teams ->
            teamAdapter.updateData(teams)
            binding.tvEmptyTeams.visibility = if (teams.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.matchesList.observe(viewLifecycleOwner) { matches ->
            val tournMatches = matches.filter { it.tournamentId.equals(currentTournamentId, ignoreCase = true) }
            matchAdapter.updateData(tournMatches)
            binding.tvEmptyMatches.visibility = if (tournMatches.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.standingsList.observe(viewLifecycleOwner) { standings ->
            standingsAdapter.updateData(standings)
            binding.tvEmptyStandings.visibility = if (standings.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showTournamentPicker() {
        val tournaments = viewModel.tournamentsList.value ?: emptyList()
        if (tournaments.isEmpty()) {
            Toast.makeText(requireContext(), "No tournaments available. Create one first!", Toast.LENGTH_SHORT).show()
            return
        }

        val names = tournaments.map { "${it.name} (${it.status})" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Switch Organized Tournament")
            .setItems(names) { _, which ->
                val selected = tournaments[which]
                currentTournamentId = selected.id
                viewModel.loadOrganizerDashboard(currentTournamentId)
                Toast.makeText(requireContext(), "Switched to: ${selected.name}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveTeamConfirmation(teamId: String, teamName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Team Confirmation")
            .setMessage("Are you sure you want to remove '$teamName' from this tournament?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeTeamFromTournament(currentTournamentId, teamId) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "$teamName removed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Could not remove team (check if matches exist)", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGenerateFixturesDialog() {
        val context = requireContext()
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 40, 50, 30)
            setBackgroundColor(Color.WHITE)
        }

        val title = android.widget.TextView(context).apply {
            text = "⚡ Auto Generate Fixtures"
            textSize = 18f
            setTextColor(Color.parseColor("#121212"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        val desc = android.widget.TextView(context).apply {
            text = "Select tournament format and settings to automatically schedule matches with odd/even team support & byes."
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
            setPadding(0, 0, 0, 16)
        }
        layout.addView(desc)

        val spinnerFormat = Spinner(context)
        val formats = arrayOf("Knockout", "Round Robin / League", "Group Stage + Knockout")
        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, formats)
        spinnerFormat.adapter = spinnerAdapter
        layout.addView(spinnerFormat)

        val etVenue = EditText(context).apply {
            hint = "Match Venue (e.g. Wankhede Ground)"
            setText("Wankhede Cricket Ground")
        }
        layout.addView(etVenue)

        val etOvers = EditText(context).apply {
            hint = "Match Overs"
            setText("20")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etOvers)

        val btnConfirm = com.google.android.material.button.MaterialButton(context).apply {
            text = "Generate Fixtures Now"
            setBackgroundColor(Color.parseColor("#E10600"))
            setTextColor(Color.WHITE)
            cornerRadius = 12
            setOnClickListener {
                val selectedFormat = when (spinnerFormat.selectedItemPosition) {
                    0 -> "KNOCKOUT"
                    1 -> "ROUND_ROBIN"
                    else -> "GROUP_KNOCKOUT"
                }
                val venue = etVenue.text.toString().trim().ifBlank { "Cricket Ground" }
                val overs = etOvers.text.toString().toIntOrNull() ?: 20

                viewModel.generateFixtures(
                    tournamentId = currentTournamentId,
                    format = selectedFormat,
                    venue = venue,
                    startDate = "",
                    overs = overs,
                    umpire = "Official Umpire",
                    scorer = "Official Scorer"
                ) { success ->
                    if (success) {
                        Toast.makeText(context, "Fixtures generated successfully!", Toast.LENGTH_SHORT).show()
                        selectTab(1) // Switch to Fixtures tab
                    } else {
                        Toast.makeText(context, "Need at least 2 registered teams to generate fixtures", Toast.LENGTH_LONG).show()
                    }
                }
                dialog.dismiss()
            }
        }
        layout.addView(btnConfirm)

        dialog.setContentView(layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
