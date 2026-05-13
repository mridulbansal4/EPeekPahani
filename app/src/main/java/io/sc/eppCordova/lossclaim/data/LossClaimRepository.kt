package io.sc.eppCordova.lossclaim.data

import android.content.Context
import io.sc.eppCordova.data.local.CsvParserService
<<<<<<< Updated upstream

class LossClaimRepository(private val dao: LossClaimDao, private val context: Context) {
    private val csvParser = CsvParserService(context)
=======
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LossClaimRepository(private val dao: LossClaimDao, private val context: Context) {

    private val csvParserService = CsvParserService(context)
>>>>>>> Stashed changes

    suspend fun getFarmerByMobile(mobile: String): FarmerEntity? {
        var farmer = dao.getFarmerByMobile(mobile)
        if (farmer == null) {
<<<<<<< Updated upstream
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
=======
            val csvFarmer = csvParserService.getFarmerByMobile(mobile)
            if (csvFarmer != null) {
                farmer = FarmerEntity(
                    mobileNumber = csvFarmer.mobile,
                    farmerName = csvFarmer.name,
                    village = csvFarmer.village,
                    taluka = csvFarmer.taluka,
                    district = csvFarmer.district,
                    gatNumber = csvFarmer.khasraNumber,
                    crop = csvFarmer.primaryCrop,
                    area = csvFarmer.landHoldingHa + " Ha",
                    insuranceStatus = csvFarmer.pmKisanBeneficiary.equals("Yes", ignoreCase = true) || csvFarmer.hasKcc.equals("Yes", ignoreCase = true)
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
                    crop = "Soybean",
                    area = "2.3 Acre",
                    insuranceStatus = true
>>>>>>> Stashed changes
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
