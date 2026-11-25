package com.example.twelveman.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.R
import com.example.twelveman.data.model.Booking
import com.example.twelveman.databinding.ItemBookingBinding

class BookingAdapter(
    private var bookings: List<Booking>,
    private val onItemClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings.distinctBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(bookings[position])
    }

    override fun getItemCount(): Int = bookings.size

    inner class BookingViewHolder(private val binding: ItemBookingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(booking: Booking) {
            val context = binding.root.context

            binding.tvBookingPlayerName.text = booking.playerName
            binding.tvBookingIdBadge.text = if (booking.id.startsWith("#")) booking.id else "#${booking.id}"
            binding.tvBookingRole.text = booking.playerRole
            binding.tvBookingCategoryTag.text = if (booking.isPlayerBooking) "• Player Booking" else "• Umpire Booking"
            binding.tvBookingVenue.text = booking.venue
            binding.tvBookingMatchType.text = if (booking.matchType.isNotBlank()) "${booking.matchType} Match" else "Cricket Match"
            binding.tvBookingDateTime.text = "${booking.date} • ${booking.timeSlot}"
            binding.tvBookingAmount.text = "₹${booking.amount}"
            binding.tvBookingPaymentStatus.text = "Payment: ${booking.paymentStatus}"

            // Icons
            if (booking.isPlayerBooking) {
                binding.ivBookingAvatar.setImageResource(R.drawable.ic_person)
                binding.ivBookingTypeMini.setImageResource(R.drawable.ic_cricket_bat)
            } else {
                binding.ivBookingAvatar.setImageResource(R.drawable.ic_person)
                binding.ivBookingTypeMini.setImageResource(R.drawable.ic_booking)
            }

            // Status Badge Coloring
            binding.tvBookingStatusBadge.text = booking.status.uppercase()
            when (booking.status.uppercase()) {
                "CONFIRMED" -> {
                    binding.tvBookingStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_outline)
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_red))
                }
                "PENDING" -> {
                    binding.tvBookingStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.header_dark_grey))
                }
                "CANCELLED" -> {
                    binding.tvBookingStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                }
                "COMPLETED" -> {
                    binding.tvBookingStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_inactive)
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.header_black))
                }
                else -> {
                    binding.tvBookingStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_outline)
                    binding.tvBookingStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_red))
                }
            }

            binding.root.setOnClickListener { onItemClick(booking) }
        }
    }
}
