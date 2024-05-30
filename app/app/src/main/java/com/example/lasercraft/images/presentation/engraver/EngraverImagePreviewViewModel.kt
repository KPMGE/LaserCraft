package com.example.lasercraft.images.presentation.engraver

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lasercraft.ApiService
import com.example.lasercraft.mqtt.MqttClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MQTT_RECEIVE_IMAGE_TOPIC = "laser_engraver_img"

@HiltViewModel
class EngraverImagePreviewViewModel @Inject constructor(
    private val mqttClient: MqttClient,
    private val api: ApiService,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EngraverImagePreviewState>(EngraverImagePreviewState.LOADING)
    val uiState = _uiState.asStateFlow()

    init {
        mqttClient.connect(onSuccess = {
            mqttClient.subscribe(
                topic = MQTT_RECEIVE_IMAGE_TOPIC,
                onMessage = { handleImageReceived(it) }
            )
        })
    }

    fun engraveImage() = viewModelScope.launch(Dispatchers.IO) {
        try {
            api.engraveImage()
        } catch (ex: Exception) {
            Log.d("MQTT ON VIEWMODEL", ex.toString())
        }
    }

    private fun handleImageReceived(imgByteArray: ByteArray) {
        Log.d("MQTT ON VIEWMODEL", "mqtt message got")

        val bitmap = BitmapFactory.decodeByteArray(
            imgByteArray,
            0,
            imgByteArray.size
        )

        if (bitmap == null) {
            _uiState.value = EngraverImagePreviewState.ERROR
            Log.d("MQTT ON VIEWMODEL", "Error when parsing bitmap")
            return
        }

        Log.d("MQTT ON VIEWMODEL", "Bitmap parsed correctly")

        // Decode the drawable resource into a Bitmap
        val imageBitmap =  bitmap.asImageBitmap()
        _uiState.value = EngraverImagePreviewState.SUCCESS(imageBitmap)
    }
}