package com.example.twelveman.ui.umpires

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.Booking
import com.example.twelveman.databinding.ItemUmpireBookingCardBinding

class UmpireBookingAdapter(
    private var bookings: List<Booking>,
    private val onContactClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit
) : RecyclerView.Adapter<UmpireBookingAdapter.BookingViewHolder>() {

    fun updateList(newList: List<Booking>) {
        bookings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemUmpireBookingCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(
        private val binding: ItemUmpireBookingCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(b: Booking) {
            binding.tvBookedUmpireName.text = b.playerName
            binding.tvBookingIdTag.text = "Booking ID: #${b.id} • Match Official"
            binding.tvBookingMatchName.text = if (b.notes.isNotBlank() && b.notes.contains("Match:")) {
                b.notes.substringAfter("Match:").substringBefore(";").trim()
            } else {
                "${b.matchType} Match"
            }
            binding.tvBookingDateTime.text = "${b.date} • ${b.timeSlot}"
            binding.tvBookingVenue.text = b.venue
            binding.tvBookingFeeAmount.text = "₹${b.amount} (${b.paymentStatus})"

            val statusUpper = b.status.uppercase()
            binding.tvBookingStatusBadge.text = statusUpper

            when (statusUpper) {
                "CONFIRMED" -> {
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_green))
                    binding.btnCancelUmpireBooking.visibility = View.VISIBLE
                }
                "PENDING" -> {
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.accent_gold))
                    binding.btnCancelUmpireBooking.visibility = View.VISIBLE
                }
                "CANCELLED" -> {
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                    binding.btnCancelUmpireBooking.visibility = View.GONE
                }
                else -> {
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(binding.root.context, R.color.primary_red))
                    binding.btnCancelUmpireBooking.visibility = View.GONE
                }
            }

            binding.btnContactUmpire.setOnClickListener { onContactClick(b) }
            binding.btnCancelUmpireBooking.setOnClickListener { onCancelClick(b) }
        }
    }
}
