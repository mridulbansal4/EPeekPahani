package io.sc.eppCordova.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.dto.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val backendApi: BackendApi,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadFile(uri: Uri): ApiResult<UploadResponse> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return ApiResult.Error("Cannot open file")

            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val fileName = "upload_${System.currentTimeMillis()}"
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val response = backendApi.uploadFile(part)
            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                if (uploadResponse.success) {
                    ApiResult.Success(uploadResponse)
                } else {
                    ApiResult.Error(uploadResponse.message ?: "Upload failed")
                }
            } else {
                ApiResult.Error(response.message())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Upload failed")
        }
    }
}
