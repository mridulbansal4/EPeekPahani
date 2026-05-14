package io.sc.eppCordova.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.local.dao.SyncQueueDao
import io.sc.eppCordova.data.local.entity.Farmer
import io.sc.eppCordova.data.remote.dto.KycDto
import io.sc.eppCordova.data.remote.dto.PaymentDto
import io.sc.eppCordova.data.repository.ApiResult
import io.sc.eppCordova.data.repository.KycRepository
import io.sc.eppCordova.data.repository.PaymentRepository
import io.sc.eppCordova.domain.model.GatStatusItem
import io.sc.eppCordova.utils.NetworkUtils
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val cropRecordDao: CropRecordDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkUtils: NetworkUtils,
    private val kycRepository: KycRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _farmerData = MutableLiveData<Farmer?>()
    val farmerData: LiveData<Farmer?> = _farmerData

    private val _gatList = MutableLiveData<List<GatStatusItem>>()
    val gatList: LiveData<List<GatStatusItem>> = _gatList

    val pendingSyncCount: LiveData<Int> = syncQueueDao.getPendingSyncCount()
    val isOnline: LiveData<Boolean> = networkUtils.isOnline

    private val _kycStatus = MutableLiveData<KycDto?>()
    val kycStatus: LiveData<KycDto?> = _kycStatus

    private val _paymentStatus = MutableLiveData<PaymentDto?>()
    val paymentStatus: LiveData<PaymentDto?> = _paymentStatus

    private val _kycLoading = MutableLiveData<Boolean>(false)
    val kycLoading: LiveData<Boolean> = _kycLoading

    private val _paymentLoading = MutableLiveData<Boolean>(false)
    val paymentLoading: LiveData<Boolean> = _paymentLoading

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val farmer = cropRecordDao.getFarmer()
            _farmerData.postValue(farmer)

            val lands = cropRecordDao.getAllLandRecords()
            val statusItems = lands.map { land ->
                val crop = cropRecordDao.getCropRecordByGutNo(land.gutNo)
                val status = when {
                    crop == null -> "Pending"
                    crop.isSubmitted -> "Submitted"
                    else -> "Draft"
                }
                GatStatusItem(land, status)
            }
            _gatList.postValue(statusItems)

            farmer?.userId?.let { fetchKycAndPayments(it) }
        }
    }

    private suspend fun fetchKycAndPayments(farmerId: String) {
        _kycLoading.postValue(true)
        when (val result = kycRepository.getKycStatus(farmerId)) {
            is ApiResult.Success -> {
                _kycStatus.postValue(result.data.firstOrNull())
            }
            is ApiResult.Error -> {
                _kycStatus.postValue(null)
            }
        }
        _kycLoading.postValue(false)

        _paymentLoading.postValue(true)
        when (val result = paymentRepository.getPayments(farmerId)) {
            is ApiResult.Success -> {
                _paymentStatus.postValue(result.data.firstOrNull())
            }
            is ApiResult.Error -> {
                _paymentStatus.postValue(null)
            }
        }
        _paymentLoading.postValue(false)
    }

    fun getKycStatusLabel(): String {
        val status = _kycStatus.value?.status ?: return "नोंदणीकृत"
        return when {
            status.contains("VERIFIED", true) -> "KYC Verified"
            status.contains("PENDING", true) -> "KYC Pending"
            status.contains("REJECTED", true) -> "KYC Rejected"
            else -> status
        }
    }

    fun getPaymentStatusLabel(): String {
        val pay = _paymentStatus.value ?: return "कोणतेही देयक नाही"
        return when {
            pay.status?.contains("PAID", true) == true ->
                "₹${pay.amount ?: 0} - ${pay.transactionDate ?: ""}"
            pay.status?.contains("PENDING", true) == true -> "देयक प्रतीक्षेत"
            else -> pay.status ?: "-"
        }
    }
}
