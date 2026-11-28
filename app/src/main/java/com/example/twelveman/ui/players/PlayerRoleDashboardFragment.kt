package com.example.twelveman.ui.players

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.repository.BookingRepository
import com.example.twelveman.databinding.FragmentPlayerRoleDashboardBinding
import kotlinx.coroutines.launch

class PlayerRoleDashboardFragment : Fragment() {
    private var _binding: FragmentPlayerRoleDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlayersViewModel by viewModels()
    private val bookingRepository = BookingRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerRoleDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager.getInstance(requireContext())
        val user = session.getCurrentUser()
        if (user == null || !user.role.equals("PLAYER", ignoreCase = true)) {
            Toast.makeText(requireContext(), "Player Dashboard is restricted to registered Players.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.homeFragment)
            return
        }

        binding.tvPlayerDashName.text = user.name
        binding.ivPlayerDashBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnPlayerDashHome.setOnClickListener { findNavController().navigate(R.id.homeFragment) }

        val userIdInt = user.id.toIntOrNull() ?: 0
        viewModel.loadMyPlayerProfile(userIdInt)

        viewModel.myPlayerProfile.observe(viewLifecycleOwner) { data ->
            if (data == null) return@observe
            val u = data["user"] as? Map<*, *>
            val p = data["profile"] as? Map<*, *>
            val name = u?.get("full_name")?.toString() ?: user.name
            val city = p?.get("city")?.toString() ?: u?.get("city")?.toString() ?: "Mumbai, MH"
            val role = p?.get("playing_role")?.toString() ?: "Batsman"
            val bat = p?.get("batting_style")?.toString() ?: "Right Hand"
            val bowl = p?.get("bowling_style")?.toString() ?: "Right Arm Fast"
            val exp = p?.get("experience")?.toString() ?: "1+ Years"
            val fmt = p?.get("preferred_format")?.toString() ?: "T20"
            val avail = p?.get("availability_status")?.toString() ?: "Available"
            val matches = p?.get("matches_played")?.toString() ?: "0"
            val runs = p?.get("runs")?.toString() ?: "0"
            val wickets = p?.get("wickets")?.toString() ?: "0"
            val bio = p?.get("bio")?.toString() ?: ""

            binding.tvPlayerDashName.text = name
            binding.tvPlayerDashRoleCity.text = "$role • $city"
            binding.tvPlayerDashAvail.text = avail
            binding.tvPlayerDashDetails.text = buildString {
                append("• Batting Style: $bat\n")
                append("• Bowling Style: $bowl\n")
                append("• Experience: $exp\n")
                append("• Preferred Format: $fmt\n")
                append("• Stats: $matches Matches | $runs Runs | $wickets Wickets\n")
                if (bio.isNotBlank()) append("• Bio: $bio")
            }

            binding.btnEditPlayerProfile.setOnClickListener {
                showEditDialog(name, city, role, bat, bowl, exp, fmt, avail, bio)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bookingRepository.getBookings().collect { list ->
                val playerBookings = list.filter { it.bookingType.equals("PLAYER", ignoreCase = true) }
                if (playerBookings.isNotEmpty()) {
                    binding.tvPlayerBookingsInfo.text = playerBookings.take(5).joinToString("\n\n") { b ->
                        "${b.itemName} • ${b.date} (${b.timeSlot})\nVenue: ${b.venue} • Status: ${b.status}"
                    }
                } else {
                    binding.tvPlayerBookingsInfo.text = "No player booking requests found yet."
                }
            }
        }
    }

    private fun showEditDialog(
        name: String, city: String, role: String, bat: String, bowl: String,
        exp: String, fmt: String, avail: String, bio: String
    ) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
        }
        val etName = EditText(ctx).apply { hint = "Full Name"; setText(name) }
        val etCity = EditText(ctx).apply { hint = "City"; setText(city) }
        val etAvail = EditText(ctx).apply { hint = "Availability (Available / Unavailable)"; setText(avail) }
        val etBio = EditText(ctx).apply { hint = "Short Bio"; setText(bio) }
        container.addView(etName)
        container.addView(etCity)
        container.addView(etAvail)
        container.addView(etBio)

        AlertDialog.Builder(ctx)
            .setTitle("Edit Player Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val payload = mapOf(
                    "full_name" to etName.text.toString().trim().ifBlank { name },
                    "city" to etCity.text.toString().trim().ifBlank { city },
                    "playing_role" to role,
                    "batting_style" to bat,
                    "bowling_style" to bowl,
                    "experience" to exp,
                    "preferred_format" to fmt,
                    "availability_status" to etAvail.text.toString().trim().ifBlank { avail },
                    "bio" to etBio.text.toString().trim()
                )
                viewModel.updateMyPlayerProfile(payload) { res ->
                    if (!isAdded) return@updateMyPlayerProfile
                    res.onSuccess {
                        Toast.makeText(ctx, "Player profile updated!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(ctx, err.localizedMessage ?: "Update failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
