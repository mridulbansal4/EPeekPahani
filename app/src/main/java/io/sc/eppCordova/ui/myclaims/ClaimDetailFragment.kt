package io.sc.eppCordova.ui.myclaims

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.sc.eppCordova.databinding.FragmentClaimDetailBinding

@AndroidEntryPoint
class ClaimDetailFragment : Fragment() {

    private var _binding: FragmentClaimDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyClaimsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClaimDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.selectedClaim.observe(viewLifecycleOwner) { claim ->
            if (claim != null) {
                binding.tvDetail.text = buildString {
                    appendLine("दावा क्र: ${claim.claimId ?: "-"}")
                    appendLine()
                    appendLine("प्रक्रिया स्थिती: ${workflowLabel(claim.workflowStage)}")
                    appendLine("विश्वास गुण: ${claim.confidenceScore?.let { "$it%" } ?: "-"}")
                    appendLine("भू-सत्यापन: ${boolLabel(claim.geoVerified)}")
                    appendLine("पाऊस जुळणी: ${boolLabel(claim.rainfallMatched)}")
                    appendLine("डुप्लिकेट धोका: ${boolLabel(claim.duplicateRisk)}")
                    appendLine()
                    appendLine("नियुक्त अधिकारी: ${claim.assignedOfficer ?: "-"}")
                    appendLine("पुनरावलोकन: ${claim.reviewRemarks ?: "-"}")
                    appendLine("भूखंड: ${claim.landParcelId ?: "-"}")
                    appendLine()
                    appendLine("DBT स्थिती: ${claim.dbtStatus ?: "-"}")
                    appendLine("देयक स्थिती: ${claim.paymentStatus ?: "-"}")
                    appendLine()
                    appendLine("निर्मिती: ${claim.createdAt ?: "-"}")
                    appendLine("अद्यतन: ${claim.updatedAt ?: "-"}")
                }
            }
        }

        viewModel.selectedReport.observe(viewLifecycleOwner) { report ->
            if (report != null) {
                binding.tvDetail.append(buildString {
                    appendLine()
                    appendLine("=== बॅकएंड अहवाल ===")
                    appendLine("अहवाल क्र: ${report.reportId ?: "-"}")
                    appendLine("प्रक्रिया स्थिती: ${report.workflowStage ?: "-"}")
                    appendLine("विश्वास गुण: ${report.confidenceScore?.let { "$it%" } ?: "-"}")
                    appendLine("भू-सत्यापन: ${boolLabel(report.geoVerified)}")
                    appendLine("तीव्रता पातळी: ${report.severityLevel ?: "-"}")
                    appendLine("AI टिप्पण्या: ${report.aiRemarks ?: "-"}")
                    appendLine("अधिकारी टिप्पण्या: ${report.officerRemarks ?: "-"}")
                    appendLine("नियुक्त अधिकारी: ${report.assignedOfficer ?: "-"}")
                    appendLine("तक्रार दुवा: ${report.grievanceLinkage ?: "-"}")
                })
            }
        }
    }

    private fun workflowLabel(stage: String?): String = when {
        stage == null -> "नवीन"
        stage.contains("SUBMITTED", true) -> "Under Verification"
        stage.contains("GEO_VERIFIED", true) -> "Geo Verification Completed"
        stage.contains("OFFICER_REVIEW", true) -> "Awaiting Officer Review"
        stage.contains("DBT_INITIATED", true) -> "DBT Initiated"
        stage.contains("COMPENSATED", true) || stage.contains("RELEASED", true) ->
            "Compensation Released"
        stage.contains("REJECTED", true) -> "Rejected"
        else -> stage.replace("_", " ")
    }

    private fun boolLabel(value: Boolean?): String = when (value) {
        true -> "होय"
        false -> "नाही"
        null -> "-"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
