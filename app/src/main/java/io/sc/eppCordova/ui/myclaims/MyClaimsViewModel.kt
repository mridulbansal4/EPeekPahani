package io.sc.eppCordova.ui.myclaims

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.local.dao.LossClaimDao
import io.sc.eppCordova.data.local.entity.CropRecord
import io.sc.eppCordova.data.remote.dto.ClaimResponse
import io.sc.eppCordova.data.remote.dto.ReportDto
import io.sc.eppCordova.data.repository.ApiResult
import io.sc.eppCordova.data.repository.ClaimsRepository
import io.sc.eppCordova.data.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ClaimsLoadState {
    object Loading : ClaimsLoadState()
    data class Success(val claims: List<ClaimResponse>) : ClaimsLoadState()
    data class Error(val message: String) : ClaimsLoadState()
}

@HiltViewModel
class MyClaimsViewModel @Inject constructor(
    private val cropRecordDao: CropRecordDao,
    private val lossClaimDao: LossClaimDao,
    private val claimsRepository: ClaimsRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _cropRecords = MutableLiveData<List<CropRecord>>()
    val cropRecords: LiveData<List<CropRecord>> = _cropRecords

    private val _claimsState = MutableLiveData<ClaimsLoadState>(ClaimsLoadState.Loading)
    val claimsState: LiveData<ClaimsLoadState> = _claimsState

    private val _selectedClaim = MutableLiveData<ClaimResponse?>()
    val selectedClaim: LiveData<ClaimResponse?> = _selectedClaim

    private val _selectedReport = MutableLiveData<ReportDto?>()
    val selectedReport: LiveData<ReportDto?> = _selectedReport

    private val _certificates = MutableLiveData<List<CropRecord>>()
    val certificates: LiveData<List<CropRecord>> = _certificates

    private val _allReports = MutableLiveData<List<ReportDto>>(emptyList())

    private var pollingJob: Job? = null
    private var currentFarmerId: String? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val farmer = cropRecordDao.getFarmer()
            val farmerId = farmer?.userId
            if (farmerId != null) {
                currentFarmerId = farmerId
                val allLands = cropRecordDao.getAllLandRecords()
                val crops = mutableListOf<CropRecord>()
                for (land in allLands) {
                    cropRecordDao.getCropRecordByGutNo(land.gutNo)?.let { crops.add(it) }
                }
                _cropRecords.postValue(crops)
                _certificates.postValue(crops.filter { it.certificateId != null })
                fetchClaims(farmerId)
                fetchReports(farmerId)
                startPolling(farmerId)
            }
        }
    }

    private suspend fun fetchClaims(farmerId: String) {
        when (val result = claimsRepository.getFarmerClaims(farmerId)) {
            is ApiResult.Success -> {
                _claimsState.value = ClaimsLoadState.Success(result.data)
            }
            is ApiResult.Error -> {
                _claimsState.value = ClaimsLoadState.Error(result.message)
            }
        }
    }

    private suspend fun fetchReports(farmerId: String) {
        when (val result = reportRepository.getReports(farmerId)) {
            is ApiResult.Success -> {
                _allReports.value = result.data
                syncSelectedReport()
            }
            is ApiResult.Error -> { }
        }
    }

    private fun syncSelectedReport() {
        val claimId = _selectedClaim.value?.claimId
        if (claimId != null) {
            _selectedReport.value = _allReports.value?.find { it.reportId == claimId }
        }
    }

    private fun startPolling(farmerId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (currentFarmerId != null) {
                    fetchClaims(farmerId)
                    fetchReports(farmerId)
                }
            }
        }
    }

    fun selectClaim(claim: ClaimResponse) {
        _selectedClaim.value = claim
        _selectedReport.value = _allReports.value?.find { it.reportId == claim.claimId }
    }

    fun retry() {
        val farmerId = currentFarmerId
        if (farmerId != null) {
            _claimsState.value = ClaimsLoadState.Loading
            viewModelScope.launch {
                fetchClaims(farmerId)
                fetchReports(farmerId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
