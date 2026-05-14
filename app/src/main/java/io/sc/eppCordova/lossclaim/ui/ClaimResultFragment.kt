package io.sc.eppCordova.lossclaim.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.sc.eppCordova.databinding.FragmentClaimResultBinding
import io.sc.eppCordova.lossclaim.viewmodel.LossClaimViewModel
import io.sc.eppCordova.utils.PdfGenerator
import java.io.File

class ClaimResultFragment : Fragment() {

    private var _binding: FragmentClaimResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LossClaimViewModel by activityViewModels()
    private var generatedPdfFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClaimResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Generate data based on Farmer responses and AI observation
        val damagePercent = if (viewModel.damagePercentageEstimate > 0) viewModel.damagePercentageEstimate else 65
        val payout = 30000.0 * (damagePercent / 100.0)
        
        binding.tvDamageScore.text = "$damagePercent%"
        binding.tvPayoutEstimate.text = "₹${payout.toInt()}"
        
        val farmer = viewModel.currentFarmer.value
        val photos = viewModel.capturedPhotos.value

        if (farmer != null) {
            generatedPdfFile = PdfGenerator.generateSurveyReport(
                requireContext(),
                farmer,
                photos,
                damagePercent,
                payout
            )
            
            if (generatedPdfFile != null) {
                // Change button to let user open PDF if they want
                binding.btnSubmitClaim.text = "Submit Claim & Save Report"
            }
        }

        binding.btnSubmitClaim.setOnClickListener {
            if (generatedPdfFile != null) {
                Toast.makeText(requireContext(), "Survey Report Saved! Claim Submitted.", Toast.LENGTH_LONG).show()
                viewModel.submitClaim(damagePercent, payout, generatedPdfFile!!.absolutePath)
                openPdf(generatedPdfFile!!)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Survey completed and data saved offline securely.", Toast.LENGTH_LONG).show()
                requireActivity().finish()
            }
        }
    }

    private fun openPdf(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}