package io.sc.eppCordova.lossclaim.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.sc.eppCordova.lossclaim.data.AppDatabase
import io.sc.eppCordova.lossclaim.data.FarmerEntity
import io.sc.eppCordova.lossclaim.data.LossClaimEntity
import io.sc.eppCordova.lossclaim.data.LossClaimRepository
import io.sc.eppCordova.lossclaim.domain.ConditionalFlowEngine
import io.sc.eppCordova.lossclaim.domain.model.AiPrompt
import io.sc.eppCordova.lossclaim.domain.model.DisasterType
import io.sc.eppCordova.lossclaim.domain.model.EvidencePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LossClaimViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LossClaimRepository
    private val flowEngine = ConditionalFlowEngine()

    private val _currentFarmer = MutableStateFlow<FarmerEntity?>(null)
    val currentFarmer: StateFlow<FarmerEntity?> = _currentFarmer

    private val _selectedDamageType = MutableStateFlow<String>("")
    val selectedDamageType: StateFlow<String> = _selectedDamageType

    // AI Survey Flow State
    private val _currentPrompt = MutableStateFlow<AiPrompt?>(null)
    val currentPrompt: StateFlow<AiPrompt?> = _currentPrompt

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _capturedPhotos = MutableStateFlow<List<EvidencePhoto>>(emptyList())
    val capturedPhotos: StateFlow<List<EvidencePhoto>> = _capturedPhotos

    private var currentDisasterTypeEnum: DisasterType = DisasterType.UNKNOWN
    var damagePercentageEstimate: Int = 0

    init {
        val dao = AppDatabase.getDatabase(application).lossClaimDao()
        repository = LossClaimRepository(dao, application)
    }

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
        _currentPrompt.value = flowEngine.getInitialPrompt()
    }

    fun addPhoto(imagePath: String) {
        val lat = _currentLocation.value?.latitude ?: 0.0
        val lon = _currentLocation.value?.longitude ?: 0.0
        val photo = EvidencePhoto(
            imagePath = imagePath,
            latitude = lat,
            longitude = lon,
            timestamp = System.currentTimeMillis(),
            disasterType = _selectedDamageType.value,
            gatNumber = _currentFarmer.value?.gatNumber ?: "Unknown"
        )
        _capturedPhotos.value = _capturedPhotos.value + photo
    }

    fun advanceFlow(observation: String? = null) {
        val current = _currentPrompt.value ?: return
        val next = flowEngine.getNextPrompt(current.id, currentDisasterTypeEnum, observation)
        _currentPrompt.value = next
    }

    fun setSurveyFarmerResponses(calamity: String, percentage: Int) {
        setDamageType(calamity)
        damagePercentageEstimate = percentage
    }

    fun submitClaim(damagePercent: Int, compensation: Double, imagePath: String) {
        viewModelScope.launch {
            val farmer = _currentFarmer.value ?: return@launch
            val claim = LossClaimEntity(
                mobileNumber = farmer.mobileNumber,
                gatNumber = farmer.gatNumber,
                crop = farmer.primaryCrop ?: "Unknown",
                damageType = _selectedDamageType.value,
                damagePercentage = damagePercent,
                estimatedCompensation = compensation,
                imagePath = imagePath
            )
            repository.saveLossClaim(claim)
        }
    }
}