package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.ClaimRequest
import io.sc.eppCordova.data.remote.dto.ClaimResponse
import io.sc.eppCordova.data.remote.dto.ClaimsListDto
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

@Singleton
class ClaimsRepository @Inject constructor(
    private val backendApi: BackendApi
) {
    suspend fun submitClaim(request: ClaimRequest): ApiResult<ClaimResponse> {
        return try {
            val response = backendApi.submitClaim(request)
            ApiResult.Success(response)
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Claim submission failed")
        }
    }

    suspend fun getFarmerClaims(farmerId: String): ApiResult<List<ClaimResponse>> {
        return try {
            val response = backendApi.getFarmerClaims(farmerId)
            ApiResult.Success(response.claims ?: emptyList())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to fetch claims")
        }
    }
}
