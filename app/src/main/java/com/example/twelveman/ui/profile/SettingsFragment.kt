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
import com.example.twelveman.utils.ThemeManager

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

        binding.ivSettingsBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutProfileOption.setOnClickListener {
            findNavController().popBackStack()
        }

        // Initialize Theme Mode Radio Buttons
        val currentTheme = ThemeManager.getTheme(requireContext())
        when (currentTheme) {
            ThemeManager.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            ThemeManager.THEME_DARK -> binding.rbThemeDark.isChecked = true
            ThemeManager.THEME_SYSTEM -> binding.rbThemeSystem.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }

        binding.rgThemeMode.setOnCheckedChangeListener { _, checkedId ->
            val (selectedMode, modeName) = when (checkedId) {
                binding.rbThemeLight.id -> Pair(ThemeManager.THEME_LIGHT, "Light Mode")
                binding.rbThemeDark.id -> Pair(ThemeManager.THEME_DARK, "Dark Mode")
                binding.rbThemeSystem.id -> Pair(ThemeManager.THEME_SYSTEM, "System Default")
                else -> Pair(ThemeManager.THEME_SYSTEM, "System Default")
            }

            if (selectedMode != ThemeManager.getTheme(requireContext())) {
                ThemeManager.setTheme(requireContext(), selectedMode)
                Toast.makeText(requireContext(), "$modeName applied immediately!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
            Toast.makeText(requireContext(), "Notifications ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchLocation.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleLocation(isChecked)
            Toast.makeText(requireContext(), "Location Services ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.layoutCurrency.setOnClickListener {
            val currencies = arrayOf("INR (₹)", "USD ($)", "GBP (£)", "AUD ($)")
            AlertDialog.Builder(requireContext())
                .setTitle("Select Currency Preference")
                .setItems(currencies) { _, which ->
                    val selected = currencies[which]
                    viewModel.setCurrency(selected)
                    binding.tvCurrencyValue.text = selected
                    Toast.makeText(requireContext(), "Currency set to $selected", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        binding.layoutClearCache.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Clear App Cache")
                .setMessage("Are you sure you want to clear temporary cached match and player images?")
                .setPositiveButton("Clear Now") { _, _ ->
                    try {
                        requireContext().cacheDir.deleteRecursively()
                        Toast.makeText(requireContext(), "Cache cleared successfully (0.0 MB)", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Configure Server IP") { _, _ ->
                    com.example.twelveman.utils.ServerConfigDialog.show(requireContext())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.layoutTerms.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Terms & Conditions")
                .setMessage("Welcome to 12THMAN Cricket Management Platform.\n\n1. Fair Play: All players and match organizers must uphold the spirit of cricket.\n2. Match Bookings: Umpires and players must be notified in advance for scheduled matches.\n3. Scoring: Live ball-by-ball updates must adhere to official ICC laws of cricket.\n4. Privacy: User credentials and tournament fixtures are secured under end-to-end authentication.")
                .setPositiveButton("I Agree") { dialog, _ -> dialog.dismiss() }
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
