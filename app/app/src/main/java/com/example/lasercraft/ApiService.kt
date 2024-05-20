package com.example.lasercraft

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("img")
    suspend fun processImage(@Part image: MultipartBody.Part)
}