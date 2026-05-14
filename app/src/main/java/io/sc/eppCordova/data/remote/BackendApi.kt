package io.sc.eppCordova.data.remote

import io.sc.eppCordova.data.remote.dto.ClaimRequest
import io.sc.eppCordova.data.remote.dto.ClaimResponse
import io.sc.eppCordova.data.remote.dto.ClaimsListDto
import io.sc.eppCordova.data.remote.dto.FarmerDto
import io.sc.eppCordova.data.remote.dto.KycDto
import io.sc.eppCordova.data.remote.dto.PaymentDto
import io.sc.eppCordova.data.remote.dto.UploadResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApi {

    @GET("api/farmers")
    suspend fun getFarmers(): List<FarmerDto>

    @GET("api/farmers/{id}/claims")
    suspend fun getFarmerClaims(@Path("id") farmerId: String): ClaimsListDto

    @POST("api/claims")
    suspend fun submitClaim(@retrofit2.http.Body request: ClaimRequest): ClaimResponse

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(@Part file: MultipartBody.Part): UploadResponse

    @GET("api/payments")
    suspend fun getPayments(@Query("farmerId") farmerId: String? = null): List<PaymentDto>

    @GET("api/kyc")
    suspend fun getKycStatus(@Query("farmerId") farmerId: String? = null): List<KycDto>
}
