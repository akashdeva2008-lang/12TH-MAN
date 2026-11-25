package com.example.twelveman.ui.tournaments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.Team

class TeamSelectionAdapter(
    private var teams: List<Team>,
    private val selectedTeamId: String?,
    private val disabledTeamId: String?,
    private val oppositeSideLabel: String,
    private val onTeamSelect: (Team) -> Unit,
    private val onTeamEdit: (Team) -> Unit,
    private val onTeamDelete: (Team) -> Unit
) : RecyclerView.Adapter<TeamSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.cardTeamSelection)
        val ivEmblem: ImageView = view.findViewById(R.id.ivTeamEmblem)
        val tvBadgeShort: TextView = view.findViewById(R.id.tvTeamBadgeShort)
        val tvName: TextView = view.findViewById(R.id.tvTeamItemName)
        val tvShortPill: TextView = view.findViewById(R.id.tvTeamItemShortPill)
        val tvSubtitle: TextView = view.findViewById(R.id.tvTeamItemSubtitle)
        val tvStatus: TextView = view.findViewById(R.id.tvTeamItemStatus)
        val btnEdit: ImageView = view.findViewById(R.id.btnEditTeam)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteTeam)
        val ivCheck: ImageView = view.findViewById(R.id.ivSelectCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_team_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val team = teams[position]
        holder.tvName.text = team.teamName

        val short = if (team.shortName.isNotBlank()) team.shortName else team.teamName.take(3).uppercase()
        holder.tvBadgeShort.text = short.take(3)
        holder.tvShortPill.text = short

        val count = if (team.playerCount > 0) team.playerCount else if (team.players.isNotEmpty()) team.players.size else team.playerNames.size
        val capt = if (team.captainName.isNotBlank()) team.captainName else "Not Set"
        holder.tvSubtitle.text = "Capt: $capt • $count Players • ${team.city}"

        // Color tint for emblem
        val tintColor = when (team.logoUrl) {
            "BLUE" -> Color.parseColor("#1565C0")
            "GOLD" -> Color.parseColor("#FFB300")
            "GREEN" -> Color.parseColor("#2E7D32")
            "DARK" -> Color.parseColor("#212121")
            else -> Color.parseColor("#E10600")
        }
        holder.ivEmblem.setColorFilter(tintColor)

        val isSelectedCurrent = (team.id == selectedTeamId)
        val isDisabledOpposite = (team.id == disabledTeamId && !disabledTeamId.isNullOrBlank())

        if (isDisabledOpposite) {
            holder.card.alpha = 0.55f
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "Already selected as $oppositeSideLabel"
            holder.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
            holder.ivCheck.visibility = View.GONE

            holder.card.setOnClickListener {
                Toast.makeText(holder.itemView.context, "This team is already selected as $oppositeSideLabel. Please choose a different team.", Toast.LENGTH_SHORT).show()
            }
        } else {
            holder.card.alpha = 1.0f
            if (isSelectedCurrent) {
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvStatus.text = "Currently Selected"
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                holder.ivCheck.visibility = View.VISIBLE
            } else {
                holder.tvStatus.visibility = View.GONE
                holder.ivCheck.visibility = View.GONE
            }

            holder.card.setOnClickListener {
                onTeamSelect(team)
            }
        }

        holder.btnEdit.setOnClickListener {
            onTeamEdit(team)
        }

        holder.btnDelete.setOnClickListener {
            onTeamDelete(team)
        }
    }

    override fun getItemCount(): Int = teams.size

    fun updateTeams(newTeams: List<Team>) {
        this.teams = newTeams
        notifyDataSetChanged()
    }
}
