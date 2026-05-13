package io.sc.eppCordova.lossclaim.data

import android.content.Context
import io.sc.eppCordova.data.local.CsvParserService

class LossClaimRepository(private val dao: LossClaimDao, private val context: Context) {
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
                    area = csvFarmer.landHoldingHa ?: "0",
                    insuranceStatus = csvFarmer.pmKisanBeneficiary == "Yes"
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
