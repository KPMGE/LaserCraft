package com.example.lasercraft.images.presentation.picker.presentation

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _uiState =
        MutableStateFlow(SingleImagePickerScreenState.LOADING)
    val uiState = _uiState.asStateFlow()

    fun processImage(imgUri: Uri) = viewModelScope.launch {
        _uiState.value = SingleImagePickerScreenState.LOADING

        val bytes = getBytesFromUri(imgUri)
        val fileName = "selectedImage.png"
        val body = bytes?.toRequestBody()

        if (body == null) {
            _uiState.value = SingleImagePickerScreenState.ERROR
            return@launch
        }

        val imagePart = MultipartBody.Part.createFormData("image", fileName, body)

        try {
            api.processImage(imagePart)
            Log.d(TAG, "Image sent for processing successfully")
            _uiState.value = SingleImagePickerScreenState.SUCCESS
        } catch (e: Exception) {
            _uiState.value = SingleImagePickerScreenState.ERROR
            Log.e(TAG, "Error while sending the image")
            Log.e(TAG, e.toString())
        }
    }

    @SuppressLint("Recycle")
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
        const val TAG = "SINGLE IMAGE PICKER"
    }
}