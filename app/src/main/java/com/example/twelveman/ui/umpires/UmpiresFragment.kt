package com.example.twelveman.ui.umpires

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentUmpiresBinding

class UmpiresFragment : Fragment() {
    private var _binding: FragmentUmpiresBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UmpiresViewModel by viewModels()
    private lateinit var adapter: UmpireAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUmpiresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UmpireAdapter(emptyList()) { umpire ->
            val bundle = Bundle().apply { putString("umpireId", umpire.id) }
            findNavController().navigate(R.id.action_umpires_to_details, bundle)
        }

        binding.rvUmpires.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@UmpiresFragment.adapter
        }

        viewModel.umpiresList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        binding.etSearchUmpires.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.fetchUmpires(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}