package com.example.twelveman.ui.profile

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Team
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.DialogTeamSquadDetailsBinding
import com.example.twelveman.databinding.ItemMyRegisteredTeamBinding
import com.example.twelveman.databinding.ItemSquadPlayerRowBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MyRegisteredTeamAdapter(
    private var teams: List<Team>,
    private val onEditTeam: (Team) -> Unit,
    private val onDeleteTeam: (Team) -> Unit
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

    private fun resolveLogoColor(logoUrl: String?): Int {
        return when (logoUrl) {
            "BLUE" -> Color.parseColor("#1565C0")
            "GOLD" -> Color.parseColor("#FFB300")
            "GREEN" -> Color.parseColor("#2E7D32")
            "DARK" -> Color.parseColor("#212121")
            else -> Color.parseColor("#E50920")
        }
    }

    inner class TeamViewHolder(private val binding: ItemMyRegisteredTeamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(team: Team) {
            val short = if (team.shortName.isNotBlank()) team.shortName.take(3).uppercase()
            else team.teamName.trim().take(3).uppercase()

            binding.tvTeamBadgeShort.text = short
            binding.ivTeamEmblem.setColorFilter(resolveLogoColor(team.logoUrl))

            binding.tvTeamName.text = team.teamName
            binding.tvShortNameBadge.text = short

            val captainPhone = if (team.captainPhone.isNotBlank()) " • ${team.captainPhone}" else ""
            binding.tvTeamCaptain.text = "Captain: ${team.captainName}$captainPhone"

            val cityStr = team.city.ifBlank { "Mumbai" }
            val tournStr = if (team.tournamentId.isNotBlank()) " • ${team.tournamentId}" else ""
            binding.tvTournamentCity.text = "Location: $cityStr$tournStr"

            val count = if (team.players.isNotEmpty()) team.players.size
            else if (team.playerCount > 0) team.playerCount
            else team.playerNames.size
            binding.tvSquadCountBadge.text = "$count Players"

            if (team.players.isNotEmpty()) {
                val preview = team.players.take(6).joinToString(", ") { p ->
                    val tag = if (p.isCaptain) " (C)" else if (p.isViceCaptain) " (VC)" else ""
                    "${p.playerName}$tag"
                }
                val moreTag = if (team.players.size > 6) " +${team.players.size - 6} more" else ""
                binding.tvSquadSnippet.text = "Squad: $preview$moreTag"
            } else if (team.playerNames.isNotEmpty()) {
                val preview = team.playerNames.take(6).joinToString(", ")
                val moreTag = if (team.playerNames.size > 6) " +${team.playerNames.size - 6} more" else ""
                binding.tvSquadSnippet.text = "Squad: $preview$moreTag"
            } else {
                binding.tvSquadSnippet.text = "Squad: ${team.captainName} (C)"
            }

            binding.btnViewSquad.setOnClickListener {
                showSquadDialog(binding.root.context, team)
            }
            binding.root.setOnClickListener {
                showSquadDialog(binding.root.context, team)
            }
            binding.btnEditMyTeam.setOnClickListener {
                onEditTeam(team)
            }
            binding.btnDeleteMyTeam.setOnClickListener {
                onDeleteTeam(team)
            }
        }

        private fun showSquadDialog(context: Context, team: Team) {
            val dialog = BottomSheetDialog(context)
            val dialogBinding = DialogTeamSquadDetailsBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(dialogBinding.root)

            val short = if (team.shortName.isNotBlank()) team.shortName else team.teamName.take(3).uppercase()
            dialogBinding.tvSquadTeamName.text = "${team.teamName} ($short)"
            dialogBinding.ivSquadTeamLogo.setColorFilter(resolveLogoColor(team.logoUrl))
            dialogBinding.tvSquadTeamSubtitle.text = "Captain: ${team.captainName} • ${team.city.ifBlank { "Mumbai" }}"

            val bioParts = mutableListOf<String>()
            if (team.viceCaptain.isNotBlank()) bioParts.add("Vice-Captain: ${team.viceCaptain}")
            if (team.captainPhone.isNotBlank()) bioParts.add("Contact: ${team.captainPhone}")
            if (team.description.isNotBlank()) bioParts.add(team.description)
            if (bioParts.isNotEmpty()) {
                dialogBinding.tvSquadTeamBio.visibility = View.VISIBLE
                dialogBinding.tvSquadTeamBio.text = bioParts.joinToString(" • ")
            } else {
                dialogBinding.tvSquadTeamBio.visibility = View.GONE
            }

            val squadList = if (team.players.isNotEmpty()) {
                team.players
            } else if (team.playerNames.isNotEmpty()) {
                team.playerNames.mapIndexed { index, name ->
                    TeamPlayerItem(
                        playerName = name,
                        role = if (index == 0) "Captain" else "Player",
                        jerseyNumber = (index + 1).toString(),
                        isCaptain = (index == 0)
                    )
                }
            } else {
                listOf(TeamPlayerItem(playerName = team.captainName, role = "Captain", jerseyNumber = "7", isCaptain = true))
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
            binding.tvPlayerRole.text = player.role.ifBlank { "Player" }

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
