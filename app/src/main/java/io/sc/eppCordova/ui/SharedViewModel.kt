package io.sc.eppCordova.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sc.eppCordova.data.local.entity.AdminUnit
import io.sc.eppCordova.data.local.entity.CropRecord
import io.sc.eppCordova.data.local.entity.Farmer
import io.sc.eppCordova.data.local.entity.LandRecord
import io.sc.eppCordova.data.repository.AuthRepository
import io.sc.eppCordova.data.repository.CropRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CropFormData(
    var season: String = "",
    var cropName: String = "",
    var cropType: String = "",
    var sowDate: String = "",
    var harvestDate: String = "",
    var areaHectares: Double = 0.0
)

data class GpsData(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var accuracy: Float = 0f,
    var photo1Uri: String = "",
    var photo2Uri: String = "",
    var photo3Uri: String = ""
)

data class ClaimFormData(
    var lossType: String = "",
    var incidentDate: String = "",
    var affectedAreaHa: Double = 0.0,
    var gatNumber: String = "",
    var cropType: String = "",
    var cropName: String = ""
)

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val repository: CropRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _farmerState = MutableLiveData<Farmer>()
    val farmerState: LiveData<Farmer> = _farmerState

    private val _selectedAdminUnit = MutableLiveData<AdminUnit>()
    val selectedAdminUnit: LiveData<AdminUnit> = _selectedAdminUnit

    private val _selectedLandRecord = MutableLiveData<LandRecord>()
    val selectedLandRecord: LiveData<LandRecord> = _selectedLandRecord

    val cropFormData = MutableLiveData(CropFormData())
    val gpsData = MutableLiveData(GpsData())

    private val _submitState = MutableLiveData<UiState>(UiState.Idle)
    val submitState: LiveData<UiState> = _submitState

    private val _claimFormData = MutableLiveData(ClaimFormData())
    val claimFormData: LiveData<ClaimFormData> = _claimFormData

    private val _claimEvidenceUris = MutableLiveData<List<String>>(emptyList())
    val claimEvidenceUris: LiveData<List<String>> = _claimEvidenceUris

    fun setFarmer(farmer: Farmer) {
        _farmerState.value = farmer
    }

    fun setAdminUnit(unit: AdminUnit) {
        _selectedAdminUnit.value = unit
    }

    fun setLandRecord(record: LandRecord) {
        _selectedLandRecord.value = record
    }

    suspend fun getDivisions() = repository.getDivisions()
    suspend fun getDistricts(division: String) = repository.getDistricts(division)
    suspend fun getTalukas(district: String) = repository.getTalukas(district)
    suspend fun getVillages(taluka: String) = repository.getVillages(taluka)

    fun sendOtp(mobile: String, onResult: (Boolean, Farmer?) -> Unit) {
        viewModelScope.launch {
            val farmer = authRepository.findFarmerByMobile(mobile)
            if (farmer != null) {
                _farmerState.value = farmer
                val result = repository.sendOtp(mobile)
                onResult(true, farmer)
            } else {
                onResult(false, null)
            }
        }
    }

    fun verifyOtp(mobile: String, otp: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.verifyOtp(mobile, otp)
            onResult(result)
        }
    }

    fun submitSurvey() {
        _submitState.value = UiState.Loading
        viewModelScope.launch {
            val record = CropRecord(
                userId = _farmerState.value?.userId ?: "",
                khataNo = _selectedLandRecord.value?.khataNo ?: "",
                gutNo = _selectedLandRecord.value?.gutNo ?: "",
                season = cropFormData.value?.season ?: "",
                cropName = cropFormData.value?.cropName ?: "",
                cropType = cropFormData.value?.cropType ?: "",
                areaHectares = cropFormData.value?.areaHectares ?: 0.0,
                sowDate = cropFormData.value?.sowDate ?: "",
                harvestDate = cropFormData.value?.harvestDate ?: "",
                photo1Uri = gpsData.value?.photo1Uri ?: "",
                photo2Uri = gpsData.value?.photo2Uri ?: "",
                photo3Uri = gpsData.value?.photo3Uri ?: "",
                latitude = gpsData.value?.latitude ?: 0.0,
                longitude = gpsData.value?.longitude ?: 0.0,
                timestamp = System.currentTimeMillis()
            )
            val success = repository.submitSurvey(record)
            if (success) {
                _submitState.value = UiState.Success
            } else {
                _submitState.value = UiState.Error("सर्व्हर त्रुटी - स्थानिक संचयन केले")
            }
        }
    }

    fun resetState() {
        _submitState.value = UiState.Idle
        cropFormData.value = CropFormData()
        gpsData.value = GpsData()
    }

    fun setPhoto1Uri(uri: String) { gpsData.value = gpsData.value?.copy(photo1Uri = uri) }
    fun setPhoto2Uri(uri: String) { gpsData.value = gpsData.value?.copy(photo2Uri = uri) }
    fun setPhoto3Uri(uri: String) { gpsData.value = gpsData.value?.copy(photo3Uri = uri) }

    fun setClaimFormData(lossType: String, incidentDate: String, affectedAreaHa: Double,
                         gatNumber: String, cropType: String, cropName: String) {
        _claimFormData.value = ClaimFormData(lossType, incidentDate, affectedAreaHa, gatNumber, cropType, cropName)
    }

    fun setClaimEvidenceUris(uris: List<String>) {
        _claimEvidenceUris.value = uris
    }
}
