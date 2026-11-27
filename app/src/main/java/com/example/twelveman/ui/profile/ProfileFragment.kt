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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.DialogMyRegisteredTeamsBinding
import com.example.twelveman.databinding.FragmentProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

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
            if (user != null) {
                binding.tvProfileName.text = user.name
                binding.tvProfileRole.text = "Role: ${user.role}"
                binding.tvProfileEmail.text = "Email: ${user.email}"
                binding.tvProfilePhone.text = if (user.phone.isNotBlank()) "Phone: ${user.phone}" else "Phone: Not provided"
            } else {
                binding.tvProfileName.text = "Guest User"
                binding.tvProfileRole.text = "Role: Guest"
                binding.tvProfileEmail.text = "Email: Not signed in"
                binding.tvProfilePhone.text = "Phone: -"
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

        // My Teams -> Show actual registered teams
        binding.rowMyTeams.setOnClickListener {
            showMyRegisteredTeamsDialog()
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

    private fun showMyRegisteredTeamsDialog() {
        viewModel.fetchMyRegisteredTeams()

        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogMyRegisteredTeamsBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        val adapter = MyRegisteredTeamAdapter(emptyList()) { team ->
            Toast.makeText(requireContext(), "Team: ${team.teamName} (${team.approvalStatus})", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.rvMyRegisteredTeams.layoutManager = LinearLayoutManager(requireContext())
        dialogBinding.rvMyRegisteredTeams.adapter = adapter

        viewModel.isLoadingTeams.observe(viewLifecycleOwner) { loading ->
            dialogBinding.pbLoadingTeams.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.myRegisteredTeams.observe(viewLifecycleOwner) { teams ->
            adapter.updateData(teams)
            val count = teams.size
            dialogBinding.tvMyTeamsCount.text = if (count > 0) "$count Registered Teams" else "No Registered Teams"
            dialogBinding.tvFilterBadge.text = if (count > 0) "Showing $count Active Teams" else "No Teams Found"

            if (teams.isEmpty() && viewModel.isLoadingTeams.value != true) {
                dialogBinding.layoutEmptyTeams.visibility = View.VISIBLE
                dialogBinding.rvMyRegisteredTeams.visibility = View.GONE
            } else {
                dialogBinding.layoutEmptyTeams.visibility = View.GONE
                dialogBinding.rvMyRegisteredTeams.visibility = View.VISIBLE
            }
        }

        val navigateToRegister = View.OnClickListener {
            dialog.dismiss()
            val bundle = Bundle().apply { putString("tournamentId", "T301") }
            findNavController().navigate(R.id.action_profile_to_team_registration, bundle)
        }

        dialogBinding.btnRegisterNewTeamDialog.setOnClickListener(navigateToRegister)
        dialogBinding.btnEmptyRegisterTeam.setOnClickListener(navigateToRegister)
        dialogBinding.ivCloseMyTeamsDialog.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
