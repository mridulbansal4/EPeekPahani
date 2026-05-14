package io.sc.eppCordova.lossclaim.domain.engine

import io.sc.eppCordova.lossclaim.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvidencePackageBuilder @Inject constructor(
    private val fraudEngine: FraudDetectionEngine,
    private val confidenceEngine: ConfidenceScoringEngine
) {

    fun buildPackage(
        farmerId: String,
        gatNumber: String,
        disasterType: DisasterType,
        photos: List<EvidencePhoto>,
        voiceInteractions: List<VoiceInteraction>,
        observations: List<AiObservation>,
        surveyDurationSeconds: Long,
        isMockLocationUsed: Boolean
    ): EvidencePackage {
        
        val fraudAnalysis = fraudEngine.analyzeSurvey(photos, surveyDurationSeconds, isMockLocationUsed)
        
        val farmerAnswers = voiceInteractions.map { it.extractedSemantics }.fold(mutableMapOf<String, Any>()) { acc, map ->
            acc.putAll(map)
            acc
        }
        
        val completenessScore = confidenceEngine.calculateCompleteness(disasterType, observations, farmerAnswers)
        
        // Simple damage estimation heuristic based on verbal answers
        val damageStr = farmerAnswers["damage_percentage"] as? String ?: "0"
        val estimatedDamage = damageStr.toIntOrNull() ?: calculateEstimatedDamage(observations)

        val recommendation = when {
            fraudAnalysis.autoReject -> "AUTO_REJECT_FRAUD"
            completenessScore < 40 -> "MANUAL_PHYSICAL_INSPECTION_REQUIRED"
            estimatedDamage > 33 -> "APPROVE_PAYOUT"
            else -> "REJECT_NO_LOSS"
        }

        return EvidencePackage(
            farmerId = farmerId,
            gatNumber = gatNumber,
            disasterType = disasterType,
            photos = photos,
            voiceInteractions = voiceInteractions,
            observations = observations,
            fraudAnalysis = fraudAnalysis,
            completenessScore = completenessScore,
            recommendation = recommendation,
            estimatedDamagePercentage = estimatedDamage
        )
    }

    private fun calculateEstimatedDamage(observations: List<AiObservation>): Int {
        if (observations.isEmpty()) return 0
        return 40 // Default heuristic if no farmer verbal confirmation
    }
}
