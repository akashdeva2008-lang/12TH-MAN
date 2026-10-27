package com.example.twelveman.ui.umpires

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.twelveman.R
import com.example.twelveman.databinding.FragmentUmpireDetailsBinding

class UmpireDetailsFragment : Fragment() {
    private var _binding: FragmentUmpireDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UmpiresViewModel by viewModels()
    private var umpireId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUmpireDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            umpireId = it.getString("umpireId", "U201")
        }

        viewModel.loadUmpireDetails(umpireId)
        viewModel.selectedUmpire.observe(viewLifecycleOwner) { umpire ->
            umpire?.let { u ->
                binding.tvUmpireDetailName.text = u.name
                binding.tvUmpireDetailCert.text = "${u.certificationLevel} • ${u.location}"
                binding.tvUmpireDetailExp.text = "${u.experienceYears} Yrs"
                binding.tvUmpireDetailMatches.text = u.matchesOfficiated.toString()
                binding.tvUmpireDetailRating.text = "${u.rating} ★"
                binding.tvUmpireDetailBio.text = u.bio
            }
        }

        binding.btnHireUmpire.setOnClickListener {
            val bundle = Bundle().apply {
                putString("itemId", umpireId)
                putString("itemType", "UMPIRE")
            }
            findNavController().navigate(R.id.action_umpire_details_to_booking, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}