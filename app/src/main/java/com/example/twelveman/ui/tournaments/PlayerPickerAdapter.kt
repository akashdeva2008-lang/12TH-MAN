package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.TeamPlayerItem
import com.example.twelveman.databinding.ItemPlayerPickerBinding
import java.util.Locale

class PlayerPickerAdapter(
    private var allPlayers: List<TeamPlayerItem>,
    private var selectedPlayerId: String = "",
    private val onPlayerSelected: (TeamPlayerItem) -> Unit
) : RecyclerView.Adapter<PlayerPickerAdapter.PlayerPickerViewHolder>() {

    private var filteredList = allPlayers.toMutableList()

    fun updateList(newList: List<TeamPlayerItem>, selectedId: String = selectedPlayerId) {
        allPlayers = newList
        selectedPlayerId = selectedId
        filteredList = allPlayers.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase(Locale.ROOT)
        filteredList = if (q.isBlank()) {
            allPlayers.toMutableList()
        } else {
            allPlayers.filter {
                it.playerName.lowercase(Locale.ROOT).contains(q) ||
                it.jerseyNumber.contains(q) ||
                it.role.lowercase(Locale.ROOT).contains(q)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun setSelectedPlayer(player: TeamPlayerItem) {
        selectedPlayerId = player.id
        notifyDataSetChanged()
    }

    fun getSelectedPlayer(): TeamPlayerItem? {
        return allPlayers.firstOrNull { it.id == selectedPlayerId || it.playerName.equals(selectedPlayerId, ignoreCase = true) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerPickerViewHolder {
        val binding = ItemPlayerPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlayerPickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerPickerViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size

    inner class PlayerPickerViewHolder(private val binding: ItemPlayerPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: TeamPlayerItem) {
            binding.tvPickerPlayerName.text = player.playerName
            binding.tvPickerPlayerRole.text = player.role
            binding.tvPickerJersey.text = if (player.jerseyNumber.isNotBlank()) "#${player.jerseyNumber}" else "●"

            binding.tvPickerCaptainBadge.visibility = if (player.isCaptain) View.VISIBLE else View.GONE
            binding.tvPickerViceCaptainBadge.visibility = if (player.isViceCaptain) View.VISIBLE else View.GONE

            val isSelected = player.id == selectedPlayerId || (selectedPlayerId.isNotBlank() && player.playerName.equals(selectedPlayerId, ignoreCase = true))
            binding.rbPickerSelected.isChecked = isSelected

            if (isSelected) {
                binding.cardPlayerPicker.strokeColor = ContextCompat.getColor(binding.root.context, R.color.primary_red)
                binding.cardPlayerPicker.strokeWidth = 3
                binding.cardPlayerPicker.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_red_bg))
            } else {
                binding.cardPlayerPicker.strokeColor = ContextCompat.getColor(binding.root.context, R.color.border_grey)
                binding.cardPlayerPicker.strokeWidth = 1
                binding.cardPlayerPicker.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface_white))
            }

            binding.root.setOnClickListener {
                selectedPlayerId = player.id
                notifyDataSetChanged()
                onPlayerSelected(player)
            }
        }
    }
}
