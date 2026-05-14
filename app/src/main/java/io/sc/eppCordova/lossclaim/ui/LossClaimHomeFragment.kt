package io.sc.eppCordova.lossclaim.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentLossClaimHomeBinding
import io.sc.eppCordova.lossclaim.viewmodel.LossClaimViewModel
import io.sc.eppCordova.ui.SharedViewModel
import kotlinx.coroutines.launch

class LossClaimHomeFragment : Fragment() {

    private var _binding: FragmentLossClaimHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LossClaimViewModel by activityViewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLossClaimHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Mock load farmer (in reality from SharedViewModel after OTP)
        val mobile = sharedViewModel.farmerState.value?.mobile ?: "9876543210"
        viewModel.loadFarmerData(mobile)
        
        // Auto-detect disaster based on weather (PDF says "Pre-populates the disaster type suggestion")
        viewModel.setDamageType("Flood") // Simulating auto-detection

        lifecycleScope.launch {
            viewModel.currentFarmer.collect { farmer ->
                farmer?.let {
                    binding.tvFarmerName.text = it.farmerName
                    binding.tvMobileNumber.text = "+91 ${it.mobileNumber}"
                    binding.tvVillage.text = "${it.village}, ${it.taluka}, ${it.district}"
                    binding.tvGatNumber.text = it.gatNumber
                    
                    binding.tvPrimaryCrop.text = it.primaryCrop ?: "Unknown"
                    
                    val secCrop = it.secondaryCrop?.trim()
                    if (!secCrop.isNullOrEmpty() && secCrop.lowercase() != "none" && secCrop.lowercase() != "na" && secCrop.lowercase() != "null") {
                        binding.llSecondaryCrop.visibility = View.VISIBLE
                        binding.tvSecondaryCrop.text = secCrop
                    } else {
                        binding.llSecondaryCrop.visibility = View.GONE
                    }
                    
                    binding.tvLandArea.text = "${it.area} Hectares"
                    binding.tvInsurance.text = if (it.insuranceStatus) "PMFBY Active" else "Inactive"
                    binding.tvIrrigation.text = "Rainfed (Default)" // Not in entity, default value
                    binding.tvDisasterType.text = "Flood (AI Detected)"
                }
            }
        }

        binding.btnStartSurvey.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_camera)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}