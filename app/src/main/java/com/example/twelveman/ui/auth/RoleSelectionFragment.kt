package com.example.twelveman.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment() {
    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

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

        binding.ivJoinBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val goPlayer = View.OnClickListener {
            findNavController().navigate(R.id.action_role_to_player_registration)
        }
        binding.cardRolePlayer.setOnClickListener(goPlayer)
        binding.btnRegisterAsPlayer.setOnClickListener(goPlayer)

        val goUmpire = View.OnClickListener {
            findNavController().navigate(R.id.action_role_to_umpire_registration)
        }
        binding.cardRoleUmpire.setOnClickListener(goUmpire)
        binding.btnRegisterAsUmpire.setOnClickListener(goUmpire)

        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
