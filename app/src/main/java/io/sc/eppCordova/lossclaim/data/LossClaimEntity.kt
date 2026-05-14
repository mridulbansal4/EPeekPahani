package io.sc.eppCordova.lossclaim.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loss_claim_table")
data class LossClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mobileNumber: String,
    val gatNumber: String,
    val crop: String,
    val damageType: String,
    val damagePercentage: Int,
    val estimatedCompensation: Double,
    val imagePath1: String,
    val imagePath2: String,
    val videoPath: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)