package io.sc.eppCordova.lossclaim.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sc.eppCordova.data.remote.dto.ClaimRequest
import io.sc.eppCordova.data.remote.dto.ReportDto
import io.sc.eppCordova.data.repository.ApiResult
import io.sc.eppCordova.data.repository.ClaimsRepository
import io.sc.eppCordova.data.repository.ReportRepository
import io.sc.eppCordova.lossclaim.data.FarmerEntity
import io.sc.eppCordova.lossclaim.data.LossClaimEntity
import io.sc.eppCordova.lossclaim.data.LossClaimRepository
import io.sc.eppCordova.lossclaim.domain.engine.*
import io.sc.eppCordova.lossclaim.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LossClaimViewModel @Inject constructor(
    private val repository: LossClaimRepository,
    private val surveyStateMachine: SurveyStateMachine,
    private val visionAnalysisEngine: VisionAnalysisEngine,
    private val voiceProcessingEngine: VoiceProcessingEngine,
    private val evidencePackageBuilder: EvidencePackageBuilder,
    private val claimsRepository: ClaimsRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _currentFarmer = MutableStateFlow<FarmerEntity?>(null)
    val currentFarmer: StateFlow<FarmerEntity?> = _currentFarmer.asStateFlow()

    private val _selectedDamageType = MutableStateFlow<String>("")
    val selectedDamageType: StateFlow<String> = _selectedDamageType.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _capturedPhotos = MutableStateFlow<List<EvidencePhoto>>(emptyList())
    val capturedPhotos: StateFlow<List<EvidencePhoto>> = _capturedPhotos.asStateFlow()
    
    private val _capturedVideos = MutableStateFlow<List<EvidenceVideo>>(emptyList())
    val capturedVideos: StateFlow<List<EvidenceVideo>> = _capturedVideos.asStateFlow()
    
    private val _voiceInteractions = MutableStateFlow<List<VoiceInteraction>>(emptyList())
    val voiceInteractions: StateFlow<List<VoiceInteraction>> = _voiceInteractions.asStateFlow()

    private val _surveyStartTime = MutableStateFlow<Long>(0)
    
    // Expose Survey State Machine's state directly
    val surveyState: StateFlow<SurveyState> = surveyStateMachine.state

    private var currentDisasterTypeEnum: DisasterType = DisasterType.UNKNOWN
    var finalEvidencePackage: EvidencePackage? = null

    private val _generatedReport = MutableStateFlow<ReportDto?>(null)
    val generatedReport: StateFlow<ReportDto?> = _generatedReport.asStateFlow()

    private val _backendSubmitState = MutableStateFlow<BackendSubmitState>(BackendSubmitState.IDLE)
    val backendSubmitState: StateFlow<BackendSubmitState> = _backendSubmitState.asStateFlow()

    sealed class BackendSubmitState {
        data object IDLE : BackendSubmitState()
        data object SUBMITTING : BackendSubmitState()
        data object FETCHING_REPORT : BackendSubmitState()
        data object SUCCESS : BackendSubmitState()
        data class ERROR(val message: String) : BackendSubmitState()
    }

    fun loadFarmerData(mobileNumber: String) {
        viewModelScope.launch {
            _currentFarmer.value = repository.getFarmerByMobile(mobileNumber)
        }
    }

    fun setDamageType(type: String) {
        _selectedDamageType.value = type
        currentDisasterTypeEnum = when (type.uppercase().replace(" ", "_")) {
            "FLOOD" -> DisasterType.FLOOD
            "HAILSTORM" -> DisasterType.HAILSTORM
            "DROUGHT" -> DisasterType.DROUGHT
            "PEST", "PEST_ATTACK" -> DisasterType.PEST_ATTACK
            "DISEASE", "CROP_DISEASE" -> DisasterType.DISEASE
            "CYCLONE" -> DisasterType.CYCLONE
            "EXCESS_RAINFALL" -> DisasterType.EXCESS_RAINFALL
            "FIRE", "FIRE_DAMAGE" -> DisasterType.FIRE_DAMAGE
            "ANIMAL", "ANIMAL_DAMAGE" -> DisasterType.ANIMAL_DAMAGE
            "UNSEASONAL_RAIN" -> DisasterType.UNSEASONAL_RAIN
            "WIND", "WIND_DAMAGE" -> DisasterType.WIND_DAMAGE
            "OTHER" -> DisasterType.OTHER
            else -> DisasterType.UNKNOWN
        }
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun startSurvey() {
        _capturedPhotos.value = emptyList()
        _capturedVideos.value = emptyList()
        _voiceInteractions.value = emptyList()
        _surveyStartTime.value = System.currentTimeMillis()
        surveyStateMachine.startSurvey(currentDisasterTypeEnum)
    }

    fun processCapturedPhoto(imagePath: String) {
        viewModelScope.launch {
            val file = File(imagePath)
            if (!file.exists()) return@launch

            val analysisResult = visionAnalysisEngine.analyzeFrame(file, _selectedDamageType.value)
            
            val photo = EvidencePhoto(
                imagePath = imagePath,
                latitude = _currentLocation.value?.latitude ?: 0.0,
                longitude = _currentLocation.value?.longitude ?: 0.0,
                timestamp = System.currentTimeMillis(),
                disasterType = _selectedDamageType.value,
                gatNumber = _currentFarmer.value?.gatNumber ?: "Unknown",
                observations = analysisResult.observations.map { it.type },
                isClear = analysisResult.isClear
            )
            
            _capturedPhotos.value = _capturedPhotos.value + photo
            
            // Feed the observation back into the state machine
            val primaryObservation = analysisResult.observations.firstOrNull()
            surveyStateMachine.processInput(
                observation = primaryObservation,
                photoCount = _capturedPhotos.value.size,
                videoCount = _capturedVideos.value.size
            )
        }
    }

    fun processCapturedVideo(videoPath: String, durationSecs: Int) {
        viewModelScope.launch {
            val file = File(videoPath)
            if (!file.exists()) return@launch

            // Simulate frame extraction and AI analysis on keyframes
            // For now, generate a mock observation based on disaster type
            val photoSim = EvidencePhoto(
                imagePath = videoPath, // Pretending the video path is an image path for the mock engine
                latitude = _currentLocation.value?.latitude ?: 0.0,
                longitude = _currentLocation.value?.longitude ?: 0.0,
                timestamp = System.currentTimeMillis(),
                disasterType = _selectedDamageType.value,
                gatNumber = _currentFarmer.value?.gatNumber ?: "Unknown"
            )
            
            // Note: Since VisionAnalysisEngine currently operates on image files, we simulate an observation.
            // In a real app we'd extract a frame using MediaMetadataRetriever and pass it.
            val mockObservations = listOf(AiObservation(
                type = when(currentDisasterTypeEnum) {
                    DisasterType.FLOOD -> "waterlogging"
                    DisasterType.PEST_ATTACK -> "pest_visible"
                    DisasterType.CYCLONE -> "lodging"
                    DisasterType.DROUGHT -> "dry_soil"
                    else -> "damaged_crop"
                },
                confidence = 0.88f,
                timestamp = System.currentTimeMillis(),
                sourceImage = videoPath
            ))

            val video = EvidenceVideo(
                videoPath = videoPath,
                durationSeconds = durationSecs,
                latitude = _currentLocation.value?.latitude ?: 0.0,
                longitude = _currentLocation.value?.longitude ?: 0.0,
                timestamp = System.currentTimeMillis(),
                keyframes = listOf("frame_1.jpg", "frame_2.jpg", "frame_3.jpg"), // mock keyframes
                observations = mockObservations.map { it.type },
                gpsVerified = true
            )
            
            _capturedVideos.value = _capturedVideos.value + video
            
            val primaryObservation = mockObservations.firstOrNull()
            surveyStateMachine.processInput(
                observation = primaryObservation,
                photoCount = _capturedPhotos.value.size,
                videoCount = _capturedVideos.value.size
            )
        }
    }

    fun processVoiceResponse(transcript: String, promptId: String, promptText: String) {
        viewModelScope.launch {
            val voiceResult = voiceProcessingEngine.processTranscript(transcript)
            
            val interaction = VoiceInteraction(
                promptId = promptId,
                promptText = promptText,
                rawAudioPath = "", // No raw audio when using SpeechRecognizer transcript directly
                transcript = transcript,
                extractedSemantics = voiceResult.extractedSemantics,
                confidenceScore = voiceResult.confidence
            )
            
            _voiceInteractions.value = _voiceInteractions.value + interaction
            
            // Feed farmer's semantic answers to the state machine
            surveyStateMachine.processInput(
                farmerAnswer = voiceResult.extractedSemantics,
                photoCount = _capturedPhotos.value.size,
                videoCount = _capturedVideos.value.size
            )
        }
    }

    fun skipCurrentPrompt() {
        // Manually push flow forward if skipped
        surveyStateMachine.processInput(
            photoCount = _capturedPhotos.value.size,
            videoCount = _capturedVideos.value.size
        )
    }

    fun generateEvidencePackage(isMockLocationUsed: Boolean = false) {
        if (_capturedPhotos.value.size < 2 || _capturedVideos.value.isEmpty()) {
            // Strict Validation - Need at least 2 photos and 1 video
            return
        }
        val durationSecs = (System.currentTimeMillis() - _surveyStartTime.value) / 1000
        val allObservations = _capturedPhotos.value.flatMap { photo -> 
            photo.observations.map { AiObservation(it, 1.0f, photo.timestamp, photo.imagePath) }
        }

        finalEvidencePackage = evidencePackageBuilder.buildPackage(
            farmerId = _currentFarmer.value?.mobileNumber ?: "unknown",
            gatNumber = _currentFarmer.value?.gatNumber ?: "unknown",
            disasterType = currentDisasterTypeEnum,
            photos = _capturedPhotos.value,
            videos = _capturedVideos.value,
            voiceInteractions = _voiceInteractions.value,
            observations = allObservations,
            surveyDurationSeconds = durationSecs,
            isMockLocationUsed = isMockLocationUsed
        )
    }

    fun submitClaim() {
        viewModelScope.launch {
            val pkg = finalEvidencePackage ?: return@launch
            if (pkg.photos.size < 2 || pkg.videos.isEmpty()) return@launch
            
            val farmer = _currentFarmer.value ?: return@launch

            val claim = LossClaimEntity(
                mobileNumber = farmer.mobileNumber,
                gatNumber = farmer.gatNumber,
                crop = farmer.primaryCrop ?: "Unknown",
                damageType = _selectedDamageType.value,
                damagePercentage = pkg.estimatedDamagePercentage,
                estimatedCompensation = (pkg.estimatedDamagePercentage * 100).toDouble(), // mock logic
                imagePath1 = pkg.photos[0].imagePath,
                imagePath2 = pkg.photos[1].imagePath,
                videoPath = pkg.videos[0].videoPath,
                latitude = pkg.photos[0].latitude,
                longitude = pkg.photos[0].longitude
            )
            repository.saveLossClaim(claim)

            _backendSubmitState.value = BackendSubmitState.SUBMITTING
            val request = ClaimRequest(
                farmerId = farmer.mobileNumber,
                cropType = farmer.primaryCrop ?: "Unknown",
                claimType = _selectedDamageType.value,
                village = farmer.village,
                latitude = pkg.photos[0].latitude,
                longitude = pkg.photos[0].longitude,
                incidentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                affectedAreaHa = farmer.area?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 1.0,
                gatNumber = farmer.gatNumber,
                description = "Loss claim due to ${_selectedDamageType.value}"
            )

            when (val result = claimsRepository.submitClaim(request)) {
                is ApiResult.Success -> {
                    val claimId = result.data.claimId
                    if (claimId != null) {
                        _backendSubmitState.value = BackendSubmitState.FETCHING_REPORT
                        when (val reportResult = reportRepository.getReportById(claimId)) {
                            is ApiResult.Success -> {
                                _generatedReport.value = reportResult.data
                                _backendSubmitState.value = BackendSubmitState.SUCCESS
                            }
                            is ApiResult.Error -> {
                                _backendSubmitState.value = BackendSubmitState.SUCCESS
                            }
                        }
                    } else {
                        _backendSubmitState.value = BackendSubmitState.SUCCESS
                    }
                }
                is ApiResult.Error -> {
                    _backendSubmitState.value = BackendSubmitState.ERROR(result.message)
                }
            }
        }
    }
}
