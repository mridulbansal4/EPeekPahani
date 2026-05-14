package io.sc.eppCordova.lossclaim.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentClaimResultBinding
import io.sc.eppCordova.lossclaim.viewmodel.LossClaimViewModel
import io.sc.eppCordova.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        populateLocalData()
        setupActions()
        observeBackendReport()
    }

    private fun populateLocalData() {
        val pkg = viewModel.finalEvidencePackage
        val damagePercent = pkg?.estimatedDamagePercentage ?: 0
        val payout = 30000.0 * (damagePercent / 100.0)

        val farmer = viewModel.currentFarmer.value

        val timeStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val refId = "MH-${System.currentTimeMillis().toString().takeLast(6)}"

        binding.tvRefId.text = "Ref ID: $refId"
        binding.tvTimestamp.text = "Time: $timeStamp"

        if (farmer != null) {
            binding.tvVillageGat.text = "Village: ${farmer.village} | Gat: ${farmer.gatNumber}"
            binding.tvCropDisaster.text = "Crop: ${farmer.primaryCrop} | Disaster: ${pkg?.disasterType?.name ?: "Unknown"}"

            generatedPdfFile = PdfGenerator.generateSurveyReport(
                requireContext(),
                farmer,
                pkg,
                damagePercent,
                payout
            )
        }

        val firstTime = pkg?.photos?.firstOrNull()?.timestamp ?: 0L
        val lastTime = pkg?.photos?.lastOrNull()?.timestamp ?: 0L
        val duration = if (lastTime > firstTime) (lastTime - firstTime) / 1000 else 60

        binding.tvEvidenceCount.text = "Evidence Photos: ${pkg?.photos?.size ?: 0}"
        val videoCount = pkg?.videos?.size ?: 0
        binding.tvVideoCount.text = "Evidence Videos: $videoCount"
        if (videoCount > 0) {
            binding.badgeVideoVerified.visibility = View.VISIBLE
            binding.videoThumbnailContainer.visibility = View.VISIBLE
            val firstVideoPath = pkg?.videos?.firstOrNull()?.videoPath
            if (!firstVideoPath.isNullOrEmpty()) {
                com.bumptech.glide.Glide.with(this)
                    .load(File(firstVideoPath))
                    .into(binding.ivVideoThumbnail)
            }
        } else {
            binding.badgeVideoVerified.visibility = View.GONE
            binding.videoThumbnailContainer.visibility = View.GONE
        }
        binding.tvDuration.text = "Survey Time: ${duration}s"
    }

    private fun observeBackendReport() {
        viewModel.submitClaim()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.backendSubmitState.collect { state ->
                    when (state) {
                        is LossClaimViewModel.BackendSubmitState.IDLE -> {}
                        is LossClaimViewModel.BackendSubmitState.SUBMITTING -> {
                            binding.btnSubmitClaim.text = "Submitting..."
                            binding.btnSubmitClaim.isEnabled = false
                        }
                        is LossClaimViewModel.BackendSubmitState.FETCHING_REPORT -> {
                            binding.btnSubmitClaim.text = "Generating Report..."
                        }
                        is LossClaimViewModel.BackendSubmitState.SUCCESS -> {
                            binding.btnSubmitClaim.text = "Done"
                            binding.btnSubmitClaim.isEnabled = true
                            showReportFromBackend()
                        }
                        is LossClaimViewModel.BackendSubmitState.ERROR -> {
                            binding.btnSubmitClaim.text = "Done"
                            binding.btnSubmitClaim.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun showReportFromBackend() {
        val report = viewModel.generatedReport.value ?: return
        val tag = "backendReportContainer"
        val container = binding.root.findViewWithTag<LinearLayout>(tag)
            ?: createBackendReportContainer().apply { this.tag = tag }

        container.removeAllViews()
        container.visibility = View.VISIBLE

        addReportRow(container, "Report ID", report.reportId ?: "-")
        addReportRow(container, "Workflow Stage", report.workflowStage ?: "Under Verification")
        addReportRow(container, "Confidence Score", report.confidenceScore?.let { "$it%" } ?: "-")
        addReportRow(container, "Severity Level", report.severityLevel ?: "-")
        addReportRow(container, "Geo Verified", if (report.geoVerified == true) "Yes" else "No")
        addReportRow(container, "Assigned Officer", report.assignedOfficer ?: "To be assigned")
        addReportRow(container, "AI Remarks", report.aiRemarks ?: "-")
        addReportRow(container, "Officer Remarks", report.officerRemarks ?: "Pending")
        addReportRow(container, "Grievance Linkage", report.grievanceLinkage ?: "-")
    }

    private fun createBackendReportContainer(): LinearLayout {
        val linearLayout = LinearLayout(requireContext()).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val title = TextView(requireContext()).apply {
            text = "Backend Generated Report"
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 32; bottomMargin = 12 }
        }
        linearLayout.addView(title)
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            radius = 12f
            cardElevation = 2f
            setContentPadding(16, 16, 16, 16)
        }
        val innerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            id = View.generateViewId()
        }
        card.addView(innerLayout)
        linearLayout.addView(card)

        val parent = binding.root as ViewGroup
        val statusSection = parent.findViewById<View>(resources.getIdentifier("btnSubmitClaim", "id", requireContext().packageName))
        val statusParent = statusSection?.parent as? ViewGroup
        if (statusParent != null) {
            statusParent.addView(linearLayout, statusParent.indexOfChild(statusSection))
        }

        return innerLayout
    }

    private fun addReportRow(container: LinearLayout, label: String, value: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8 }
        }
        val labelTv = TextView(requireContext()).apply {
            text = "$label: "
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 13f
        }
        val valueTv = TextView(requireContext()).apply {
            text = value
            setTextColor(resources.getColor(R.color.black, null))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        row.addView(labelTv)
        row.addView(valueTv)
        container.addView(row)
    }

    private fun setupActions() {
        binding.btnViewPdf.setOnClickListener {
            if (generatedPdfFile != null) {
                openPdf(generatedPdfFile!!)
            } else {
                Toast.makeText(requireContext(), "PDF Report not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnViewVideoProof.setOnClickListener {
            val pkg = viewModel.finalEvidencePackage
            if (pkg != null && pkg.videos.isNotEmpty()) {
                val videosJson = com.google.gson.Gson().toJson(pkg.videos)
                val farmer = viewModel.currentFarmer.value
                val villageGat = "Village: ${farmer?.village} | Gat: ${farmer?.gatNumber}"
                val disasterType = pkg.disasterType.name
                VideoEvidenceViewerActivity.start(requireContext(), videosJson, villageGat, disasterType)
            } else {
                Toast.makeText(requireContext(), "No Video Evidence Available", Toast.LENGTH_SHORT).show()
            }
        }

        val videoCount = viewModel.finalEvidencePackage?.videos?.size ?: 0
        binding.btnViewVideoProof.isEnabled = videoCount > 0
        binding.btnViewVideoProof.alpha = if (videoCount > 0) 1.0f else 0.5f

        binding.btnDownloadPdf.setOnClickListener {
            if (generatedPdfFile != null) {
                Toast.makeText(requireContext(), "PDF Saved to Documents folder.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "PDF Report not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSharePdf.setOnClickListener {
            if (generatedPdfFile != null) {
                sharePdf(generatedPdfFile!!)
            } else {
                Toast.makeText(requireContext(), "PDF Report not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmitClaim.setOnClickListener {
            Toast.makeText(requireContext(), "Survey completed securely.", Toast.LENGTH_LONG).show()
            requireActivity().finish()
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

    private fun sharePdf(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Crop Inspection Report")
            intent.putExtra(Intent.EXTRA_TEXT, "Please find attached the official PMFBY Crop Inspection Report.")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Share Report via"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
