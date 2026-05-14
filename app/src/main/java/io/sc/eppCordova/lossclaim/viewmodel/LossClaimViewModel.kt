package io.sc.eppCordova.lossclaim.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val evidencePackageBuilder: EvidencePackageBuilder
) : ViewModel() {

    private val _currentFarmer = MutableStateFlow<FarmerEntity?>(null)
    val currentFarmer: StateFlow<FarmerEntity?> = _currentFarmer.asStateFlow()

    private val _selectedDamageType = MutableStateFlow<String>("")
    val selectedDamageType: StateFlow<String> = _selectedDamageType.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _capturedPhotos = MutableStateFlow<List<EvidencePhoto>>(emptyList())
    val capturedPhotos: StateFlow<List<EvidencePhoto>> = _capturedPhotos.asStateFlow()
    
    private val _voiceInteractions = MutableStateFlow<List<VoiceInteraction>>(emptyList())
    val voiceInteractions: StateFlow<List<VoiceInteraction>> = _voiceInteractions.asStateFlow()

    private val _surveyStartTime = MutableStateFlow<Long>(0)
    
    // Expose Survey State Machine's state directly
    val surveyState: StateFlow<SurveyState> = surveyStateMachine.state

    private var currentDisasterTypeEnum: DisasterType = DisasterType.UNKNOWN
    var finalEvidencePackage: EvidencePackage? = null

    fun loadFarmerData(mobileNumber: String) {
        viewModelScope.launch {
            _currentFarmer.value = repository.getFarmerByMobile(mobileNumber)
        }
    }

    fun setDamageType(type: String) {
        _selectedDamageType.value = type
        currentDisasterTypeEnum = when (type.uppercase()) {
            "FLOOD" -> DisasterType.FLOOD
            "HAILSTORM" -> DisasterType.HAILSTORM
            "DROUGHT" -> DisasterType.DROUGHT
            "PEST" -> DisasterType.PEST_ATTACK
            "DISEASE" -> DisasterType.DISEASE
            "CYCLONE" -> DisasterType.CYCLONE
            else -> DisasterType.UNKNOWN
        }
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = location
    }

    fun startSurvey() {
        _capturedPhotos.value = emptyList()
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
            surveyStateMachine.processInput(observation = primaryObservation)
        }
    }

    fun processAudioResponse(audioFile: File, promptId: String) {
        viewModelScope.launch {
            val voiceResult = voiceProcessingEngine.processAudio(audioFile)
            
            val interaction = VoiceInteraction(
                promptId = promptId,
                rawAudioPath = audioFile.absolutePath,
                transcript = voiceResult.transcript,
                extractedSemantics = voiceResult.extractedSemantics,
                confidenceScore = voiceResult.confidence
            )
            
            _voiceInteractions.value = _voiceInteractions.value + interaction
            
            // Feed farmer's semantic answers to the state machine
            surveyStateMachine.processInput(farmerAnswer = voiceResult.extractedSemantics)
        }
    }

    fun skipCurrentPrompt() {
        // Manually push flow forward if skipped
        surveyStateMachine.processInput()
    }

    fun generateEvidencePackage(isMockLocationUsed: Boolean = false) {
        val durationSecs = (System.currentTimeMillis() - _surveyStartTime.value) / 1000
        val allObservations = _capturedPhotos.value.flatMap { photo -> 
            photo.observations.map { AiObservation(it, 1.0f, photo.timestamp, photo.imagePath) }
        }

        finalEvidencePackage = evidencePackageBuilder.buildPackage(
            farmerId = _currentFarmer.value?.mobileNumber ?: "unknown",
            gatNumber = _currentFarmer.value?.gatNumber ?: "unknown",
            disasterType = currentDisasterTypeEnum,
            photos = _capturedPhotos.value,
            voiceInteractions = _voiceInteractions.value,
            observations = allObservations,
            surveyDurationSeconds = durationSecs,
            isMockLocationUsed = isMockLocationUsed
        )
    }

    fun submitClaim() {
        viewModelScope.launch {
            val pkg = finalEvidencePackage ?: return@launch
            val farmer = _currentFarmer.value ?: return@launch
            
            val claim = LossClaimEntity(
                mobileNumber = farmer.mobileNumber,
                gatNumber = farmer.gatNumber,
                crop = farmer.primaryCrop ?: "Unknown",
                damageType = _selectedDamageType.value,
                damagePercentage = pkg.estimatedDamagePercentage,
                estimatedCompensation = (pkg.estimatedDamagePercentage * 100).toDouble(), // mock logic
                imagePath = pkg.photos.firstOrNull()?.imagePath ?: ""
            )
            repository.saveLossClaim(claim)
        }
    }
}
