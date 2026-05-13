package io.sc.eppCordova.lossclaim.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farmer_table")
data class FarmerEntity(
    @PrimaryKey val mobileNumber: String,
    val farmerName: String,
    val village: String,
    val taluka: String,
    val district: String,
    val gatNumber: String,
    val primaryCrop: String?,
    val secondaryCrop: String?,
    val area: String,
    val insuranceStatus: Boolean
)