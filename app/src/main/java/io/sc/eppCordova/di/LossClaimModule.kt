package io.sc.eppCordova.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.sc.eppCordova.lossclaim.data.AppDatabase
import io.sc.eppCordova.lossclaim.data.LossClaimDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LossClaimModule {

    @Provides
    @Singleton
    fun provideLossClaimAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideLossClaimDao(database: AppDatabase): LossClaimDao {
        return database.lossClaimDao()
    }
}
