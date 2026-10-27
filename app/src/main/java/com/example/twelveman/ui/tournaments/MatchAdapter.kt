package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Match
import com.example.twelveman.databinding.ItemMatchBinding

class MatchAdapter(
    private var matches: List<Match>,
    private val onItemClick: (Match) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    fun updateData(newMatches: List<Match>) {
        matches = newMatches
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount(): Int = matches.size

    inner class MatchViewHolder(private val binding: ItemMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(match: Match) {
            binding.tvMatchStage.text = "Match ${match.matchNumber} • ${match.stage}"
            binding.tvMatchStatus.text = match.status
            binding.tvTeamA.text = match.teamA
            binding.tvTeamB.text = match.teamB
            binding.tvMatchResult.text = if (match.result.isNotBlank()) match.result else "Scheduled"
            binding.tvMatchVenueDate.text = "${match.date} • ${match.time} • ${match.venue}"

            binding.root.setOnClickListener { onItemClick(match) }
        }
    }
}