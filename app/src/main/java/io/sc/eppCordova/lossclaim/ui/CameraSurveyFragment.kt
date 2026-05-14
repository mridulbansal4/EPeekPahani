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
import android.view.WindowManager
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    
    private var recordingTimerJob: kotlinx.coroutines.Job? = null
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

        // Edge-to-edge layout setup
        requireActivity().window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(this, false)
        }
        
        // Fix the white status bar patch caused by parent CoordinatorLayout's fitsSystemWindows
        val navHost = requireActivity().findViewById<View>(R.id.nav_host_fragment)
        (navHost?.parent as? View)?.let { parentView ->
            parentView.fitsSystemWindows = false
            parentView.requestApplyInsets()
            parentView.setBackgroundColor(Color.BLACK)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = v.resources.displayMetrics.density
            
            // Apply margins considering the system bars
            val titleParams = binding.tvScreenTitle.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = insets.top + (48 * density).toInt()
            binding.tvScreenTitle.layoutParams = titleParams
            
            val timerParams = binding.layoutRecordingStatus.layoutParams as ViewGroup.MarginLayoutParams
            timerParams.topMargin = insets.top + (48 * density).toInt()
            binding.layoutRecordingStatus.layoutParams = timerParams
            
            val fabParams = binding.btnPrimaryAction.layoutParams as ViewGroup.MarginLayoutParams
            fabParams.bottomMargin = insets.bottom + (48 * density).toInt() // Reverted button height to allow more breathing room
            binding.btnPrimaryAction.layoutParams = fabParams

            val cardParams = binding.questionCard.layoutParams as ViewGroup.MarginLayoutParams
            cardParams.bottomMargin = (24 * density).toInt() // Pushed card down relative to button
            binding.questionCard.layoutParams = cardParams

            windowInsets
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

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
                resetPrimaryButton()
                
                // Ignore ERROR_CLIENT and ERROR_NO_MATCH for a cleaner UX if they happen rapidly
                if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                    binding.tvAiInstruction.text = "Please tap mic to speak again."
                    return
                }

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Error from server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Didn't understand, please try again."
                }
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                binding.tvAiInstruction.text = "Voice unrecognised, please tap mic again."
            }

            override fun onResults(results: Bundle?) {
                isRecordingAudio = false
                resetPrimaryButton()
                binding.tvAiInstruction.text = "Processing response..."
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val transcript = matches[0]
                    currentPromptId?.let { id ->
                        val text = (viewModel.surveyState.value as? SurveyState.Active)?.currentPrompt?.textMarathi ?: "Unknown question"
                        viewModel.processVoiceResponse(transcript, id, text)
                    }
                } else {
                    binding.tvAiInstruction.text = "No response recorded, please try again."
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

        viewModel.startSurvey()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.surveyState.collect { state ->
                when (state) {
                    is SurveyState.Idle -> { }
                    is SurveyState.Active -> {
                        binding.btnPrimaryAction.isEnabled = true
                        
                        val prompt = state.currentPrompt
                        binding.tvAiInstruction.text = prompt.textMarathi

                        tts?.speak(prompt.textMarathi, TextToSpeech.QUEUE_FLUSH, null, null)

                        setupInputMode(prompt.type, prompt.id)
                    }
                    is SurveyState.Reviewing -> {
                        resetPrimaryButton()
                        
                        val prettyMissing = state.missingEvidence.joinToString(", ") {
                            it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                        }
                        binding.tvAiInstruction.text = "Missing Evidence:\n$prettyMissing\n\nPlease review."
                        
                        binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_media_play)
                        binding.btnPrimaryAction.setOnClickListener {
                            viewModel.generateEvidencePackage()
                            findNavController().navigate(R.id.action_camera_to_processing)
                        }
                    }
                    is SurveyState.Completed -> {
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
        binding.btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1D6F42")) // Theme Green
    }

    private fun setupInputMode(type: QuestionType, promptId: String) {
        binding.btnPrimaryAction.setOnClickListener(null)
        resetPrimaryButton()
        isRecordingVideo = false
        isRecordingAudio = false
        
        when (type) {
            QuestionType.CAPTURE_PHOTO -> {
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_menu_camera)
                binding.btnPrimaryAction.setOnClickListener { takePhoto() }
            }
            QuestionType.CAPTURE_VIDEO -> {
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
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_btn_speak_now)
                binding.btnPrimaryAction.setOnClickListener {
                    if (!isRecordingAudio) {
                        startRecording(promptId)
                        binding.btnPrimaryAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
                        isRecordingAudio = true
                    } else {
                        binding.tvAiInstruction.text = "Processing response..."
                        stopRecording()
                        isRecordingAudio = false
                    }
                }
            }
            QuestionType.INFO -> {
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPrimaryAction.setOnClickListener { viewModel.skipCurrentPrompt() }
            }
            else -> {
                binding.btnPrimaryAction.setImageResource(android.R.drawable.ic_media_play)
                binding.btnPrimaryAction.setOnClickListener { viewModel.skipCurrentPrompt() }
            }
        }
    }

    private fun startRecording(promptId: String) {
        currentPromptId = promptId
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "mr-IN")
        speechRecognizer?.startListening(intent)
        binding.tvAiInstruction.text = "Listening..."
    }

    private fun stopRecording() {
        speechRecognizer?.stopListening()
    }

    private fun startRecordingTimer() {
        binding.layoutRecordingStatus.visibility = View.VISIBLE
        secondsRecorded = 0
        updateTimerText()
        recordingTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while(true) {
                kotlinx.coroutines.delay(1000)
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
                        binding.tvAiInstruction.text = "Recording video..."
                    }
                    is VideoRecordEvent.Finalize -> {
                        stopRecordingTimer()
                        if (!recordEvent.hasError()) {
                            val durationSecs = ((System.currentTimeMillis() - videoRecordingStart) / 1000).toInt()
                            
                            binding.tvAiInstruction.text = "Analyzing video evidence..."
                            viewModel.processCapturedVideo(videoFile.absolutePath, durationSecs)
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(requireContext(), "Video capture failed", Toast.LENGTH_SHORT).show()
                            binding.btnPrimaryAction.isEnabled = true
                            resetPrimaryButton()
                            isRecordingVideo = false
                            
                            val prompt = (viewModel.surveyState.value as? SurveyState.Active)?.currentPrompt
                            if (prompt != null) {
                                binding.tvAiInstruction.text = prompt.textMarathi
                            }
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
                    
                    binding.tvAiInstruction.text = "Analyzing photo..."
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
        
        // Restore window settings
        requireActivity().window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, true)
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.surface)
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.surface)
        }
        
        // Restore parent view's fitsSystemWindows
        val navHost = requireActivity().findViewById<View>(R.id.nav_host_fragment)
        (navHost?.parent as? View)?.let { parentView ->
            parentView.fitsSystemWindows = true
            parentView.requestApplyInsets()
            parentView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
        }
        
        cameraExecutor.shutdown()
        tts?.stop()
        tts?.shutdown()
        _binding = null
    }
}
