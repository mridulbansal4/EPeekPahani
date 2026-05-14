package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.ReportDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val backendApi: BackendApi
) {
    suspend fun getReports(farmerId: String?): ApiResult<List<ReportDto>> {
        return try {
            val response = backendApi.getReports(farmerId)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!.reports ?: emptyList())
            } else {
                ApiResult.Error(response.message())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to fetch reports")
        }
    }

    suspend fun getReportById(reportId: String): ApiResult<ReportDto> {
        return try {
            val response = backendApi.getReportById(reportId)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.message())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to fetch report")
        }
    }
}
