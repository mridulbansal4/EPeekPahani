package io.sc.eppCordova.lossclaim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LossClaimDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: FarmerEntity)

    @Query("SELECT * FROM farmer_table WHERE mobileNumber = :mobileNumber")
    suspend fun getFarmerByMobile(mobileNumber: String): FarmerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLossClaim(claim: LossClaimEntity): Long

    @Query("SELECT * FROM loss_claim_table WHERE isSynced = 0")
    suspend fun getUnsyncedClaims(): List<LossClaimEntity>

    @Query("UPDATE loss_claim_table SET isSynced = 1 WHERE id = :claimId")
    suspend fun markClaimAsSynced(claimId: Int)
}