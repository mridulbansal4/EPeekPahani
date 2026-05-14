package io.sc.eppCordova.lossclaim.domain.engine

import android.graphics.BitmapFactory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import io.sc.eppCordova.BuildConfig
import io.sc.eppCordova.lossclaim.domain.model.AiObservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisionAnalysisEngine @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    suspend fun analyzeFrame(imageFile: File, expectedDisaster: String): FrameAnalysisResult = withContext(Dispatchers.IO) {
        // Check frame quality (simulated)
        val isClear = checkFrameQuality(imageFile)
        if (!isClear) {
            return@withContext FrameAnalysisResult(
                isClear = false,
                observations = emptyList(),
                qualityIssue = "blurry"
            )
        }

        val observations = analyzeWithGemini(imageFile, expectedDisaster)
        
        FrameAnalysisResult(
            isClear = true,
            observations = observations,
            qualityIssue = null
        )
    }

    private fun checkFrameQuality(imageFile: File): Boolean {
        // In reality: Check blur variance, brightness
        return true 
    }

    private suspend fun analyzeWithGemini(imageFile: File, expectedDisaster: String): List<AiObservation> {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return emptyList()
        
        val promptText = """
            Analyze this agricultural field image. The expected disaster is $expectedDisaster.
            Identify any visible crop damage symptoms. 
            Return a JSON array of strings containing one or more of these specific keys: 
            ["waterlogging", "dry_soil", "pest_visible", "disease_spots", "damaged_leaves", "lodging", "yellow_leaves"]
            Only return the JSON array, without markdown formatting. If no damage is visible, return [].
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(content {
                image(bitmap)
                text(promptText)
            })
            
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: "[]"
            val jsonArray = org.json.JSONArray(jsonText)
            val observationsList = mutableListOf<AiObservation>()
            
            for (i in 0 until jsonArray.length()) {
                val obsType = jsonArray.getString(i)
                observationsList.add(
                    AiObservation(
                        type = obsType,
                        confidence = 0.85f,
                        timestamp = System.currentTimeMillis(),
                        sourceImage = imageFile.absolutePath
                    )
                )
            }
            
            if (observationsList.isEmpty()) {
                emptyList()
            } else {
                observationsList
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty list on failure to avoid masking real API issues with mock data
            emptyList()
        }
    }

    private fun simulateDetection(disasterType: String, imagePath: String = "camera_frame"): AiObservation {
        val type = when (disasterType.uppercase()) {
            "FLOOD" -> "waterlogging"
            "DROUGHT" -> "dry_soil"
            "PEST_ATTACK" -> "pest_visible"
            "DISEASE" -> "disease_spots"
            "HAILSTORM" -> "damaged_leaves"
            "CYCLONE" -> "lodging"
            else -> "yellow_leaves"
        }
        return AiObservation(
            type = type,
            confidence = 0.85f,
            timestamp = System.currentTimeMillis(),
            sourceImage = imagePath
        )
    }

    data class FrameAnalysisResult(
        val isClear: Boolean,
        val observations: List<AiObservation>,
        val qualityIssue: String?
    )
}
