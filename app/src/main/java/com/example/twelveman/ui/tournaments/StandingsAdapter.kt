package com.example.twelveman.ui.tournaments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.TeamStanding
import com.example.twelveman.databinding.ItemStandingRowBinding

class StandingsAdapter(
    private var standings: List<TeamStanding>
) : RecyclerView.Adapter<StandingsAdapter.StandingViewHolder>() {

    fun updateData(newList: List<TeamStanding>) {
        this.standings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val binding = ItemStandingRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StandingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        holder.bind(standings[position], position)
    }

    override fun getItemCount(): Int = standings.size

    inner class StandingViewHolder(private val binding: ItemStandingRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TeamStanding, position: Int) {
            binding.tvStandingPos.text = item.position.toString()
            binding.tvStandingTeamName.text = item.teamName
            binding.tvStandingP.text = item.played.toString()
            binding.tvStandingW.text = item.won.toString()
            binding.tvStandingL.text = item.lost.toString()
            binding.tvStandingPts.text = item.points.toString()

            val prefix = if (item.nrr >= 0) "+" else ""
            binding.tvStandingNrr.text = String.format("%s%.3f", prefix, item.nrr)

            // Top 2 qualifiers highlighted slightly
            if (position < 2) {
                binding.tvStandingPos.setTextColor(Color.parseColor("#E10600"))
            } else {
                binding.tvStandingPos.setTextColor(Color.parseColor("#121212"))
            }
        }
    }
}
