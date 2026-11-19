package com.example.twelveman.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvProfileName.text = it.name
                binding.tvProfileRole.text = "Role: ${it.role}"
                binding.tvProfileEmail.text = "Email: ${it.email}"
                binding.tvProfilePhone.text = "Phone: ${it.phone}"
            }
        }

        binding.ivProfileBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Settings navigation
        val goToSettings = View.OnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        binding.btnGoToSettings.setOnClickListener(goToSettings)
        binding.ivProfileTopSettings.setOnClickListener(goToSettings)

        // My Teams
        binding.rowMyTeams.setOnClickListener {
            val teams = arrayOf(
                "Red Warriors (Captain: Rahul Sharma)",
                "Mumbai Superstars (Vice Captain: S. Iyer)",
                "Chembur Strikers (Player: Rahul Sharma)"
            )
            AlertDialog.Builder(requireContext())
                .setTitle("My Registered Teams")
                .setItems(teams) { _, which ->
                    Toast.makeText(requireContext(), "Selected: ${teams[which]}", Toast.LENGTH_SHORT).show()
                }
                .setPositiveButton("Register New Team") { _, _ ->
                    val bundle = Bundle().apply { putString("tournamentId", "T301") }
                    findNavController().navigate(R.id.action_profile_to_team_registration, bundle)
                }
                .setNegativeButton("Close", null)
                .show()
        }

        // My Statistics
        binding.rowMyStatistics.setOnClickListener {
            val user = viewModel.user.value
            AlertDialog.Builder(requireContext())
                .setTitle("Cricket Career Performance")
                .setMessage(
                    "Player: ${user?.name ?: "Rahul Sharma"}\n" +
                    "Primary Role: ${user?.role ?: "PLAYER"}\n\n" +
                    "• Matches Played: 24\n" +
                    "• Total Runs: 680\n" +
                    "• Batting Average: 34.0\n" +
                    "• Strike Rate: 138.2\n" +
                    "• High Score: 84*\n" +
                    "• Half Centuries (50s): 4\n" +
                    "• Total Wickets: 14\n" +
                    "• Bowling Economy: 7.15\n" +
                    "• Catches / Run Outs: 11\n" +
                    "• Player Rating: 4.8 ★"
                )
                .setPositiveButton("Close", null)
                .show()
        }

        // My Matches
        binding.rowMyMatches.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_bookings)
        }

        // Help and Support
        binding.rowHelpSupport.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("12THMAN Support & Helpdesk")
                .setMessage(
                    "Need assistance with booking players, umpires, or tournament scoring?\n\n" +
                    "• Email Support: support@12thman.cricket\n" +
                    "• WhatsApp Hotline: +91 98200 11223\n" +
                    "• Office Hours: Monday - Saturday (9:00 AM - 8:00 PM)\n" +
                    "• Dispute Resolution: grievances@12thman.cricket\n\n" +
                    "Version 1.0.0 (Official Stable Release)"
                )
                .setPositiveButton("OK", null)
                .show()
        }

        // Logout with Confirmation Dialog
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to sign out of 12THMAN?")
                .setPositiveButton("Logout") { _, _ ->
                    viewModel.logout()
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_profile_to_login)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
