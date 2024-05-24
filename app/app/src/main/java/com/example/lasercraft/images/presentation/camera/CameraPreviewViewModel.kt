package com.example.lasercraft.images.presentation.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    private val api: ApiService
): ViewModel() {
    fun processImage(imgBytes: ByteArray) = viewModelScope.launch {
        val body = imgBytes.toRequestBody()
        val imgFileName = "imageCapture.png"
        val imagePart = MultipartBody.Part.createFormData("image", imgFileName, body)

        try {
            api.processImage(imagePart)
            Log.d(TAG, "Image sent for processing successfully")
        } catch (e: Exception) {
            Log.d(TAG, "Error while sending the image")
            Log.d(TAG, e.toString())
        }
    }

    private fun ByteArray.toRequestBody(mimeType: String): RequestBody {
        return this.toString().toRequestBody(mimeType.toMediaTypeOrNull())
    }

    private companion object {
        const val TAG = "CAMERA VIEW MODEL"
    }
}