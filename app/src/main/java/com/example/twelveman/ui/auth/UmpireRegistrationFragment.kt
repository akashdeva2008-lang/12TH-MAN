package com.example.twelveman.ui.auth

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.data.api.models.UmpireRegistrationRequest
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.User
import com.example.twelveman.databinding.FragmentUmpireRegistrationBinding
import com.example.twelveman.ui.umpires.UmpiresViewModel
import java.util.Calendar
import java.util.Locale

class UmpireRegistrationFragment : Fragment() {
    private var _binding: FragmentUmpireRegistrationBinding? = null
    private val binding get() = _binding!!
    private val umpiresViewModel: UmpiresViewModel by viewModels()
    private var selectedPhotoUri: String = ""
    private var isSubmitting = false

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && _binding != null) {
            selectedPhotoUri = uri.toString()
            binding.ivUmpirePhotoPreview.setImageURI(uri)
            binding.ivUmpirePhotoPreview.imageTintList = null
            binding.ivUmpirePhotoPreview.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUmpireRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivUmpireRegBack.setOnClickListener { findNavController().navigateUp() }

        val pickPhotoClick = View.OnClickListener { photoPicker.launch("image/*") }
        binding.ivUmpirePhotoPreview.setOnClickListener(pickPhotoClick)
        binding.btnPickUmpirePhoto.setOnClickListener(pickPhotoClick)

        binding.etUmpireDob.setOnClickListener { showDobPicker() }

        setupOptionSelector(
            binding.etUmpireCert,
            "Select Certification",
            arrayOf("ICC Accredited", "National Certified", "State Level", "Club Certified")
        )
        setupOptionSelector(
            binding.etUmpireFormats,
            "Preferred Match Formats",
            arrayOf("T10, T20", "T20, ODI", "All Formats (T10, T20, ODI, Test)", "Test & ODI")
        )
        setupOptionSelector(binding.etUmpireAvailability, "Availability", arrayOf("Available", "Unavailable"))

        binding.btnSubmitUmpireReg.setOnClickListener { submitRegistration() }
    }

    private fun setupOptionSelector(field: android.widget.EditText, title: String, options: Array<String>) {
        field.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(options) { _, which -> field.setText(options[which]) }
                .show()
        }
    }

    private fun showDobPicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                binding.etUmpireDob.setText(String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year))
            },
            cal.get(Calendar.YEAR) - 30,
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitRegistration() {
        if (isSubmitting) return

        val name = binding.etUmpireFullName.text.toString().trim()
        val dob = binding.etUmpireDob.text.toString().trim()
        val phone = binding.etUmpirePhone.text.toString().trim()
        val email = binding.etUmpireEmail.text.toString().trim()
        val pass = binding.etUmpirePassword.text.toString()
        val confirm = binding.etUmpireConfirmPassword.text.toString()
        val city = binding.etUmpireCity.text.toString().trim().ifBlank { "Mumbai, MH" }

        if (name.isEmpty()) { binding.etUmpireFullName.error = "Full name is required"; return }
        if (dob.isEmpty()) { binding.etUmpireDob.error = "Date of birth is required"; return }
        if (phone.isEmpty()) { binding.etUmpirePhone.error = "Phone number is required"; return }
        if (email.isEmpty()) { binding.etUmpireEmail.error = "Email address is required"; return }
        if (pass.length < 6) { binding.etUmpirePassword.error = "Password must be at least 6 characters"; return }
        if (pass != confirm) { binding.etUmpireConfirmPassword.error = "Passwords do not match"; return }

        isSubmitting = true
        binding.btnSubmitUmpireReg.isEnabled = false
        binding.btnSubmitUmpireReg.text = "Registering Umpire..."

        val expYears = binding.etUmpireExpYears.text.toString().trim().toIntOrNull() ?: 1
        val matches = binding.etUmpireMatches.text.toString().trim().toIntOrNull() ?: 0

        val request = UmpireRegistrationRequest(
            fullName = name,
            email = email,
            phone = phone,
            password = pass,
            dob = dob,
            city = city,
            profileImage = selectedPhotoUri,
            experienceYears = expYears,
            certification = binding.etUmpireCert.text.toString().trim().ifBlank { "State Level" },
            matchesOfficiated = matches,
            preferredFormats = binding.etUmpireFormats.text.toString().trim().ifBlank { "T20, ODI" },
            availabilityStatus = binding.etUmpireAvailability.text.toString().trim().ifBlank { "Available" },
            languagesKnown = binding.etUmpireLanguages.text.toString().trim().ifBlank { "English, Hindi" },
            bio = binding.etUmpireBio.text.toString().trim()
        )

        umpiresViewModel.registerUmpire(request) { result ->
            if (!isAdded) return@registerUmpire
            isSubmitting = false
            binding.btnSubmitUmpireReg.isEnabled = true
            binding.btnSubmitUmpireReg.text = "Register as Umpire"

            result.onSuccess { authResp ->
                val data = authResp.data
                if (data != null) {
                    SessionManager.getInstance(requireContext()).saveUser(
                        User(
                            id = data.id.toString(),
                            name = data.fullName,
                            email = data.email,
                            phone = data.phone,
                            role = "UMPIRE"
                        ),
                        data.token
                    )
                }
                Toast.makeText(requireContext(), "Umpire registered successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_umpire_reg_to_umpire_dashboard)
            }.onFailure { err ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Registration Failed")
                    .setMessage(err.localizedMessage ?: "Unable to register umpire.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
