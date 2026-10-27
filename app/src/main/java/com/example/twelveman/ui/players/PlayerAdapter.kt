package com.example.twelveman.ui.players

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Player
import com.example.twelveman.databinding.ItemPlayerBinding

class PlayerAdapter(
    private var players: List<Player>,
    private val onItemClick: (Player) -> Unit
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    fun updateData(newPlayers: List<Player>) {
        players = newPlayers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    inner class PlayerViewHolder(private val binding: ItemPlayerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(player: Player) {
            binding.tvPlayerName.text = player.name
            binding.tvPlayerRole.text = player.role
            binding.tvPlayerLocation.text = player.location
            binding.tvPlayerStats.text = "${player.matchesPlayed} M / ${player.runsScored} R"
            binding.tvPlayerPrice.text = "₹${player.pricePerMatch}"
            binding.tvPlayerRating.text = player.rating.toString()
            binding.tvPlayerStatus.text = player.availabilityStatus

            binding.root.setOnClickListener { onItemClick(player) }
        }
    }
}