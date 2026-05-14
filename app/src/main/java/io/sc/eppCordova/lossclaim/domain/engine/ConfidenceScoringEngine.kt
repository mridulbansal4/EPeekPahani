package io.sc.eppCordova.lossclaim.domain.engine

import io.sc.eppCordova.lossclaim.domain.model.AiObservation
import io.sc.eppCordova.lossclaim.domain.model.DisasterType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfidenceScoringEngine @Inject constructor() {

    fun calculateCompleteness(
        disasterType: DisasterType,
        observations: List<AiObservation>,
        farmerAnswers: Map<String, Any>
    ): Int {
        var score = 0
        
        // Base points for starting
        if (observations.isNotEmpty()) score += 20
        if (farmerAnswers.isNotEmpty()) score += 20

        // Observation diversity
        val uniqueObservations = observations.map { it.type }.distinct()
        if (uniqueObservations.size >= 2) score += 20

        // Correlation between farmer answers and AI observations
        val hasDisasterCorrelation = checkCorrelation(disasterType, uniqueObservations, farmerAnswers)
        if (hasDisasterCorrelation) score += 40

        return score.coerceIn(0, 100)
    }

    private fun checkCorrelation(disasterType: DisasterType, observations: List<String>, answers: Map<String, Any>): Boolean {
        // Simple mock logic - in reality, cross-check specific entities from voice with CV observations
        return when (disasterType) {
            DisasterType.FLOOD -> observations.contains("waterlogging") || answers.containsKey("flood_duration_days")
            DisasterType.DROUGHT -> observations.contains("dry_soil") || answers.containsKey("irrigation_source")
            DisasterType.HAILSTORM -> observations.contains("damaged_leaves")
            DisasterType.PEST_ATTACK -> observations.contains("pest_visible") || answers.containsKey("pest_name")
            DisasterType.DISEASE -> observations.contains("disease_spots")
            else -> false
        }
    }

    fun getMissingEvidence(
        disasterType: DisasterType,
        observations: List<AiObservation>,
        farmerAnswers: Map<String, Any>
    ): List<String> {
        val missing = mutableListOf<String>()
        val types = observations.map { it.type }
        
        if (observations.isEmpty()) missing.add("crop_overview_photo")
        
        when (disasterType) {
            DisasterType.FLOOD -> {
                if (!types.contains("waterlogging") && !types.contains("soil")) missing.add("soil_water_evidence")
            }
            DisasterType.PEST_ATTACK -> {
                if (!types.contains("close_up")) missing.add("close_up_leaf_capture")
            }
            else -> {}
        }
        
        if (farmerAnswers.isEmpty()) missing.add("farmer_verbal_confirmation")

        return missing
    }
}
