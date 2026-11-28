package com.example.twelveman.ui.umpires

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
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.databinding.FragmentUmpireRoleDashboardBinding

class UmpireRoleDashboardFragment : Fragment() {
    private var _binding: FragmentUmpireRoleDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UmpiresViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUmpireRoleDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager.getInstance(requireContext())
        val user = session.getCurrentUser()
        if (user == null || !user.role.equals("UMPIRE", ignoreCase = true)) {
            Toast.makeText(requireContext(), "Umpire Dashboard is restricted to registered Umpires.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.homeFragment)
            return
        }

        binding.tvUmpireDashName.text = user.name
        binding.ivUmpireRoleDashBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnUmpireDashHome.setOnClickListener { findNavController().navigate(R.id.homeFragment) }

        val userIdInt = user.id.toIntOrNull() ?: 0
        viewModel.loadMyUmpireProfile(userIdInt)

        viewModel.myUmpireProfile.observe(viewLifecycleOwner) { data ->
            if (data == null) return@observe
            val u = data["user"] as? Map<*, *>
            val p = data["profile"] as? Map<*, *>
            val name = u?.get("full_name")?.toString() ?: user.name
            val city = p?.get("city")?.toString() ?: u?.get("city")?.toString() ?: "Mumbai, MH"
            val cert = p?.get("certification")?.toString() ?: "State Level"
            val expYears = p?.get("experience_years")?.toString()?.toDoubleOrNull()?.toInt() ?: 1
            val matches = p?.get("matches_officiated")?.toString()?.toDoubleOrNull()?.toInt() ?: 0
            val formats = p?.get("preferred_formats")?.toString() ?: "T20, ODI"
            val langs = p?.get("languages_known")?.toString() ?: "English, Hindi"
            val avail = p?.get("availability_status")?.toString() ?: "Available"
            val bio = p?.get("bio")?.toString() ?: ""

            binding.tvUmpireDashName.text = name
            binding.tvUmpireDashCertCity.text = "$cert • $city"
            binding.tvUmpireDashAvail.text = avail
            binding.tvUmpireDashDetails.text = buildString {
                append("• Experience: $expYears Years\n")
                append("• Matches Officiated: $matches\n")
                append("• Preferred Formats: $formats\n")
                append("• Languages: $langs\n")
                if (bio.isNotBlank()) append("• Bio: $bio")
            }

            binding.btnEditUmpireProfile.setOnClickListener {
                showEditDialog(name, city, cert, expYears, matches, formats, langs, avail, bio)
            }
        }

        viewModel.myUmpireBookings.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) {
                binding.tvUmpireRequestsInfo.text = list.take(5).joinToString("\n\n") { b ->
                    "${b.itemName} • ${b.date} (${b.timeSlot})\nVenue: ${b.venue} • Status: ${b.status}"
                }
            } else {
                binding.tvUmpireRequestsInfo.text = "No officiating requests received yet."
            }
        }
    }

    private fun showEditDialog(
        name: String, city: String, cert: String, expYears: Int, matches: Int,
        formats: String, langs: String, avail: String, bio: String
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
            .setTitle("Edit Umpire Profile")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val payload = mapOf(
                    "full_name" to etName.text.toString().trim().ifBlank { name },
                    "city" to etCity.text.toString().trim().ifBlank { city },
                    "certification" to cert,
                    "experience_years" to expYears,
                    "matches_officiated" to matches,
                    "preferred_formats" to formats,
                    "languages_known" to langs,
                    "availability_status" to etAvail.text.toString().trim().ifBlank { avail },
                    "bio" to etBio.text.toString().trim()
                )
                viewModel.updateMyUmpireProfile(payload) { res ->
                    if (!isAdded) return@updateMyUmpireProfile
                    res.onSuccess {
                        Toast.makeText(ctx, "Umpire profile updated!", Toast.LENGTH_SHORT).show()
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
