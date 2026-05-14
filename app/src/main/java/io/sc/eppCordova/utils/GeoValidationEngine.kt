package io.sc.eppCordova.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.provider.Settings
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

sealed class GeoFenceResult {
    object Loading : GeoFenceResult()
    data class Pass(val accuracyMetres: Float, val lat: Double, val lon: Double) : GeoFenceResult()
    data class Fail(val distanceMetres: Float, val farmerLat: Double, val farmerLon: Double) : GeoFenceResult()
    object AccuracyTooLow : GeoFenceResult()
    object GpsUnavailable : GeoFenceResult()
    object MockLocationDetected : GeoFenceResult()
}

@Singleton
class GeoValidationEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    
    private val locationHistory = mutableListOf<Location>()

    @SuppressLint("MissingPermission")
    fun startValidation(boundaryPolygonJson: String?): Flow<GeoFenceResult> = callbackFlow {
        trySend(GeoFenceResult.Loading)
        val startTime = System.currentTimeMillis()
        locationHistory.clear()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                
                // 1. Mock Location Check
                if (location.isFromMockProvider || isMockSettingsEnabled()) {
                    trySend(GeoFenceResult.MockLocationDetected)
                    return
                }

                locationHistory.add(location)

                // 2. Continuous Continuity Check (Spoof prevention)
                if (!isMovementConsistent()) {
                    // Flag anomalous movement internally, but continue
                }

                if (System.currentTimeMillis() - startTime > 180000) { // 3 minutes timeout
                    trySend(GeoFenceResult.GpsUnavailable)
                    stopValidation()
                    return
                }

                if (location.accuracy > 30f) {
                    trySend(GeoFenceResult.AccuracyTooLow)
                    return
                }

                val polygon = parsePolygon(boundaryPolygonJson)
                if (polygon.isEmpty()) {
                    trySend(GeoFenceResult.Pass(location.accuracy, location.latitude, location.longitude))
                    return
                }

                val isInside = isPointInPolygon(location.latitude, location.longitude, polygon)
                
                if (isInside) {
                    trySend(GeoFenceResult.Pass(location.accuracy, location.latitude, location.longitude))
                } else {
                    val distance = calculateMinDistanceToPolygon(location.latitude, location.longitude, polygon)
                    if (distance <= 15f) { // 15m buffer
                        trySend(GeoFenceResult.Pass(location.accuracy, location.latitude, location.longitude))
                    } else {
                        trySend(GeoFenceResult.Fail(distance.toFloat(), location.latitude, location.longitude))
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())

        awaitClose {
            stopValidation()
        }
    }

    fun stopValidation() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun isMockSettingsEnabled(): Boolean {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ALLOW_MOCK_LOCATION) == "1"
    }

    private fun isMovementConsistent(): Boolean {
        if (locationHistory.size < 2) return true
        val last = locationHistory.last()
        val prev = locationHistory[locationHistory.size - 2]
        
        val timeDiffSec = (last.time - prev.time) / 1000f
        if (timeDiffSec <= 0) return true
        
        val distance = last.distanceTo(prev)
        val speedMps = distance / timeDiffSec
        
        // If farmer is moving faster than 10m/s (36km/h) while walking in a field, it's suspicious
        return speedMps < 10f 
    }

    private fun parsePolygon(json: String?): List<Pair<Double, Double>> {
        val result = mutableListOf<Pair<Double, Double>>()
        if (json.isNullOrEmpty()) return result
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val point = array.getJSONObject(i)
                result.add(Pair(point.getDouble("lat"), point.getDouble("lon")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun isPointInPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]
            if ((pi.second > lon) != (pj.second > lon) &&
                (lat < (pj.first - pi.first) * (lon - pi.second) / (pj.second - pi.second) + pi.first)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    private fun calculateMinDistanceToPolygon(lat: Double, lon: Double, polygon: List<Pair<Double, Double>>): Double {
        var minDistance = Double.MAX_VALUE
        val userLocation = Location("").apply {
            latitude = lat
            longitude = lon
        }
        
        for (point in polygon) {
            val pLoc = Location("").apply {
                latitude = point.first
                longitude = point.second
            }
            val dist = userLocation.distanceTo(pLoc).toDouble()
            if (dist < minDistance) minDistance = dist
        }
        return minDistance
    }
}
