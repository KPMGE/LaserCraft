package com.example.lasercraft.images.presentation.engraver

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import com.example.lasercraft.BuildConfig
import com.example.lasercraft.mqtt.MqttClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EngraverImagePreviewViewModel @Inject constructor(
    private val mqttClient: MqttClient,
    private val api: ApiService,
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<EngraverImagePreviewState>(EngraverImagePreviewState.LOADING)
    val uiState = _uiState.asStateFlow()

    init {
        mqttClient.subscribe(
            topic = BuildConfig.MQTT_RECEIVE_IMAGE_TOPIC,
            onMessage = { handleImageReceived(it) },
            onSubscribeError = { _uiState.value = EngraverImagePreviewState.ERROR }
        )
    }

    fun engraveImage() = viewModelScope.launch(Dispatchers.IO) {
        try {
            api.engraveImage()
        } catch (ex: Exception) {
            Log.d(TAG, ex.toString())
            _uiState.value = EngraverImagePreviewState.ERROR
        }
    }

    private fun handleImageReceived(imgByteArray: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(
            imgByteArray,
            0,
            imgByteArray.size
        )

        if (bitmap == null) {
            _uiState.value = EngraverImagePreviewState.ERROR
            Log.d(TAG, "Error when parsing bitmap")
            return
        }

        // Decode the drawable resource into a Bitmap
        val imageBitmap = bitmap.asImageBitmap()
        _uiState.value = EngraverImagePreviewState.SUCCESS(imageBitmap)
    }

    private companion object {
        const val TAG = "ENGRAVER IMAGE PREVIEW"
    }
}
