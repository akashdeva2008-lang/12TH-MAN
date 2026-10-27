package com.example.twelveman.ui.tournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Tournament
import com.example.twelveman.databinding.ItemTournamentBinding

class TournamentAdapter(
    private var tournaments: List<Tournament>,
    private val onItemClick: (Tournament) -> Unit
) : RecyclerView.Adapter<TournamentAdapter.TournamentViewHolder>() {

    fun updateData(newTournaments: List<Tournament>) {
        tournaments = newTournaments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentViewHolder {
        val binding = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TournamentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TournamentViewHolder, position: Int) {
        holder.bind(tournaments[position])
    }

    override fun getItemCount(): Int = tournaments.size

    inner class TournamentViewHolder(private val binding: ItemTournamentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tournament: Tournament) {
            binding.tvTournamentName.text = tournament.name
            binding.tvTournamentDates.text = "${tournament.startDate} - ${tournament.endDate}"
            binding.tvTournamentStatus.text = tournament.status
            binding.tvTeamsInfo.text = "${tournament.registeredTeamsCount} / ${tournament.totalTeams}"
            binding.tvEntryFee.text = "₹${tournament.entryFee}"
            binding.tvPrizeMoney.text = "₹${tournament.prizeMoney}"

            binding.root.setOnClickListener { onItemClick(tournament) }
        }
    }
}