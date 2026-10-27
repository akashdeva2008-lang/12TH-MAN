package com.example.twelveman.ui.umpires

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Umpire
import com.example.twelveman.databinding.ItemUmpireBinding

class UmpireAdapter(
    private var umpires: List<Umpire>,
    private val onItemClick: (Umpire) -> Unit
) : RecyclerView.Adapter<UmpireAdapter.UmpireViewHolder>() {

    fun updateData(newUmpires: List<Umpire>) {
        umpires = newUmpires
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UmpireViewHolder {
        val binding = ItemUmpireBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UmpireViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UmpireViewHolder, position: Int) {
        holder.bind(umpires[position])
    }

    override fun getItemCount(): Int = umpires.size

    inner class UmpireViewHolder(private val binding: ItemUmpireBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(umpire: Umpire) {
            binding.tvUmpireName.text = umpire.name
            binding.tvCertification.text = umpire.certificationLevel
            binding.tvUmpireLocation.text = umpire.location
            binding.tvUmpireExp.text = "${umpire.experienceYears} Yrs / ${umpire.matchesOfficiated} M"
            binding.tvUmpirePrice.text = "₹${umpire.pricePerMatch}"
            binding.tvUmpireRating.text = umpire.rating.toString()
            binding.tvUmpireStatus.text = umpire.availabilityStatus

            binding.root.setOnClickListener { onItemClick(umpire) }
        }
    }
}