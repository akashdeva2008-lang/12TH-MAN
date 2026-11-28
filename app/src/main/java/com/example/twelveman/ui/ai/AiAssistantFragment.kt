package com.example.twelveman.ui.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.ai.ModelDownloadManager
import com.example.twelveman.ai.ModelDownloadState
import com.example.twelveman.ai.SmolLMInferenceEngine
import com.example.twelveman.ai.SmolLMModelManager
import com.example.twelveman.databinding.FragmentAiAssistantBinding

class AiAssistantFragment : Fragment() {

    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AiAssistantViewModel by viewModels()

    private lateinit var chatAdapter: AiChatAdapter

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull()?.trim()
            if (!spokenText.isNullOrEmpty() && _binding != null) {
                binding.etChatInput.setText(spokenText)
                binding.etChatInput.setSelection(spokenText.length)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        chatAdapter = AiChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter
            itemAnimator = null
            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom && chatAdapter.itemCount > 0) {
                    post {
                        smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Model Architecture Details Dialog
        binding.btnModelInfo.setOnClickListener {
            showModelInfoDialog()
        }

        // Reset Conversation
        binding.btnClearChat.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset Chat")
                .setMessage("Clear conversation history and restart?")
                .setPositiveButton("Reset") { _, _ ->
                    viewModel.clearHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Multi-line wrapping while keeping IME Send action button
        binding.etChatInput.setHorizontallyScrolling(false)
        binding.etChatInput.maxLines = 4

        // Plus (+) Quick Cricket Topics / Attachment Menu
        binding.btnAttachPrompt.setOnClickListener {
            val topics = arrayOf(
                "Regenerate Last Response",
                "Ask about MCC Cricket Rules & Laws",
                "Ask about T20 Powerplay & Fielding Restrictions",
                "Ask about Strike Rate & Economy Calculations",
                "Ask about Captaincy & Field Placements"
            )
            val prompts = arrayOf(
                "",
                "Explain the key MCC cricket laws every umpire should know.",
                "What are the T20 Powerplay and fielding circle restrictions?",
                "How are batting strike rate and bowling economy calculated?",
                "Suggest attacking field placements for a fast bowler in Powerplay."
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Assistant Options & Topics")
                .setItems(topics) { _, which ->
                    if (which == 0) {
                        viewModel.regenerateLastResponse()
                    } else {
                        binding.etChatInput.setText(prompts[which])
                        binding.etChatInput.setSelection(prompts[which].length)
                        binding.etChatInput.requestFocus()
                    }
                }
                .show()
        }

        // Stop Generation Button
        binding.btnStopGeneration.setOnClickListener {
            viewModel.stopGeneration()
        }

        // Cancel Model Download Button
        binding.btnCancelDownload.setOnClickListener {
            viewModel.cancelModelDownload()
        }

        // Microphone Voice Input
        binding.btnVoiceInput.setOnClickListener {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask 12THMAN GPT-1 about cricket...")
                }
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Voice input not available on this device", Toast.LENGTH_SHORT).show()
            }
        }

        // Send Message
        binding.btnSendMessage.setOnClickListener {
            handleSend()
        }

        // IME Send Action
        binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSend()
                true
            } else {
                false
            }
        }

        // Quick Prompt Chips
        binding.chipLbw.setOnClickListener {
            submitQuickPrompt("What is an LBW under MCC Law 36?")
        }
        binding.chipInswinger.setOnClickListener {
            submitQuickPrompt("Explain the difference between an inswinger and outswinger.")
        }
        binding.chipPowerplay.setOnClickListener {
            submitQuickPrompt("What are the mandatory Powerplay rules in T20 cricket?")
        }
        binding.chipYorker.setOnClickListener {
            submitQuickPrompt("What is a yorker delivery and when should it be bowled?")
        }
        binding.chipEconomy.setOnClickListener {
            submitQuickPrompt("How is a bowler's economy rate calculated in cricket?")
        }
        binding.chipTwelfthMan.setOnClickListener {
            submitQuickPrompt("What is the role of the 12th man in a cricket squad?")
        }
    }

    private fun handleSend() {
        val query = binding.etChatInput.text.toString().trim()
        if (query.isNotEmpty()) {
            binding.etChatInput.text.clear()
            viewModel.sendMessage(query)
        }
    }

    private fun submitQuickPrompt(promptText: String) {
        viewModel.sendMessage(promptText)
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.rvChatMessages.post {
                        binding.rvChatMessages.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.layoutThinking.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSendMessage.isEnabled = !isLoading
            binding.btnSendMessage.alpha = if (isLoading) 0.5f else 1.0f
        }

        viewModel.downloadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ModelDownloadState.NotDownloaded -> {
                    val freeMb = viewModel.getAvailableStorageMb()
                    binding.cardModelStatusBanner.visibility = View.VISIBLE
                    binding.tvModelStatusTitle.text = "Enable SmolLM2-1.7B On-Device AI"
                    binding.tvModelStatusSubtitle.text = "Model: 1.06 GB (Q4_K_M) • Free Storage: $freeMb MB"
                    binding.pbModelDownload.visibility = View.GONE
                    binding.btnCancelDownload.visibility = View.GONE
                    binding.btnEnableDownloadModel.visibility = View.VISIBLE
                    binding.btnEnableDownloadModel.text = "Download"
                    binding.btnEnableDownloadModel.setOnClickListener {
                        promptDownloadConsent()
                    }
                    binding.tvModelRibbon.text = "SmolLM2-1.7B Not Downloaded • Tap Download (1.06 GB)"
                }
                is ModelDownloadState.Downloading -> {
                    val dlMb = state.downloadedBytes / (1024L * 1024L)
                    val totMb = (state.totalBytes / (1024L * 1024L)).coerceAtLeast(1L)
                    binding.cardModelStatusBanner.visibility = View.VISIBLE
                    binding.tvModelStatusTitle.text = "Downloading SmolLM2-1.7B (${state.progressPercent}%)"
                    binding.tvModelStatusSubtitle.text = "$dlMb MB / $totMb MB from Hugging Face"
                    binding.pbModelDownload.visibility = View.VISIBLE
                    binding.pbModelDownload.isIndeterminate = false
                    binding.pbModelDownload.progress = state.progressPercent
                    binding.btnEnableDownloadModel.visibility = View.GONE
                    binding.btnCancelDownload.visibility = View.VISIBLE
                    binding.tvModelRibbon.text = "Downloading SmolLM2-1.7B: ${state.progressPercent}%"
                }
                is ModelDownloadState.Downloaded, is ModelDownloadState.LoadingModel -> {
                    binding.cardModelStatusBanner.visibility = View.VISIBLE
                    binding.tvModelStatusTitle.text = "Loading SmolLM2-1.7B Engine..."
                    binding.tvModelStatusSubtitle.text = "Initializing llama.cpp (2048 context)"
                    binding.pbModelDownload.visibility = View.VISIBLE
                    binding.pbModelDownload.isIndeterminate = true
                    binding.btnEnableDownloadModel.visibility = View.GONE
                    binding.btnCancelDownload.visibility = View.GONE
                    binding.tvModelRibbon.text = "Loading SmolLM2-1.7B into memory..."
                }
                is ModelDownloadState.ModelReady -> {
                    binding.cardModelStatusBanner.visibility = View.GONE
                    binding.tvModelRibbon.text = "SmolLM2-1.7B-Instruct (Q4_K_M) • On-Device • Ready"
                }
                is ModelDownloadState.Error -> {
                    binding.cardModelStatusBanner.visibility = View.VISIBLE
                    binding.tvModelStatusTitle.text = "SmolLM2 Setup Issue"
                    binding.tvModelStatusSubtitle.text = state.message
                    binding.pbModelDownload.visibility = View.GONE
                    binding.btnCancelDownload.visibility = View.GONE
                    binding.btnEnableDownloadModel.visibility = View.VISIBLE
                    binding.btnEnableDownloadModel.text = "Retry"
                    binding.btnEnableDownloadModel.setOnClickListener {
                        if (ModelDownloadManager(requireContext()).isModelValid()) {
                            viewModel.loadSmolLMModel()
                        } else {
                            promptDownloadConsent()
                        }
                    }
                    binding.tvModelRibbon.text = "SmolLM2-1.7B • Action Required"
                }
            }
        }
    }

    private fun promptDownloadConsent() {
        val networkNote = if (viewModel.isOnMeteredNetwork()) {
            "You are currently on mobile data. Downloading ~1.06 GB may use your cellular data plan."
        } else {
            "You are connected to Wi-Fi. The model will be saved in app-private storage (~1.06 GB) and reused offline."
        }
        val freeMb = viewModel.getAvailableStorageMb()

        AlertDialog.Builder(requireContext())
            .setTitle("Download SmolLM2-1.7B-Instruct")
            .setMessage(
                "Model: ${ModelDownloadManager.MODEL_NAME}\n" +
                    "File Size: ~1.06 GB\n" +
                    "Available Storage: $freeMb MB\n\n" +
                    "$networkNote\n\nProceed with download?"
            )
            .setPositiveButton("Download Now") { _, _ ->
                viewModel.startModelDownload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showModelInfoDialog() {
        val stateLabel = when (viewModel.downloadState.value) {
            is ModelDownloadState.ModelReady -> "Loaded & Ready (On-Device)"
            is ModelDownloadState.Downloading -> "Downloading..."
            is ModelDownloadState.LoadingModel -> "Loading..."
            is ModelDownloadState.Downloaded -> "Downloaded (Pending Load)"
            is ModelDownloadState.Error -> "Error"
            else -> "Not Downloaded"
        }
        val message = buildString {
            append("• Model: HuggingFaceTB/SmolLM2-1.7B-Instruct\n")
            append("• Format: GGUF (Q4_K_M Quantization)\n")
            append("• Runtime: llama.cpp (Android JNI)\n")
            append("• Context Window: ${SmolLMModelManager.CONTEXT_SIZE} tokens\n")
            append("• Max Generation: ${SmolLMInferenceEngine.MAX_GENERATION_TOKENS} tokens\n")
            append("• Model Size: ~1.06 GB\n")
            append("• Status: $stateLabel\n")
            append("• System Info: ${SmolLMModelManager.getSystemInfoSafe()}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("12THMAN GPT-1 • SmolLM2 Engine")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
