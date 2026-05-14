package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.PaymentDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val backendApi: BackendApi
) {
    suspend fun getPayments(farmerId: String?): ApiResult<List<PaymentDto>> {
        return try {
            val response = backendApi.getPayments(farmerId)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to fetch payments")
        }
    }
}
