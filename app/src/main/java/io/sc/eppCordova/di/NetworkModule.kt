package io.sc.eppCordova.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.sc.eppCordova.data.remote.ApiService
import io.sc.eppCordova.data.remote.BackendApi
import io.sc.eppCordova.data.remote.EPeekPahaniApi
import io.sc.eppCordova.data.remote.MockApiInterceptor
import io.sc.eppCordova.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MockRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMockInterceptor(@ApplicationContext context: Context): MockApiInterceptor =
        MockApiInterceptor(context)

    @Provides
    @Singleton
    fun provideMockOkHttpClient(mockInterceptor: MockApiInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @MockRetrofit
    fun provideMockRetrofit(mockOkHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(mockOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(@MockRetrofit retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideEPeekPahaniApi(@MockRetrofit retrofit: Retrofit): EPeekPahaniApi =
        retrofit.create(EPeekPahaniApi::class.java)

    @Provides
    @Singleton
    @BackendRetrofit
    fun provideBackendRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(Constants.BACKEND_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBackendApi(@BackendRetrofit retrofit: Retrofit): BackendApi =
        retrofit.create(BackendApi::class.java)
}
