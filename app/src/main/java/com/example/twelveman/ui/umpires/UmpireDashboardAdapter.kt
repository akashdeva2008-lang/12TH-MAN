package com.example.twelveman.ui.umpires

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.databinding.ItemUmpireDashboardCardBinding

class UmpireDashboardAdapter(
    private var umpires: List<Umpire>,
    private val onBookClick: (Umpire) -> Unit,
    private val onProfileClick: (Umpire) -> Unit
) : RecyclerView.Adapter<UmpireDashboardAdapter.UmpireDashboardViewHolder>() {

    fun updateList(newList: List<Umpire>) {
        umpires = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UmpireDashboardViewHolder {
        val binding = ItemUmpireDashboardCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UmpireDashboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UmpireDashboardViewHolder, position: Int) {
        holder.bind(umpires[position])
    }

    override fun getItemCount(): Int = umpires.size

    inner class UmpireDashboardViewHolder(
        private val binding: ItemUmpireDashboardCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(umpire: Umpire) {
            binding.tvUmpireCardName.text = umpire.name
            binding.tvUmpireCardLocation.text = "${umpire.location} • ${umpire.experienceYears} Yrs Exp"
            binding.tvUmpireCardCertification.text = umpire.certificationLevel
            binding.tvUmpireCardPrice.text = "₹${umpire.pricePerMatch}"
            binding.tvUmpireCardRating.text = String.format(java.util.Locale.US, "%.1f", umpire.rating)
            binding.tvUmpireCardMatches.text = "🏆 ${umpire.matchesOfficiated}+ Matches Officiated"

            val isAvailable = umpire.availabilityStatus.equals("Available", ignoreCase = true)
            binding.tvUmpireCardAvailability.text = if (isAvailable) "● Available" else "● ${umpire.availabilityStatus}"
            binding.tvUmpireCardAvailability.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    if (isAvailable) R.color.accent_green else R.color.text_secondary
                )
            )

            binding.btnBookUmpireNow.setOnClickListener { onBookClick(umpire) }
            binding.btnViewUmpireProfile.setOnClickListener { onProfileClick(umpire) }
            binding.root.setOnClickListener { onProfileClick(umpire) }
        }
    }
}
