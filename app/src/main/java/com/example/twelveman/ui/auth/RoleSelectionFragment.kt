package com.example.twelveman.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
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

        binding.cardRolePlayer.setOnClickListener {
            selectedRole = "PLAYER"
            Toast.makeText(requireContext(), "Selected: Cricket Player", Toast.LENGTH_SHORT).show()
        }

        binding.cardRoleUmpire.setOnClickListener {
            selectedRole = "UMPIRE"
            Toast.makeText(requireContext(), "Selected: Cricket Umpire", Toast.LENGTH_SHORT).show()
        }

        binding.cardRoleOrganizer.setOnClickListener {
            selectedRole = "ORGANIZER"
            Toast.makeText(requireContext(), "Selected: Tournament Organizer", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_role_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}