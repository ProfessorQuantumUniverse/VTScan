// di/NetworkModule.kt
package com.quantum_prof.vtscansuite.di

import com.quantum_prof.vtscansuite.BuildConfig
import com.quantum_prof.vtscansuite.data.remote.VTScanApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS) // Uploads können dauern

        // Nur in Debug-Builds mitloggen: in einem Release-Build landete sonst der
        // "x-apikey"-Header des Nutzers im Logcat (und damit in jedem Bug-Report).
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                // HEADERS statt BODY: verhindert das Puffern großer Datei-Uploads (Performance + korrekter Fortschritt)
                level = HttpLoggingInterceptor.Level.HEADERS
                redactHeader("x-apikey")
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideVTScanApiService(okHttpClient: OkHttpClient, json: Json): VTScanApiService {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://www.virustotal.com/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(VTScanApiService::class.java) // Create the correct interface
    }
}