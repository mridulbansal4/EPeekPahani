package io.sc.eppCordova.lossclaim.data

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.ClaimRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: LossClaimDao,
    private val backendApi: BackendApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val unsyncedClaims = dao.getUnsyncedClaims()

            for (claim in unsyncedClaims) {
                // Upload image 1
                val url1 = uploadFile(claim.imagePath1)
                // Upload image 2
                val url2 = uploadFile(claim.imagePath2)
                // Upload video
                val videoUrl = uploadFile(claim.videoPath)

                // If any upload fails (null), we retry later
                if (url1 == null || url2 == null || videoUrl == null) {
                    return@withContext Result.retry()
                }

                val request = ClaimRequest(
                    farmerId = claim.mobileNumber,
                    cropType = claim.crop,
                    claimType = claim.damageType,
                    village = "MockVillage", // Assuming fetched from farmer info elsewhere
                    latitude = claim.latitude,
                    longitude = claim.longitude,
                    incidentDate = claim.timestamp.toString(),
                    affectedAreaHa = claim.damagePercentage.toDouble() / 100.0,
                    gatNumber = claim.gatNumber,
                    description = "Automated survey submission",
                    evidenceUrls = listOf(url1, url2, videoUrl)
                )

                val response = backendApi.submitClaim(request)
                if (response.isSuccessful && response.body() != null) {
                    dao.markClaimAsSynced(claim.id)
                } else {
                    return@withContext Result.retry()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadFile(filePath: String): String? {
        if (filePath.isBlank()) return null
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val mimeType = if (filePath.endsWith(".mp4", true)) "video/mp4" else "image/jpeg"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            
            val response = backendApi.uploadFile(part)
            if (response.isSuccessful) {
                response.body()?.path ?: response.body()?.url ?: file.name
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}