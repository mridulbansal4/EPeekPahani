package io.sc.eppCordova.lossclaim.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.sc.eppCordova.lossclaim.data.AppDatabase
import io.sc.eppCordova.lossclaim.data.FarmerEntity
import io.sc.eppCordova.lossclaim.data.LossClaimEntity
import io.sc.eppCordova.lossclaim.data.LossClaimRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LossClaimViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LossClaimRepository

    private val _currentFarmer = MutableStateFlow<FarmerEntity?>(null)
    val currentFarmer: StateFlow<FarmerEntity?> = _currentFarmer

    private val _selectedDamageType = MutableStateFlow<String>("")
    val selectedDamageType: StateFlow<String> = _selectedDamageType

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