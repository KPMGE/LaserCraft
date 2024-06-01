package com.example.lasercraft.images.presentation.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class CameraPreviewViewModel @Inject constructor(
    private val api: ApiService
): ViewModel() {

    private val _state = MutableStateFlow(CameraPreviewScreenState.IDLE)
    val state = _state.asStateFlow()

    fun processImage(image: Bitmap) = viewModelScope.launch {
        // resize image
        val outputStream = ByteArrayOutputStream()
        val resizedBitmap = image.resize(IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT)
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        // convert img bitmap into a byte array
        val imgBytes = outputStream.toByteArray()

        val body = imgBytes.toRequestBody()
        val imgFileName = "imageCapture.png"
        val imagePart = MultipartBody.Part.createFormData("image", imgFileName, body)

        try {
            Log.d(TAG, "Image sent for processing successfully")
            _state.value = CameraPreviewScreenState.SUCCESS
            api.processImage(imagePart)
        } catch (e: Exception) {
            _state.value = CameraPreviewScreenState.ERROR
            Log.e(TAG, "Error while sending the image")
            Log.e(TAG, e.toString())
        }
    }

    private fun ByteArray.toRequestBody(mimeType: String): RequestBody {
        return this.toString().toRequestBody(mimeType.toMediaTypeOrNull())
    }

    private companion object {
        const val TAG = "CAMERA VIEW MODEL"
        const val IMAGE_MAX_WIDTH = 1280
        const val IMAGE_MAX_HEIGHT = 720
    }
}
private fun Bitmap.resize(maxWidth: Int, maxHeight: Int): Bitmap {
    val width = this.width
    val height = this.height
    val aspectRatio = width.toFloat() / height.toFloat()

    var newWidth = maxWidth
    var newHeight = maxHeight

    if (newWidth < newHeight) {
        newHeight = (newWidth / aspectRatio).toInt()
    } else {
        newWidth = (newHeight * aspectRatio).toInt()
    }

    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}
