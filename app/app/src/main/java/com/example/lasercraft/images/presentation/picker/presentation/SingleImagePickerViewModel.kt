package com.example.lasercraft.images.presentation.picker.presentation

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SingleImagePickerViewModel @Inject constructor(
    private val api: ApiService,
    application: Application
): AndroidViewModel(Application()) {
    private val contentResolver: ContentResolver by lazy {
        application.contentResolver
    }

    fun processImage(imgUri: Uri) = viewModelScope.launch {
        val bytes = getBytesFromUri(imgUri)
        val fileName = "test.png"
        val body = bytes?.toRequestBody()!!

        val imagePart = MultipartBody.Part.createFormData("image", fileName, body)

        try {
            api.processImage(imagePart)
            Log.d(TAG, "Image sent for processing successfully")
        } catch (e: Exception) {
            Log.d(TAG, "Error while sending the image")
            Log.d(TAG, e.toString())
        }
    }

    private fun getBytesFromUri(uri: Uri): ByteArray? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len: Int
            while (inputStream!!.read(buffer).also { len = it }!= -1) {
                byteBuffer.write(buffer, 0, len)
            }
            byteBuffer.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun ByteArray.toRequestBody(mimeType: String): RequestBody {
        return this.toString().toRequestBody(mimeType.toMediaTypeOrNull())
    }

    private fun Uri.toRequestBody(mimeType: MediaType?): RequestBody {
        return this.toString().toRequestBody(mimeType)
    }

    private companion object {
        const val TAG = "PROCESS IMAGE"
    }
}