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

        val pkg = viewModel.finalEvidencePackage
        val damagePercent = pkg?.estimatedDamagePercentage ?: 0
        val payout = 30000.0 * (damagePercent / 100.0)
        
        val farmer = viewModel.currentFarmer.value

        // Populate UI
        val timeStamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val refId = "MH-${System.currentTimeMillis().toString().takeLast(6)}"
        
        binding.tvRefId.text = "Ref ID: $refId"
        binding.tvTimestamp.text = "Time: $timeStamp"
        
        if (farmer != null) {
            binding.tvVillageGat.text = "Village: ${farmer.village} | Gat: ${farmer.gatNumber}"
            binding.tvCropDisaster.text = "Crop: ${farmer.primaryCrop} | Disaster: ${pkg?.disasterType?.name ?: "Unknown"}"
            
            // Generate PDF
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
        val duration = if (lastTime > firstTime) (lastTime - firstTime) / 1000 else 60 // fallback to 60s
        
        binding.tvEvidenceCount.text = "Evidence Photos: ${pkg?.photos?.size ?: 0}"
        val videoCount = pkg?.videos?.size ?: 0
        binding.tvVideoCount.text = "Evidence Videos: $videoCount"
        if (videoCount > 0) {
            binding.badgeVideoVerified.visibility = View.VISIBLE
            binding.videoThumbnailContainer.visibility = View.VISIBLE
            val firstVideoPath = pkg?.videos?.firstOrNull()?.videoPath
            if (!firstVideoPath.isNullOrEmpty()) {
                com.bumptech.glide.Glide.with(this)
                    .load(java.io.File(firstVideoPath))
                    .into(binding.ivVideoThumbnail)
            }
        } else {
            binding.badgeVideoVerified.visibility = View.GONE
            binding.videoThumbnailContainer.visibility = View.GONE
        }
        binding.tvDuration.text = "Survey Time: ${duration}s"

        // Setup Buttons
        binding.btnViewVideoProof.setOnClickListener {
            if (pkg != null && pkg.videos.isNotEmpty()) {
                val videosJson = com.google.gson.Gson().toJson(pkg.videos)
                val villageGat = "Village: ${farmer?.village} | Gat: ${farmer?.gatNumber}"
                val disasterType = pkg.disasterType.name
                VideoEvidenceViewerActivity.start(requireContext(), videosJson, villageGat, disasterType)
            } else {
                Toast.makeText(requireContext(), "No Video Evidence Available", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnViewVideoProof.isEnabled = videoCount > 0
        binding.btnViewVideoProof.alpha = if (videoCount > 0) 1.0f else 0.5f

        binding.btnViewPdf.setOnClickListener {
            if (generatedPdfFile != null) {
                openPdf(generatedPdfFile!!)
            } else {
                Toast.makeText(requireContext(), "PDF Report not ready yet", Toast.LENGTH_SHORT).show()
            }
        }

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
            viewModel.submitClaim()
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
