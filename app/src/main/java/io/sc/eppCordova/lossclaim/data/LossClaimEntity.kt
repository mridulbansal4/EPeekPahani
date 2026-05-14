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
    val imagePath: String,
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)