package com.example.twelveman.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentRegisterBinding
import com.example.twelveman.utils.Resource
import com.example.twelveman.utils.ServerConfigDialog

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            viewModel.register(name, email, phone, "PLAYER", pass)
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigateUp()
        }

        // Long press anywhere on the background to configure server IP
        binding.root.setOnLongClickListener {
            ServerConfigDialog.show(requireContext())
            true
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnRegister.isEnabled = false
                    binding.btnRegister.text = "Creating Account..."
                }
                is Resource.Success -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Create Account"
                    Toast.makeText(requireContext(), "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAuthResult()
                    findNavController().navigate(R.id.action_register_to_role)
                }
                is Resource.Error -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Create Account"
                    val message = result.message ?: "Registration failed"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.resetAuthResult()
                }
                null -> {
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Create Account"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
