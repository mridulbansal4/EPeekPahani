package io.sc.eppCordova.lossclaim.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.res.ColorStateList
import android.graphics.Color

class CameraSurveyFragment : Fragment() {

    private var _binding: FragmentCameraSurveyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LossClaimViewModel by activityViewModels()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var videoRecordingStart: Long = 0
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    
    private var isRecordingAudio = false
    private var isRecordingVideo = false
    private var currentPromptId: String? = null
    private var recordingTimerJob: Job? = null
    private var secondsRecorded = 0

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
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isRecordingAudio = false
                stopRecordingTimer()
                binding.btnNextStep.visibility = View.VISIBLE
                binding.progressBarAi.visibility = View.GONE
                binding.btnNextStep.text = "Start Audio"
                binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Error from server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Didn't understand, please try again."
                }
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                isRecordingAudio = false
                stopRecordingTimer()
                binding.btnNextStep.text = "Start Audio"
                binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcript = matches[0]
                    currentPromptId?.let { id ->
                        val text = (viewModel.surveyState.value as? SurveyState.Active)?.currentPrompt?.textMarathi ?: "Unknown question"
                        viewModel.processVoiceResponse(transcript, id, text)
                    }
                } else {
                    // Reset UI if no match
                    binding.btnNextStep.visibility = View.VISIBLE
                    binding.progressBarAi.visibility = View.GONE
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFarmer.collect { farmer ->
                binding.tvFarmerDetails.text = "Farmer: ${farmer?.farmerName ?: "Unknown"}"
            }
        }

        viewModel.startSurvey()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.surveyState.collect { state ->
                when (state) {
                    is SurveyState.Idle -> { }
                    is SurveyState.Active -> {
                        // Hide loader, show button
                        binding.progressBarAi.visibility = View.GONE
                        binding.btnNextStep.visibility = View.VISIBLE
                        binding.btnNextStep.isEnabled = true
                        
                        val prompt = state.currentPrompt
                        binding.tvAiInstruction.text = prompt.textMarathi
                        binding.tvStepProgress.text = "Stage: ${prompt.stage.name.replace("_", " ")}"
                        binding.tvAiStatusChip.text = "AI Ready"
                        
                        tts?.speak(prompt.textMarathi, TextToSpeech.QUEUE_FLUSH, null, null)

                        setupInputMode(prompt.type, prompt.id)
                    }
                    is SurveyState.Reviewing -> {
                        // Force UI reset immediately
                        binding.progressBarAi.visibility = View.GONE
                        binding.btnNextStep.visibility = View.VISIBLE
                        binding.btnNextStep.setOnTouchListener(null)
                        binding.btnNextStep.setOnClickListener(null)
                        binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
                        
                        val prettyMissing = state.missingEvidence.joinToString(", ") {
                            it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                        }
                        binding.tvAiInstruction.text = "Missing Evidence:\n$prettyMissing\n\nPlease review."
                        binding.btnNextStep.text = "Finish Anyway"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.generateEvidencePackage()
                            findNavController().navigate(R.id.action_camera_to_processing)
                        }
                    }
                    is SurveyState.Completed -> {
                        binding.progressBarAi.visibility = View.GONE
                        binding.btnNextStep.visibility = View.VISIBLE
                        binding.btnNextStep.setOnTouchListener(null)
                        binding.btnNextStep.setOnClickListener(null)
                        binding.tvAiInstruction.text = "Survey completed successfully. Generating package..."
                        viewModel.generateEvidencePackage()
                        findNavController().navigate(R.id.action_camera_to_processing)
                    }
                }
            }
        }
    }

    private fun setupInputMode(type: QuestionType, promptId: String) {
        // Clear previous listeners to prevent overlap
        binding.btnNextStep.setOnClickListener(null)
        binding.btnNextStep.setOnTouchListener(null)
        binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
        isRecordingVideo = false
        isRecordingAudio = false
        stopRecordingTimer()
        
        when (type) {
            QuestionType.CAPTURE_PHOTO -> {
                binding.btnNextStep.text = "Capture Photo"
                binding.btnNextStep.setOnClickListener { takePhoto() }
            }
            QuestionType.CAPTURE_VIDEO -> {
                binding.btnNextStep.text = "Start Video"
                binding.btnNextStep.setOnClickListener {
                    if (!isRecordingVideo) {
                        startVideoRecording()
                        binding.btnNextStep.text = "Stop Recording"
                        binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        isRecordingVideo = true
                    } else {
                        stopVideoRecording()
                        binding.btnNextStep.text = "Start Video"
                        binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
                        isRecordingVideo = false
                    }
                }
            }
            QuestionType.VERBAL_CONFIRM -> {
                binding.btnNextStep.text = "Start Audio"
                binding.btnNextStep.setOnClickListener {
                    if (!isRecordingAudio) {
                        startRecording(promptId)
                        binding.btnNextStep.text = "Stop Audio"
                        binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        isRecordingAudio = true
                    } else {
                        binding.btnNextStep.visibility = View.INVISIBLE
                        binding.progressBarAi.visibility = View.VISIBLE
                        stopRecording()
                        isRecordingAudio = false
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

    private fun startRecordingTimer() {
        binding.layoutRecordingStatus.visibility = View.VISIBLE
        secondsRecorded = 0
        updateTimerText()
        recordingTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while(true) {
                delay(1000)
                secondsRecorded++
                updateTimerText()
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        binding.layoutRecordingStatus.visibility = View.GONE
    }

    private fun updateTimerText() {
        val mins = secondsRecorded / 60
        val secs = secondsRecorded % 60
        binding.tvRecordingTimer.text = String.format("%02d:%02d", mins, secs)
    }

    private fun startRecording(promptId: String) {
        currentPromptId = promptId
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
        speechRecognizer?.startListening(intent)
        startRecordingTimer()
        Toast.makeText(requireContext(), "Audio Recording Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        speechRecognizer?.stopListening()
        stopRecordingTimer()
    }

    @SuppressLint("MissingPermission")
    private fun startVideoRecording() {
        val videoCapture = this.videoCapture ?: return
        val videoFile = File(requireContext().externalMediaDirs.firstOrNull(), "evidence_video_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        videoRecordingStart = System.currentTimeMillis()
        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .start(ContextCompat.getMainExecutor(requireContext())) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
                        startRecordingTimer()
                        Toast.makeText(requireContext(), "Video Recording Started", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        stopRecordingTimer()
                        if (!recordEvent.hasError()) {
                            val durationSecs = ((System.currentTimeMillis() - videoRecordingStart) / 1000).toInt()
                            val lat = viewModel.currentLocation.value?.latitude ?: 0.0
                            val lon = viewModel.currentLocation.value?.longitude ?: 0.0
                            
                            Toast.makeText(requireContext(), "Video saved. AI is analyzing...", Toast.LENGTH_SHORT).show()
                            binding.btnNextStep.visibility = View.INVISIBLE
                            binding.progressBarAi.visibility = View.VISIBLE
                            binding.tvAiStatusChip.text = "AI Analyzing"
                            
                            viewModel.processCapturedVideo(videoFile.absolutePath, durationSecs)
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(requireContext(), "Video capture failed", Toast.LENGTH_SHORT).show()
                            binding.btnNextStep.isEnabled = true
                            binding.btnNextStep.text = "Start Video"
                            binding.btnNextStep.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gov_green))
                            isRecordingVideo = false
                        }
                    }
                }
            }
    }

    private fun stopVideoRecording() {
        recording?.stop()
        recording = null
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
                    
                    // UI Acknowledgement
                    Toast.makeText(requireContext(), "Photo saved. AI is analyzing...", Toast.LENGTH_SHORT).show()
                    binding.btnNextStep.visibility = View.INVISIBLE
                    binding.progressBarAi.visibility = View.VISIBLE
                    binding.tvAiStatusChip.text = "AI Analyzing"
                    
                    viewModel.processCapturedPhoto(photoFile.absolutePath)
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
            
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageCapture, videoCapture)
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