package io.sc.eppCordova.lossclaim.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.sc.eppCordova.R
import io.sc.eppCordova.databinding.FragmentCameraSurveyBinding
import io.sc.eppCordova.lossclaim.domain.model.QuestionType
import io.sc.eppCordova.lossclaim.domain.model.SurveyStage
import io.sc.eppCordova.lossclaim.viewmodel.LossClaimViewModel
import io.sc.eppCordova.utils.ImageUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraSurveyFragment : Fragment() {

    private var _binding: FragmentCameraSurveyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LossClaimViewModel by activityViewModels()

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var tts: TextToSpeech? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && 
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startCamera()
            fetchLocation()
        } else {
            Toast.makeText(requireContext(), "Camera and Location permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraSurveyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("mr", "IN") // Marathi by default
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
            fetchLocation()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }

        viewModel.startSurvey()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPrompt.collect { prompt ->
                if (prompt == null) return@collect
                
                binding.tvAiInstruction.text = prompt.textEnglish
                binding.tvStepProgress.text = "Stage: ${prompt.stage.name.replace("_", " ")}"
                
                // Read text out loud
                tts?.speak(prompt.textMarathi, TextToSpeech.QUEUE_FLUSH, null, null)

                if (prompt.stage == SurveyStage.FARMER_CONFIRMATION) {
                    findNavController().navigate(R.id.action_camera_to_processing) // Move to next screen for Q&A
                    return@collect
                }

                when (prompt.type) {
                    QuestionType.CAPTURE_PHOTO -> {
                        binding.btnNextStep.text = "Capture Evidence"
                        binding.btnNextStep.setOnClickListener {
                            takePhoto()
                        }
                    }
                    QuestionType.INFO -> {
                        binding.btnNextStep.text = "Understood"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.advanceFlow()
                        }
                    }
                    QuestionType.YES_NO -> {
                        binding.btnNextStep.text = "Yes / No (Tap to skip mock)"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.advanceFlow()
                        }
                    }
                    QuestionType.OPTIONS -> {
                        binding.btnNextStep.text = "Select Option (Tap to skip mock)"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.advanceFlow()
                        }
                    }
                    else -> {
                        binding.btnNextStep.text = "Next"
                        binding.btnNextStep.setOnClickListener { viewModel.advanceFlow() }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                viewModel.updateLocation(location)
            }
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        @Suppress("DEPRECATION")
        val photoFile = File(requireContext().externalMediaDirs.firstOrNull(), "loss_claim_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.btnNextStep.isEnabled = false
        binding.btnNextStep.text = "Capturing..."

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo failed", Toast.LENGTH_SHORT).show()
                    binding.btnNextStep.isEnabled = true
                    binding.btnNextStep.text = "Retry Capture"
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val lat = viewModel.currentLocation.value?.latitude ?: 0.0
                    val lon = viewModel.currentLocation.value?.longitude ?: 0.0
                    val gat = viewModel.currentFarmer.value?.gatNumber ?: "N/A"
                    val disaster = viewModel.selectedDamageType.value

                    // Add watermark
                    ImageUtils.addGeoWatermark(photoFile, lat, lon, gat, disaster)
                    
                    viewModel.addPhoto(photoFile.absolutePath)
                    
                    // After taking a photo, simulate an observation and advance the flow
                    val simulatedObservation = when (viewModel.selectedDamageType.value.uppercase()) {
                        "FLOOD" -> "waterlogging"
                        "DISEASE" -> "damaged_leaves"
                        "HAILSTORM" -> "damaged_leaves"
                        else -> null
                    }
                    
                    viewModel.advanceFlow(simulatedObservation)
                    
                    binding.btnNextStep.isEnabled = true
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                // Ignore
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        tts?.stop()
        tts?.shutdown()
        _binding = null
    }
}