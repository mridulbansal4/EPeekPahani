package io.sc.eppCordova.lossclaim.domain.engine

import io.sc.eppCordova.lossclaim.domain.model.EvidencePhoto
import io.sc.eppCordova.lossclaim.domain.model.FraudAnalysis
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FraudDetectionEngine @Inject constructor() {

    fun analyzeSurvey(
        photos: List<EvidencePhoto>,
        surveyDurationSeconds: Long,
        isMockLocationUsed: Boolean
    ): FraudAnalysis {
        val flags = mutableListOf<String>()
        var riskScore = 0
        var autoReject = false

        // 1. Mock GPS Detection
        if (isMockLocationUsed) {
            flags.add("MOCK_LOCATION_DETECTED")
            riskScore += 100
            autoReject = true
        }

        // 2. Survey Duration check
        if (surveyDurationSeconds < 10) {
            flags.add("SURVEY_TOO_FAST")
            riskScore += 30
        }

        // 3. Duplicate Frames Check (Simulated)
        val hasDuplicates = checkDuplicateFrames(photos)
        if (hasDuplicates) {
            flags.add("DUPLICATE_FRAMES_DETECTED")
            riskScore += 50
            autoReject = true
        }

        // 4. GPS Consistency
        if (photos.size >= 2) {
            val p1 = photos.first()
            val p2 = photos.last()
            val distance = calculateDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            if (distance > 500) { // Jumped 500 meters during survey
                flags.add("UNREALISTIC_GPS_MOVEMENT")
                riskScore += 60
            }
        }

        return FraudAnalysis(
            riskScore = riskScore.coerceIn(0, 100),
            flags = flags,
            autoReject = autoReject
        )
    }

    private fun checkDuplicateFrames(photos: List<EvidencePhoto>): Boolean {
        // Simulated structural similarity index (SSIM) check
        return false
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}
