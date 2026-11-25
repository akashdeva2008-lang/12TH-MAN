package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.TeamPlayerItem

class TeamSquadFormAdapter(
    private val players: MutableList<TeamPlayerItem>,
    private val onRemovePlayer: (Int) -> Unit
) : RecyclerView.Adapter<TeamSquadFormAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJersey: TextView = view.findViewById(R.id.tvPlayerJersey)
        val tvName: TextView = view.findViewById(R.id.tvPlayerItemName)
        val tvRole: TextView = view.findViewById(R.id.tvPlayerItemRole)
        val tvCaptainBadge: TextView = view.findViewById(R.id.tvCaptainBadge)
        val btnRemove: ImageView = view.findViewById(R.id.btnRemovePlayer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_team_form_player, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val player = players[position]
        val jerseyText = if (player.jerseyNumber.isNotBlank()) "#${player.jerseyNumber}" else "#${position + 1}"
        holder.tvJersey.text = jerseyText
        holder.tvName.text = player.playerName

        val contactStr = if (player.contact.isNotBlank()) " • ${player.contact}" else ""
        holder.tvRole.text = "${player.role}$contactStr"

        if (player.isCaptain) {
            holder.tvCaptainBadge.visibility = View.VISIBLE
            holder.tvCaptainBadge.text = "(C)"
        } else if (player.isViceCaptain) {
            holder.tvCaptainBadge.visibility = View.VISIBLE
            holder.tvCaptainBadge.text = "(VC)"
        } else {
            holder.tvCaptainBadge.visibility = View.GONE
        }

        holder.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onRemovePlayer(pos)
            }
        }
    }

    override fun getItemCount(): Int = players.size
}
