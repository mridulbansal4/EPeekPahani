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
import io.sc.eppCordova.lossclaim.domain.engine.SurveyState
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
    
    // Simulate audio recording
    private var isRecording = false
    private var currentAudioFile: File? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && 
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
            permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startCamera()
            fetchLocation()
        } else {
            Toast.makeText(requireContext(), "Camera, Location and Mic permissions required", Toast.LENGTH_SHORT).show()
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
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            ))
        }

        viewModel.startSurvey()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.surveyState.collect { state ->
                when (state) {
                    is SurveyState.Idle -> { }
                    is SurveyState.Active -> {
                        val prompt = state.currentPrompt
                        binding.tvAiInstruction.text = prompt.textMarathi
                        binding.tvStepProgress.text = "Stage: ${prompt.stage.name.replace("_", " ")}"
                        
                        tts?.speak(prompt.textMarathi, TextToSpeech.QUEUE_FLUSH, null, null)

                        setupInputMode(prompt.type, prompt.id)
                    }
                    is SurveyState.Reviewing -> {
                        binding.tvAiInstruction.text = "Some evidence is missing: ${state.missingEvidence.joinToString()}. Please review."
                        binding.btnNextStep.text = "Finish Anyway"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.generateEvidencePackage()
                            findNavController().navigate(R.id.action_camera_to_processing)
                        }
                    }
                    is SurveyState.Completed -> {
                        binding.tvAiInstruction.text = "Survey completed successfully. Generating package..."
                        viewModel.generateEvidencePackage()
                        findNavController().navigate(R.id.action_camera_to_processing)
                    }
                }
            }
        }
    }

    private fun setupInputMode(type: QuestionType, promptId: String) {
        when (type) {
            QuestionType.CAPTURE_PHOTO -> {
                binding.btnNextStep.text = "Capture Photo"
                binding.btnNextStep.setOnClickListener { takePhoto() }
            }
            QuestionType.VERBAL_CONFIRM -> {
                binding.btnNextStep.text = "Hold to Speak"
                binding.btnNextStep.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startRecording()
                            binding.btnNextStep.text = "Listening..."
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            stopRecording(promptId)
                            binding.btnNextStep.text = "Processing Voice..."
                            true
                        }
                        else -> false
                    }
                }
            }
            QuestionType.INFO -> {
                binding.btnNextStep.text = "Next"
                binding.btnNextStep.setOnClickListener { viewModel.skipCurrentPrompt() }
            }
            else -> {
                binding.btnNextStep.text = "Skip"
                binding.btnNextStep.setOnClickListener { viewModel.skipCurrentPrompt() }
            }
        }
    }

    private fun startRecording() {
        isRecording = true
        currentAudioFile = File(requireContext().cacheDir, "audio_${System.currentTimeMillis()}.wav")
        // Normally start MediaRecorder here
    }

    private fun stopRecording(promptId: String) {
        isRecording = false
        // Normally stop MediaRecorder here
        currentAudioFile?.let {
            viewModel.processAudioResponse(it, promptId)
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
        val photoFile = File(requireContext().externalMediaDirs.firstOrNull(), "evidence_${System.currentTimeMillis()}.jpg")
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

                    ImageUtils.addGeoWatermark(photoFile, lat, lon, gat, disaster)
                    viewModel.processCapturedPhoto(photoFile.absolutePath)
                    
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
    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        tts?.stop()
        tts?.shutdown()
        _binding = null
    }
}
