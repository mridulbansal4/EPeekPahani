package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.local.dao.CropRecordDao
import io.sc.eppCordova.data.local.entity.CropRecord
import io.sc.eppCordova.data.remote.ApiService
import io.sc.eppCordova.data.remote.OtpRequest
import io.sc.eppCordova.data.remote.VerifyOtpRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CropRepository @Inject constructor(
    private val apiService: ApiService,
    private val cropRecordDao: CropRecordDao
) {
    suspend fun submitSurvey(record: CropRecord): Boolean {
        return try {
            val response = apiService.submitSurvey(record)
            if (response.isSuccessful && response.body()?.success == true) {
                cropRecordDao.insertCropRecord(record.copy(isSubmitted = true))
                true
            } else {
                cropRecordDao.insertCropRecord(record.copy(isSubmitted = false))
                false
            }
        } catch (e: Exception) {
            cropRecordDao.insertCropRecord(record.copy(isSubmitted = false))
            false
        }
    }

    suspend fun syncPending() {
        val pending = cropRecordDao.getPendingRecords()
        pending.forEach { record ->
            try {
                val response = apiService.submitSurvey(record)
                if (response.isSuccessful && response.body()?.success == true) {
                    cropRecordDao.markAsSubmitted(record.cropId)
                }
            } catch (e: Exception) {
                // Ignore and try again next time
            }
        }
    }

    suspend fun sendOtp(mobile: String): Boolean {
        return try {
            val response = apiService.sendOtp(OtpRequest(mobile))
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun verifyOtp(mobile: String, otp: String): Boolean {
        return try {
            val response = apiService.verifyOtp(VerifyOtpRequest(mobile, otp))
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }
    
    // Fallbacks for Admin Units
    suspend fun getDivisions(): List<String> = try { apiService.getDivisions().body() ?: listOf("पुणे", "मुंबई", "नाशिक", "औरंगाबाद") } catch (e: Exception) { listOf("पुणे", "मुंबई", "नाशिक", "औरंगाबाद") }
    suspend fun getDistricts(division: String): List<String> = try { apiService.getDistricts(division).body() ?: listOf("पुणे", "सातारा", "सोलापूर") } catch (e: Exception) { listOf("पुणे", "सातारा", "सोलापूर") }
    suspend fun getTalukas(district: String): List<String> = try { apiService.getTalukas(district).body() ?: listOf("हवेली", "मुळशी", "खेड") } catch (e: Exception) { listOf("हवेली", "मुळशी", "खेड") }
    suspend fun getVillages(taluka: String): List<String> = try { apiService.getVillages(taluka).body() ?: listOf("उरुळी कांचन", "लोहगाव", "मांजरी") } catch (e: Exception) { listOf("उरुळी कांचन", "लोहगाव", "मांजरी") }
}
