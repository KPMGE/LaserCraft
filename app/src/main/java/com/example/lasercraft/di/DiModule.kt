package com.example.lasercraft.di

import android.content.Context
import com.example.lasercraft.ApiService
import com.example.lasercraft.mqtt.MqttClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val BASE_URL = "https://1509-2804-56c-a52f-e100-2588-288c-d86b-af5a.ngrok-free.app"

@Module
@InstallIn(SingletonComponent::class)
object LaserCraftModule {
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
    }

    @Provides
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMqttClient(
        @ApplicationContext context: Context
    ) = MqttClient(context)
}
