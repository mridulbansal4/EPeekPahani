package io.sc.eppCordova.lossclaim.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dao = AppDatabase.getDatabase(applicationContext).lossClaimDao()
            val repository = LossClaimRepository(dao, applicationContext)

            val unsyncedClaims = repository.getUnsyncedClaims()

            for (claim in unsyncedClaims) {
                // Mock API call to upload claim here
                // e.g., apiService.uploadClaim(claim)
                
                // If successful, mark as synced
                repository.markClaimSynced(claim.id)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}