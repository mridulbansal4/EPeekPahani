package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.KycDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycRepository @Inject constructor(
    private val backendApi: BackendApi
) {
    suspend fun getKycStatus(farmerId: String?): ApiResult<List<KycDto>> {
        return try {
            val response = backendApi.getKycStatus(farmerId)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to fetch KYC status")
        }
    }
}
