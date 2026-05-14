package io.sc.eppCordova.lossclaim.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sc.eppCordova.data.local.CsvParserService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LossClaimRepository @Inject constructor(private val dao: LossClaimDao, @ApplicationContext private val context: Context) {
    private val csvParser = CsvParserService(context)

    suspend fun getFarmerByMobile(mobile: String): FarmerEntity? {
        var farmer = dao.getFarmerByMobile(mobile)
        if (farmer == null) {
            val csvFarmer = csvParser.getFarmerByMobile(mobile)
            if (csvFarmer != null) {
                farmer = FarmerEntity(
                    mobileNumber = mobile,
                    farmerName = csvFarmer.name ?: "Unknown",
                    village = csvFarmer.village ?: "Unknown",
                    taluka = csvFarmer.taluka ?: "Unknown",
                    district = csvFarmer.district ?: "Unknown",
                    gatNumber = csvFarmer.khasraNumber ?: "N/A",
                    primaryCrop = csvFarmer.primaryCrop,
                    secondaryCrop = csvFarmer.secondaryCrop,
                    area = (csvFarmer.landHoldingHa ?: "0") + " Ha",
                    insuranceStatus = csvFarmer.pmKisanBeneficiary?.equals("Yes", ignoreCase = true) == true || csvFarmer.hasKcc?.equals("Yes", ignoreCase = true) == true
                )
                dao.insertFarmer(farmer)
            } else {
                // Fallback for demo purposes
                farmer = FarmerEntity(
                    mobileNumber = mobile,
                    farmerName = "Ramesh Kumar (Mock)",
                    village = "Shirur",
                    taluka = "Shirur",
                    district = "Pune",
                    gatNumber = "102",
                    primaryCrop = "Soybean",
                    secondaryCrop = null,
                    area = "2.3 Acre",
                    insuranceStatus = true
                )
                dao.insertFarmer(farmer)
            }
        }
        return farmer
    }

    suspend fun saveLossClaim(claim: LossClaimEntity): Long {
        return dao.insertLossClaim(claim)
    }

    suspend fun getUnsyncedClaims(): List<LossClaimEntity> {
        return dao.getUnsyncedClaims()
    }

    suspend fun markClaimSynced(id: Int) {
        dao.markClaimAsSynced(id)
    }
}
