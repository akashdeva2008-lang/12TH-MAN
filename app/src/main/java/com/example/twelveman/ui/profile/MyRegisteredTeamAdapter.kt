package com.example.twelveman.ui.profile

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.DialogTeamSquadDetailsBinding
import com.example.twelveman.databinding.ItemMyRegisteredTeamBinding
import com.example.twelveman.databinding.ItemSquadPlayerRowBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyRegisteredTeamAdapter(
    private var teams: List<Team>,
    private val onTeamClick: (Team) -> Unit
) : RecyclerView.Adapter<MyRegisteredTeamAdapter.TeamViewHolder>() {

    fun updateData(newTeams: List<Team>) {
        this.teams = newTeams
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemMyRegisteredTeamBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeamViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teams[position])
    }

    override fun getItemCount(): Int = teams.size

    inner class TeamViewHolder(private val binding: ItemMyRegisteredTeamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(team: Team) {
            // Team Emblem Initial
            val initial = team.teamName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            binding.tvTeamEmblem.text = initial

            // Team Name and Short Name
            binding.tvTeamName.text = team.teamName
            binding.tvShortNameBadge.text = if (team.shortName.isNotBlank()) team.shortName else initial

            // Captain Info
            val captainPhone = if (team.captainPhone.isNotBlank()) " • ${team.captainPhone}" else ""
            binding.tvTeamCaptain.text = "Captain: ${team.captainName}$captainPhone"

            // Approval Badge
            val status = if (team.approvalStatus.isNotBlank()) team.approvalStatus.uppercase() else "APPROVED"
            binding.tvApprovalBadge.text = status

            // Tournament / City Info
            val tournStr = if (team.tournamentId.isNotBlank()) team.tournamentId else "Registered Squad"
            val cityStr = if (team.city.isNotBlank()) team.city else "Mumbai"
            binding.tvTournamentCity.text = "$tournStr • $cityStr"

            // Squad Count
            val count = if (team.players.isNotEmpty()) team.players.size else if (team.playerCount > 0) team.playerCount else team.playerNames.size
            binding.tvSquadCountBadge.text = "$count Players"

            // Squad Snippet
            if (team.players.isNotEmpty()) {
                val preview = team.players.take(6).joinToString(", ") { p ->
                    val capTag = if (p.isCaptain) " (C)" else if (p.isViceCaptain) " (VC)" else ""
                    "${p.playerName}$capTag"
                }
                val moreTag = if (team.players.size > 6) " +${team.players.size - 6} more" else ""
                binding.tvSquadSnippet.text = "Squad: $preview$moreTag"
                binding.tvSquadSnippet.visibility = View.VISIBLE
            } else if (team.playerNames.isNotEmpty()) {
                val preview = team.playerNames.take(6).joinToString(", ")
                val moreTag = if (team.playerNames.size > 6) " +${team.playerNames.size - 6} more" else ""
                binding.tvSquadSnippet.text = "Squad: $preview$moreTag"
                binding.tvSquadSnippet.visibility = View.VISIBLE
            } else {
                binding.tvSquadSnippet.text = "Squad: ${team.captainName} (Captain)"
                binding.tvSquadSnippet.visibility = View.VISIBLE
            }

            // View Squad Roster Click
            binding.btnViewSquad.setOnClickListener {
                showSquadDialog(binding.root.context, team)
            }

            binding.root.setOnClickListener {
                onTeamClick(team)
            }
        }

        private fun showSquadDialog(context: Context, team: Team) {
            val dialog = BottomSheetDialog(context)
            val dialogBinding = DialogTeamSquadDetailsBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(dialogBinding.root)

            dialogBinding.tvSquadTeamName.text = team.teamName
            val tourn = if (team.tournamentId.isNotBlank()) team.tournamentId else "12THMAN Tournament"
            dialogBinding.tvSquadTeamSubtitle.text = "Captain: ${team.captainName} • $tourn"

            val squadList = if (team.players.isNotEmpty()) {
                team.players
            } else if (team.playerNames.isNotEmpty()) {
                team.playerNames.mapIndexed { index, name ->
                    TeamPlayerItem(
                        playerName = name,
                        role = if (index == 0) "Captain / All-Rounder" else "Player",
                        isCaptain = (index == 0)
                    )
                }
            } else {
                listOf(TeamPlayerItem(playerName = team.captainName, role = "Captain", isCaptain = true))
            }

            dialogBinding.tvSquadPlayerCount.text = "${squadList.size} Players"

            dialogBinding.rvSquadPlayers.layoutManager = LinearLayoutManager(context)
            dialogBinding.rvSquadPlayers.adapter = SquadPlayerAdapter(squadList)

            dialogBinding.ivCloseSquadDialog.setOnClickListener { dialog.dismiss() }
            dialogBinding.btnDoneSquad.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }
    }
}

class SquadPlayerAdapter(
    private val players: List<TeamPlayerItem>
) : RecyclerView.Adapter<SquadPlayerAdapter.PlayerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemSquadPlayerRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position], position + 1)
    }

    override fun getItemCount(): Int = players.size

    inner class PlayerViewHolder(private val binding: ItemSquadPlayerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: TeamPlayerItem, index: Int) {
            binding.tvPlayerJersey.text = if (player.jerseyNumber.isNotBlank()) "#${player.jerseyNumber}" else "#$index"
            binding.tvPlayerName.text = player.playerName
            binding.tvPlayerRole.text = if (player.role.isNotBlank()) player.role else "Player"

            if (player.isCaptain) {
                binding.tvCaptaincyBadge.visibility = View.VISIBLE
                binding.tvCaptaincyBadge.text = "CAPTAIN"
            } else if (player.isViceCaptain) {
                binding.tvCaptaincyBadge.visibility = View.VISIBLE
                binding.tvCaptaincyBadge.text = "VICE-CAPTAIN"
            } else {
                binding.tvCaptaincyBadge.visibility = View.GONE
            }
        }
    }
}
