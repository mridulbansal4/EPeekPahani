package io.sc.eppCordova.ui.lossclaim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.local.dao.LandRecordDao
import io.sc.eppCordova.data.repository.ClaimsRepository
import io.sc.eppCordova.domain.model.CropRegistrationWithGat
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class WeatherCheckResult(val message: String, val type: Int)

@HiltViewModel
class LossClaimStep1ViewModel @Inject constructor(
    private val cropRecordDao: CropRecordDao,
    private val landRecordDao: LandRecordDao,
    private val claimsRepository: ClaimsRepository
) : ViewModel() {

    private val _verifiedGats = MutableLiveData<List<CropRegistrationWithGat>>()
    val verifiedGats: LiveData<List<CropRegistrationWithGat>> = _verifiedGats

    val selectedGat = MutableLiveData<CropRegistrationWithGat?>()
    val selectedLossType = MutableLiveData<String>()
    val incidentDate = MutableLiveData<String>()
    val affectedArea = MutableLiveData<Double>(0.0)

    private val _generatedClaimId = MutableLiveData<String>()
    val generatedClaimId: LiveData<String> = _generatedClaimId

    private val _weatherCheckResult = MutableLiveData<WeatherCheckResult?>()
    val weatherCheckResult: LiveData<WeatherCheckResult?> = _weatherCheckResult

    private var farmerId: String = ""

    init {
        loadVerifiedGats()
    }

    fun setFarmerId(id: String) {
        farmerId = id
    }

    private fun loadVerifiedGats() {
        viewModelScope.launch {
            val allLands = cropRecordDao.getAllLandRecords()
            val validGats = mutableListOf<CropRegistrationWithGat>()

            for (land in allLands) {
                val crop = cropRecordDao.getCropRecordByGutNo(land.gutNo)
                if (crop != null) {
                    validGats.add(CropRegistrationWithGat(crop, land))
                }
            }
            _verifiedGats.postValue(validGats)
        }
    }

    fun generateClaimId(district: String, taluka: String, gat: String) {
        if (_generatedClaimId.value == null) {
            val seq = Random.nextInt(100, 999)
            _generatedClaimId.value = "MH-2025-${district.take(3).uppercase()}-${taluka.take(3).uppercase()}-$gat-$seq"
        }
    }

    fun checkWeather(date: String, lossType: String) {
        if (lossType == "अतिवृष्टी" || lossType == "पूर") {
            if (Random.nextFloat() < 0.7f) {
                _weatherCheckResult.value = WeatherCheckResult("✅ या तारखेला पावसाची नोंद आहे", 1)
            } else {
                _weatherCheckResult.value = WeatherCheckResult("⚠️ हवामान डेटा जुळत नाही — पुढे जाता येईल", 2)
            }
        } else if (lossType == "दुष्काळ") {
            _weatherCheckResult.value = WeatherCheckResult("⚠️ हवामान डेटा जुळत नाही — पुढे जाता येईल", 2)
        } else {
            _weatherCheckResult.value = WeatherCheckResult("हवामान तपासणी लागू नाही", 0)
        }
    }

    fun validateAndProceed(): Boolean {
        return selectedGat.value != null &&
                !selectedLossType.value.isNullOrEmpty() &&
                !incidentDate.value.isNullOrEmpty() &&
                (affectedArea.value ?: 0.0) > 0.0
    }
}
