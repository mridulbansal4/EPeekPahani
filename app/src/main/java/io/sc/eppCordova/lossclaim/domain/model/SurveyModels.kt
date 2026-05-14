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
    CYCLONE
}

enum class QuestionType {
    INFO,
    YES_NO,
    SLIDER,
    OPTIONS,
    CAPTURE_PHOTO
}

data class AiPrompt(
    val id: String,
    val textEnglish: String,
    val textMarathi: String,
    val textHindi: String,
    val type: QuestionType,
    val stage: SurveyStage,
    val requiredDisaster: DisasterType? = null,
    val options: List<String> = emptyList(), // For OPTIONS type
    val nextPromptId: String? = null // For static flows, otherwise engine determines
)

data class EvidencePhoto(
    val imagePath: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val disasterType: String,
    val gatNumber: String
)
