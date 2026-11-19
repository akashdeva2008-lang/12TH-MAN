package com.example.twelveman.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {
    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!
    private var selectedRole = "PLAYER"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoleSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectRole("PLAYER")

        binding.cardRolePlayer.setOnClickListener {
            selectRole("PLAYER")
            Toast.makeText(requireContext(), "Selected: Cricket Player", Toast.LENGTH_SHORT).show()
        }

        binding.cardRoleUmpire.setOnClickListener {
            selectRole("UMPIRE")
            Toast.makeText(requireContext(), "Selected: Cricket Umpire", Toast.LENGTH_SHORT).show()
        }

        binding.cardRoleOrganizer.setOnClickListener {
            selectRole("ORGANIZER")
            Toast.makeText(requireContext(), "Selected: Tournament Organizer", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinue.setOnClickListener {
            binding.btnContinue.isEnabled = false
            val sessionManager = SessionManager.getInstance(requireContext())
            val user = sessionManager.getCurrentUser()
            if (user != null) {
                sessionManager.saveUser(user.copy(role = selectedRole))
            }
            Toast.makeText(requireContext(), "Welcome to 12THMAN as $selectedRole!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_role_to_home)
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        val red = ContextCompat.getColor(requireContext(), R.color.primary_red)
        val borderGrey = ContextCompat.getColor(requireContext(), R.color.border_grey)

        // Reset all strokes
        binding.cardRolePlayer.strokeColor = borderGrey
        binding.cardRolePlayer.strokeWidth = 1
        binding.cardRoleUmpire.strokeColor = borderGrey
        binding.cardRoleUmpire.strokeWidth = 1
        binding.cardRoleOrganizer.strokeColor = borderGrey
        binding.cardRoleOrganizer.strokeWidth = 1

        when (role) {
            "PLAYER" -> {
                binding.cardRolePlayer.strokeColor = red
                binding.cardRolePlayer.strokeWidth = 4
                binding.btnContinue.text = "Continue as Cricket Player"
            }
            "UMPIRE" -> {
                binding.cardRoleUmpire.strokeColor = red
                binding.cardRoleUmpire.strokeWidth = 4
                binding.btnContinue.text = "Continue as Cricket Umpire"
            }
            "ORGANIZER" -> {
                binding.cardRoleOrganizer.strokeColor = red
                binding.cardRoleOrganizer.strokeWidth = 4
                binding.btnContinue.text = "Continue as Tournament Organizer"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
