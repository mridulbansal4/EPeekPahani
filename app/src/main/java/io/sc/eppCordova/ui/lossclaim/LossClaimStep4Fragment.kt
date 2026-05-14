package io.sc.eppCordova.ui.lossclaim

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.R
import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.remote.dto.ClaimRequest
import io.sc.eppCordova.data.repository.ApiResult
import io.sc.eppCordova.data.repository.ClaimsRepository
import io.sc.eppCordova.databinding.FragmentLossClaimStep4Binding
import io.sc.eppCordova.ui.SharedViewModel
import io.sc.eppCordova.utils.OfflineBannerHelper
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LossClaimStep4Fragment : Fragment() {

    private var _binding: FragmentLossClaimStep4Binding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    @Inject
    lateinit var offlineBannerHelper: OfflineBannerHelper

    @Inject
    lateinit var claimsRepository: ClaimsRepository

    @Inject
    lateinit var cropRecordDao: CropRecordDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLossClaimStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offlineBannerHelper.attach(binding.offlineBanner.root, viewLifecycleOwner)

        binding.cbConsent.setOnCheckedChangeListener { _, isChecked ->
            binding.btnSubmit.isEnabled = isChecked
        }

        binding.btnSubmit.setOnClickListener {
            submitClaim()
        }

        binding.btnSaveDraft.setOnClickListener {
            findNavController().navigate(R.id.action_lossClaimStep4_to_dashboardFragment)
        }
    }

    private fun submitClaim() {
        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "सबमिट करत आहे..."

        lifecycleScope.launch {
            val farmer = cropRecordDao.getFarmer()
            val farmerId = farmer?.userId ?: ""
            val village = farmer?.village ?: ""

            val claimData = sharedViewModel.claimFormData.value
            val evidenceUrls = sharedViewModel.claimEvidenceUris.value

            val request = ClaimRequest(
                farmerId = farmerId,
                cropType = claimData?.cropType ?: "",
                claimType = claimData?.lossType ?: "",
                village = village,
                latitude = 0.0,
                longitude = 0.0,
                incidentDate = claimData?.incidentDate ?: "",
                affectedAreaHa = claimData?.affectedAreaHa ?: 0.0,
                gatNumber = claimData?.gatNumber ?: "",
                description = "Loss claim for ${claimData?.cropName}",
                evidenceUrls = evidenceUrls?.takeIf { it.isNotEmpty() }
            )

            when (val result = claimsRepository.submitClaim(request)) {
                is ApiResult.Success -> {
                    findNavController().navigate(R.id.action_lossClaimStep4_to_lossClaimStep5)
                }
                is ApiResult.Error -> {
                    binding.btnSubmit.isEnabled = true
                    binding.btnSubmit.text = "सबमिट करा"
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG)
                        .setAction("पुन्हा प्रयत्न") { submitClaim() }
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
