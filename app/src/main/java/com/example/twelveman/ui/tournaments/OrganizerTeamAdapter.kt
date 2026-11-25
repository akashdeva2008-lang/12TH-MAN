package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Team
import com.example.twelveman.databinding.ItemOrganizerTeamBinding

class OrganizerTeamAdapter(
    private var teams: List<Team>,
    private val onTeamClick: (Team) -> Unit,
    private val onRemoveClick: (Team) -> Unit
) : RecyclerView.Adapter<OrganizerTeamAdapter.TeamViewHolder>() {

    fun updateData(newTeams: List<Team>) {
        this.teams = newTeams
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val binding = ItemOrganizerTeamBinding.inflate(
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

    inner class TeamViewHolder(private val binding: ItemOrganizerTeamBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(team: Team) {
            binding.tvTeamName.text = team.teamName
            val count = if (team.playerCount > 0) team.playerCount else team.playerNames.size
            binding.tvTeamCaptain.text = "Captain: ${team.captainName} • $count Players"
            binding.tvTeamApprovalBadge.text = team.approvalStatus.uppercase()

            if (team.playerNames.isNotEmpty()) {
                binding.tvPlayerListSnippet.visibility = View.VISIBLE
                binding.tvPlayerListSnippet.text = "Squad: " + team.playerNames.take(7).joinToString(", ") +
                        if (team.playerNames.size > 7) " +${team.playerNames.size - 7} more" else ""
            } else {
                binding.tvPlayerListSnippet.visibility = View.GONE
            }

            binding.root.setOnClickListener { onTeamClick(team) }
            binding.ivRemoveTeam.setOnClickListener { onRemoveClick(team) }
        }
    }
}
