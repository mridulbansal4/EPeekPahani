package io.sc.eppCordova.lossclaim.domain.engine

import com.google.ai.client.generativeai.GenerativeModel
import io.sc.eppCordova.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceProcessingEngine @Inject constructor() {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Processes transcript to get semantic extraction using Gemini
    suspend fun processTranscript(transcript: String): VoiceResult = withContext(Dispatchers.IO) {
        val semantics = extractSemanticsWithGemini(transcript)
        
        VoiceResult(
            transcript = transcript,
            extractedSemantics = semantics,
            confidence = 0.9f // We assume high confidence if Gemini returns a parseable JSON
        )
    }

    private suspend fun extractSemanticsWithGemini(transcript: String): Map<String, Any> {
        val prompt = """
            You are an agricultural AI assistant. Extract semantic information from the farmer's transcript.
            The farmer might speak in Marathi or Hindi or English.
            Return a JSON object with the extracted keys. Only return the JSON, without markdown formatting.
            Possible keys: "flood_duration_days" (integer), "water_present" (boolean), "damage_percentage" (integer), "irrigation_source" (string), "pest_name" (string).
            
            Transcript: "$transcript"
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.trim()?.removePrefix("```json")?.removeSuffix("```")?.trim() ?: "{}"
            
            val jsonObject = JSONObject(jsonText)
            val map = mutableMapOf<String, Any>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty map on error instead of hardcoded mock to reflect true AI status
            emptyMap()
        }
    }

    data class VoiceResult(
        val transcript: String,
        val extractedSemantics: Map<String, Any>,
        val confidence: Float
    )
}
