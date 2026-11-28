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
                "Ask about MCC Cricket Rules & Laws",
                "Ask about T20 Powerplay & Fielding Restrictions",
                "Ask about Strike Rate & Economy Calculations",
                "Ask about Captaincy & Field Placements"
            )
            val prompts = arrayOf(
                "Explain the key MCC cricket laws every umpire should know.",
                "What are the T20 Powerplay and fielding circle restrictions?",
                "How are batting strike rate and bowling economy calculated?",
                "Suggest attacking field placements for a fast bowler in Powerplay."
            )
            AlertDialog.Builder(requireContext())
                .setTitle("Quick Cricket Topics")
                .setItems(topics) { _, which ->
                    binding.etChatInput.setText(prompts[which])
                    binding.etChatInput.setSelection(prompts[which].length)
                    binding.etChatInput.requestFocus()
                }
                .show()
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

        viewModel.modelInfo.observe(viewLifecycleOwner) { info ->
            info?.let {
                binding.tvModelRibbon.text =
                    "${it.parameters / 1000}K Params • ${it.nLayer} Layers • 0 Pretrained Weights • Val Loss: ${String.format("%.2f", it.valLoss)}"
            }
        }
    }

    private fun showModelInfoDialog() {
        val info = viewModel.modelInfo.value
        val message = buildString {
            append("• Name: 12THMAN GPT-1\n")
            append("• Architecture: Decoder-Only Transformer (GPT)\n")
            append("• Weights: Zero Pretrained (Initialized & Trained from Scratch)\n")
            append("• Parameters: ${info?.parameters ?: 910464}\n")
            append("• Transformer Layers: ${info?.nLayer ?: 4}\n")
            append("• Attention Heads: ${info?.nHead ?: 4}\n")
            append("• Embedding Dim: ${info?.nEmbd ?: 128}\n")
            append("• Context Window: ${info?.blockSize ?: 128} tokens\n")
            append("• Vocabulary: ${info?.vocabSize ?: 823} BPE tokens\n")
            append("• Checkpoint: ${info?.checkpointFile ?: "12thman_gpt1_best.pt"}\n")
            append("• Best Validation Loss: ${String.format("%.4f", info?.valLoss ?: 2.2984)}\n")
            append("• Target Device: PyTorch Engine")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("12THMAN GPT-1 Architecture")
            .setMessage(message)
            .setPositiveButton("Awesome", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
