package com.example.twelveman.ui.bookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.twelveman.data.model.Booking
import com.example.twelveman.databinding.ItemBookingBinding

class BookingAdapter(
    private var bookings: List<Booking>,
    private val onItemClick: (Booking) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    fun updateData(newBookings: List<Booking>) {
        bookings = newBookings
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
            binding.tvBookingTitle.text = booking.itemName
            binding.tvBookingVenue.text = booking.venue
            binding.tvBookingStatus.text = booking.status
            binding.tvBookingDateTime.text = "${booking.date} • ${booking.timeSlot}"
            binding.tvBookingAmount.text = "₹${booking.amount}"

            binding.root.setOnClickListener { onItemClick(booking) }
        }
    }
}