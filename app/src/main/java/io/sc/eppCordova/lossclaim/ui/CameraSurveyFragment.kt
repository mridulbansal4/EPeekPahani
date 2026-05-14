package io.sc.eppCordova.lossclaim.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
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

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSkip.setOnClickListener {
            viewModel.skipCurrentPrompt()
        }

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("mr", "IN")
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
                binding.listeningLayout.visibility = View.GONE
                resetPrimaryButton()
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
                binding.listeningLayout.visibility = View.GONE
                binding.analyzingLayout.visibility = View.VISIBLE
                binding.tvAnalyzingText.text = "Realtime validation..."
                resetPrimaryButton()
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcript = matches[0]
                    currentPromptId?.let { id ->
                        val text = (viewModel.surveyState.value as? SurveyState.Active)?.currentPrompt?.textMarathi ?: "Unknown question"
                        viewModel.processVoiceResponse(transcript, id, text)
                    }
                } else {
                    binding.analyzingLayout.visibility = View.GONE
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
                        binding.analyzingLayout.visibility = View.GONE
                        binding.listeningLayout.visibility = View.GONE
                        binding.btnPrimaryAction.isEnabled = true
                        
                        val prompt = state.currentPrompt
                        binding.tvAiInstruction.text = prompt.textMarathi
                        binding.tvStepProgress.text = prompt.stage.name.replace("_", " ")
                        binding.chipAiStatus.text = "Live AI"
                        binding.chipAiStatus.setTextColor(Color.parseColor("#1976D2"))
                        binding.chipAiStatus.setChipBackgroundColorResource(R.color.surface) 
                        // Assuming surface is fine, just use a light color
                        
                        // Fake progress logic for UI presentation
                        val progress = (Math.random() * 40 + 20).toInt()
                        binding.surveyProgressBar.progress = progress
                        binding.tvQuestionProgress.text = "Survey Question"

                        tts?.speak(prompt.textMarathi, TextToSpeech.QUEUE_FLUSH, null, null)

                        setupInputMode(prompt.type, prompt.id)
                    }
                    is SurveyState.Reviewing -> {
                        binding.analyzingLayout.visibility = View.GONE
                        binding.listeningLayout.visibility = View.GONE
                        binding.btnPrimaryAction.setOnClickListener(null)
                        resetPrimaryButton()
                        
                        val prettyMissing = state.missingEvidence.joinToString(", ") {
                            it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                        }
                        binding.tvAiInstruction.text = "Missing Evidence:\n$prettyMissing\n\nPlease review."
                        binding.tvAiGuidance.text = "Review missing items."
                        binding.btnNextStep.text = "Finish"
                        binding.btnNextStep.setOnClickListener {
                            viewModel.generateEvidencePackage()
                            findNavController().navigate(R.id.action_camera_to_processing)
                        }
                    }
                    is SurveyState.Completed -> {
                        binding.analyzingLayout.visibility = View.GONE
                        binding.listeningLayout.visibility = View.GONE
                        binding.btnPrimaryAction.setOnClickListener(null)
                        binding.tvAiInstruction.text = "Survey completed successfully. Generating package..."
                        viewModel.generateEvidencePackage()
                        findNavController().navigate(R.id.action_camera_to_processing)
                    }
                }
            }
        }
    }

    private fun resetPrimaryButton() {
        binding.btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1976D2"))
    }

    private fun setupInputMode(type: QuestionType, promptId: String) {
        binding.btnPrimaryAction.setOnClickListener(null)
        resetPrimaryButton()
        isRecordingVideo = false
        isRecordingAudio = false
        stopRecordingTimer()
        
        binding.btnNextStep.setOnClickListener { viewModel.skipCurrentPrompt() }

        when (type) {
            QuestionType.CAPTURE_PHOTO -> {
                binding.tvAiGuidance.text = "Please capture a clear photo."
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_menu_camera)
                binding.btnPrimaryAction.setOnClickListener { takePhoto() }
            }
            QuestionType.CAPTURE_VIDEO -> {
                binding.tvAiGuidance.text = "Please record a short video."
                binding.btnPrimaryAction.setImageResource(android.R.drawable.presence_video_online)
                binding.btnPrimaryAction.setOnClickListener {
                    if (!isRecordingVideo) {
                        startVideoRecording()
                        binding.btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        isRecordingVideo = true
                    } else {
                        stopVideoRecording()
                        resetPrimaryButton()
                        isRecordingVideo = false
                    }
                }
            }
            QuestionType.VERBAL_CONFIRM -> {
                binding.tvAiGuidance.text = "Please tap the mic and speak."
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_btn_speak_now)
                binding.btnPrimaryAction.setOnClickListener {
                    if (!isRecordingAudio) {
                        startRecording(promptId)
                        binding.btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        isRecordingAudio = true
                    } else {
                        binding.listeningLayout.visibility = View.GONE
                        binding.analyzingLayout.visibility = View.VISIBLE
                        binding.tvAnalyzingText.text = "Realtime validation..."
                        stopRecording()
                        isRecordingAudio = false
                    }
                }
            }
            QuestionType.INFO -> {
                binding.tvAiGuidance.text = "Please proceed to the next step."
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPrimaryAction.setOnClickListener { viewModel.skipCurrentPrompt() }
            }
            else -> {
                binding.tvAiGuidance.text = "Please proceed to the next step."
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPrimaryAction.setOnClickListener { viewModel.skipCurrentPrompt() }
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
        binding.listeningLayout.visibility = View.VISIBLE
        binding.tvAiGuidance.text = "Listening to your response..."
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
                        binding.tvAiGuidance.text = "Recording video..."
                    }
                    is VideoRecordEvent.Finalize -> {
                        stopRecordingTimer()
                        if (!recordEvent.hasError()) {
                            val durationSecs = ((System.currentTimeMillis() - videoRecordingStart) / 1000).toInt()
                            val lat = viewModel.currentLocation.value?.latitude ?: 0.0
                            val lon = viewModel.currentLocation.value?.longitude ?: 0.0
                            
                            binding.analyzingLayout.visibility = View.VISIBLE
                            binding.tvAnalyzingText.text = "Analyzing video evidence..."
                            binding.tvAiGuidance.text = "Verifying..."
                            binding.chipAiStatus.text = "AI Analyzing"
                            
                            viewModel.processCapturedVideo(videoFile.absolutePath, durationSecs)
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(requireContext(), "Video capture failed", Toast.LENGTH_SHORT).show()
                            binding.btnPrimaryAction.isEnabled = true
                            resetPrimaryButton()
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

        binding.btnPrimaryAction.isEnabled = false

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo failed", Toast.LENGTH_SHORT).show()
                    binding.btnPrimaryAction.isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val lat = viewModel.currentLocation.value?.latitude ?: 0.0
                    val lon = viewModel.currentLocation.value?.longitude ?: 0.0
                    val gat = viewModel.currentFarmer.value?.gatNumber ?: "N/A"
                    val disaster = viewModel.selectedDamageType.value

                    ImageUtils.addGeoWatermark(photoFile, lat, lon, gat, disaster)
                    
                    binding.analyzingLayout.visibility = View.VISIBLE
                    binding.tvAnalyzingText.text = "Analyzing photo..."
                    binding.tvAiGuidance.text = "Verifying..."
                    binding.chipAiStatus.text = "AI Analyzing"
                    
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