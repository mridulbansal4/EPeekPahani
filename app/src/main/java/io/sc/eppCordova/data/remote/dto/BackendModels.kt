package io.sc.eppCordova.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClaimRequest(
    val farmerId: String,
    val cropType: String,
    val claimType: String,
    val village: String,
    val latitude: Double,
    val longitude: Double,
    val incidentDate: String,
    val affectedAreaHa: Double,
    val gatNumber: String,
    val description: String? = null,
    val evidenceUrls: List<String>? = null
)

data class ClaimResponse(
    val claimId: String?,
    val farmerId: String?,
    val claimType: String?,
    val cropType: String?,
    val workflowStage: String?,
    val confidenceScore: Double?,
    val rainfallMatched: Boolean?,
    val geoVerified: Boolean?,
    val duplicateRisk: Boolean?,
    val reviewRemarks: String?,
    val landParcelId: String?,
    val assignedOfficer: String?,
    val dbtStatus: String?,
    val paymentStatus: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class FarmerDto(
    val id: String?,
    val name: String?,
    val mobile: String?,
    val village: String?,
    val district: String?,
    val taluka: String?,
    val kycStatus: String?
)

data class PaymentDto(
    val farmerId: String?,
    val claimId: String?,
    val amount: Double?,
    val status: String?,
    val transactionDate: String?,
    val utrNumber: String?
)

data class KycDto(
    val farmerId: String?,
    val status: String?,
    val verifiedAt: String?,
    val documentType: String?,
    val remarks: String?
)

data class UploadResponse(
    val success: Boolean,
    val url: String?,
    val message: String?
)

data class ApiResultDto<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)

data class ClaimsListDto(
    val claims: List<ClaimResponse>?
)
