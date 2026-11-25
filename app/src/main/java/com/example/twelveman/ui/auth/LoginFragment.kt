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
import com.example.twelveman.databinding.FragmentLoginBinding
import com.example.twelveman.utils.Resource
import com.example.twelveman.utils.ServerConfigDialog

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            viewModel.login(email, pass)
        }

        binding.tvGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.btnGuestLogin.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_home)
        }

        binding.tvServerConfig.setOnClickListener {
            ServerConfigDialog.show(requireContext())
        }

        // Long press anywhere on the background to configure server IP
        binding.root.setOnLongClickListener {
            ServerConfigDialog.show(requireContext())
            true
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.btnLogin.isEnabled = false
                    binding.btnLogin.text = "Signing In..."
                }
                is Resource.Success -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Sign In"
                    val user = result.data
                    Toast.makeText(requireContext(), "Welcome back, ${user?.name ?: "User"}!", Toast.LENGTH_SHORT).show()
                    viewModel.resetAuthResult()
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is Resource.Error -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Sign In"
                    val message = result.message ?: "Authentication failed"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.resetAuthResult()
                }
                null -> {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Sign In"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
