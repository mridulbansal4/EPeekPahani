package io.sc.eppCordova.lossclaim.domain.model

enum class SurveyStage {
    FIELD_ORIENTATION,
    AI_DAMAGE_DETECTION,
    FARMER_CONFIRMATION,
    COMPLETED
}

enum class DisasterType {
    UNKNOWN,
    FLOOD,
    HAILSTORM,
    DROUGHT,
    PEST_ATTACK,
    DISEASE,
    CYCLONE,
    EXCESS_RAINFALL,
    FIRE_DAMAGE,
    ANIMAL_DAMAGE,
    UNSEASONAL_RAIN,
    WIND_DAMAGE,
    OTHER
}

enum class QuestionType {
    INFO,
    YES_NO,
    SLIDER,
    OPTIONS,
    CAPTURE_PHOTO,
    CAPTURE_VIDEO,
    VERBAL_CONFIRM,
    END_SURVEY
}

data class AiPrompt(
    val id: String,
    val textEnglish: String,
    val textMarathi: String,
    val textHindi: String,
    val type: QuestionType,
    val stage: SurveyStage,
    val requiredDisaster: DisasterType? = null,
    val options: List<String> = emptyList(),
    val nextPromptId: String? = null
)

data class EvidencePhoto(
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val disasterType: String,
    val gatNumber: String,
    val observations: List<String> = emptyList(),
    val isClear: Boolean = true
)

data class EvidenceVideo(
    val videoPath: String,
    val durationSeconds: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val keyframes: List<String> = emptyList(),
    val observations: List<String> = emptyList(),
    val gpsVerified: Boolean = true
)

data class VoiceInteraction(
    val promptId: String,
    val promptText: String,
    val rawAudioPath: String,
    val transcript: String,
    val extractedSemantics: Map<String, Any>,
    val confidenceScore: Float
)

data class AiObservation(
    val type: String, // e.g., "yellow_leaves", "waterlogging"
    val confidence: Float,
    val timestamp: Long,
    val sourceImage: String
)

data class FraudAnalysis(
    val riskScore: Int, // 0-100
    val flags: List<String>,
    val autoReject: Boolean
)

data class EvidencePackage(
    val farmerId: String,
    val gatNumber: String,
    val disasterType: DisasterType,
    val photos: List<EvidencePhoto>,
    val videos: List<EvidenceVideo> = emptyList(),
    val voiceInteractions: List<VoiceInteraction>,
    val observations: List<AiObservation>,
    val fraudAnalysis: FraudAnalysis,
    val completenessScore: Int,
    val recommendation: String,
    val estimatedDamagePercentage: Int
)
