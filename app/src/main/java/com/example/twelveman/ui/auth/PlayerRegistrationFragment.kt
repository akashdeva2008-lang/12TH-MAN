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
import com.example.twelveman.data.api.models.PlayerRegistrationRequest
import com.example.twelveman.data.local.SessionManager
import com.example.twelveman.data.model.User
import com.example.twelveman.databinding.FragmentPlayerRegistrationBinding
import com.example.twelveman.ui.players.PlayersViewModel
import java.util.Calendar
import java.util.Locale

class PlayerRegistrationFragment : Fragment() {
    private var _binding: FragmentPlayerRegistrationBinding? = null
    private val binding get() = _binding!!
    private val playersViewModel: PlayersViewModel by viewModels()
    private var selectedPhotoUri: String = ""
    private var isSubmitting = false

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && _binding != null) {
            selectedPhotoUri = uri.toString()
            binding.ivPlayerPhotoPreview.setImageURI(uri)
            binding.ivPlayerPhotoPreview.imageTintList = null
            binding.ivPlayerPhotoPreview.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivPlayerRegBack.setOnClickListener { findNavController().navigateUp() }

        val pickPhotoClick = View.OnClickListener { photoPicker.launch("image/*") }
        binding.ivPlayerPhotoPreview.setOnClickListener(pickPhotoClick)
        binding.btnPickPlayerPhoto.setOnClickListener(pickPhotoClick)

        binding.etPlayerDob.setOnClickListener { showDobPicker() }

        setupOptionSelector(binding.etPlayerRole, "Select Playing Role", arrayOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper"))
        setupOptionSelector(binding.etPlayerBattingStyle, "Select Batting Style", arrayOf("Right Hand", "Left Hand"))
        setupOptionSelector(
            binding.etPlayerBowlingStyle,
            "Select Bowling Style",
            arrayOf("Right Arm Fast", "Left Arm Fast", "Right Arm Spin", "Left Arm Spin", "Not Applicable")
        )
        setupOptionSelector(binding.etPlayerFormat, "Preferred Match Format", arrayOf("T10", "T20", "ODI", "Test"))
        setupOptionSelector(binding.etPlayerAvailability, "Availability", arrayOf("Available", "Unavailable"))

        binding.btnSubmitPlayerReg.setOnClickListener { submitRegistration() }
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
                binding.etPlayerDob.setText(String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year))
            },
            cal.get(Calendar.YEAR) - 20,
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun submitRegistration() {
        if (isSubmitting) return

        val name = binding.etPlayerFullName.text.toString().trim()
        val dob = binding.etPlayerDob.text.toString().trim()
        val phone = binding.etPlayerPhone.text.toString().trim()
        val email = binding.etPlayerEmail.text.toString().trim()
        val pass = binding.etPlayerPassword.text.toString()
        val confirm = binding.etPlayerConfirmPassword.text.toString()
        val city = binding.etPlayerCity.text.toString().trim().ifBlank { "Mumbai, MH" }

        if (name.isEmpty()) { binding.etPlayerFullName.error = "Full name is required"; return }
        if (dob.isEmpty()) { binding.etPlayerDob.error = "Date of birth is required"; return }
        if (phone.isEmpty()) { binding.etPlayerPhone.error = "Phone number is required"; return }
        if (email.isEmpty()) { binding.etPlayerEmail.error = "Email address is required"; return }
        if (pass.length < 6) { binding.etPlayerPassword.error = "Password must be at least 6 characters"; return }
        if (pass != confirm) { binding.etPlayerConfirmPassword.error = "Passwords do not match"; return }

        isSubmitting = true
        binding.btnSubmitPlayerReg.isEnabled = false
        binding.btnSubmitPlayerReg.text = "Registering Player..."

        val request = PlayerRegistrationRequest(
            fullName = name,
            email = email,
            phone = phone,
            password = pass,
            dob = dob,
            city = city,
            profileImage = selectedPhotoUri,
            playingRole = binding.etPlayerRole.text.toString().trim().ifBlank { "Batsman" },
            battingStyle = binding.etPlayerBattingStyle.text.toString().trim().ifBlank { "Right Hand" },
            bowlingStyle = binding.etPlayerBowlingStyle.text.toString().trim().ifBlank { "Right Arm Fast" },
            experience = binding.etPlayerExperience.text.toString().trim().ifBlank { "1+ Years" },
            preferredFormat = binding.etPlayerFormat.text.toString().trim().ifBlank { "T20" },
            availabilityStatus = binding.etPlayerAvailability.text.toString().trim().ifBlank { "Available" },
            bio = binding.etPlayerBio.text.toString().trim()
        )

        playersViewModel.registerPlayer(request) { result ->
            if (!isAdded) return@registerPlayer
            isSubmitting = false
            binding.btnSubmitPlayerReg.isEnabled = true
            binding.btnSubmitPlayerReg.text = "Register as Player"

            result.onSuccess { authResp ->
                val data = authResp.data
                if (data != null) {
                    SessionManager.getInstance(requireContext()).saveUser(
                        User(
                            id = data.id.toString(),
                            name = data.fullName,
                            email = data.email,
                            phone = data.phone,
                            role = "PLAYER"
                        ),
                        data.token
                    )
                }
                Toast.makeText(requireContext(), "Player registered successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_player_reg_to_player_dashboard)
            }.onFailure { err ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Registration Failed")
                    .setMessage(err.localizedMessage ?: "Unable to register player.")
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
