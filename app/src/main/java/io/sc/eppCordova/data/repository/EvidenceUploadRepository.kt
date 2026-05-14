package io.sc.eppCordova.data.repository

import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.UploadResponse
import io.sc.eppCordova.data.remote.dto.UploadedEvidenceDto
import io.sc.eppCordova.lossclaim.domain.model.EvidencePhoto
import io.sc.eppCordova.lossclaim.domain.model.EvidenceVideo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidenceUploadRepository @Inject constructor(
    private val backendApi: BackendApi
) {
    suspend fun uploadEvidenceBatch(
        photos: List<EvidencePhoto>,
        videos: List<EvidenceVideo>,
        farmerId: String
    ): ApiResult<List<UploadedEvidenceDto>> {
        val uploaded = mutableListOf<UploadedEvidenceDto>()

        for (photo in photos) {
            val file = File(photo.imagePath)
            if (!file.exists()) continue
            val result = uploadSingleFile(file, "image/jpeg", farmerId)
            if (result is ApiResult.Error) return result
            result.data?.let { uploaded.add(it) }
        }

        for (video in videos) {
            val file = File(video.videoPath)
            if (!file.exists()) continue
            val result = uploadSingleFile(file, "video/mp4", farmerId)
            if (result is ApiResult.Error) return result
            result.data?.let { uploaded.add(it) }
        }

        return ApiResult.Success(uploaded)
    }

    suspend fun uploadSingleFile(
        file: File,
        mimeType: String,
        farmerId: String
    ): ApiResult<UploadedEvidenceDto> {
        return try {
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val response = backendApi.uploadFile(part, farmerId)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val isVideo = mimeType.startsWith("video/")

                ApiResult.Success(
                    UploadedEvidenceDto(
                        type = if (isVideo) "video" else "image",
                        fileName = body.fileName ?: file.name,
                        path = body.path ?: body.url ?: file.name,
                        uploadedAt = body.uploadedAt ?: now
                    )
                )
            } else {
                ApiResult.Error(response.message())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Upload failed for ${file.name}")
        }
    }
}
