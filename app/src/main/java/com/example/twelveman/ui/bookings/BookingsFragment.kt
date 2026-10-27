package com.example.twelveman.ui.bookings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.databinding.FragmentBookingsBinding

class BookingsFragment : Fragment() {
    private var _binding: FragmentBookingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookingsViewModel by viewModels()
    private lateinit var adapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookingAdapter(emptyList()) { _ -> }
        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@BookingsFragment.adapter
        }

        viewModel.bookings.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}