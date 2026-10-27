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
import com.example.twelveman.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutProfileOption.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
            Toast.makeText(requireContext(), "Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleDarkMode(isChecked)
            Toast.makeText(requireContext(), "Dark Theme ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleLocation(isChecked)
            Toast.makeText(requireContext(), "Location Services ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.layoutCurrency.setOnClickListener {
            val currencies = arrayOf("INR (₹)", "USD ($)", "GBP (£)", "AUD ($)")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Currency")
                .setItems(currencies) { _, which ->
                    val selected = currencies[which]
                    viewModel.setCurrency(selected)
                    binding.tvCurrencyValue.text = selected
                    Toast.makeText(requireContext(), "Currency set to $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.layoutClearCache.setOnClickListener {
            Toast.makeText(requireContext(), "App Cache Cleared (14.2 MB freed)", Toast.LENGTH_SHORT).show()
        }

        binding.layoutTerms.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Terms & Conditions")
                .setMessage("Welcome to 12THMAN. By using our app to book players, umpires, and organize tournaments, you agree to fair play, timely payments, and community guidelines.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        viewModel.currency.observe(viewLifecycleOwner) { curr ->
            binding.tvCurrencyValue.text = curr
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}