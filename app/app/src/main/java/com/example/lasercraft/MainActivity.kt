package com.example.lasercraft

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lasercraft.images.presentation.camera.CameraPreviewScreen
import com.example.lasercraft.images.presentation.picker.presentation.SingleImagePicker
import com.example.lasercraft.mqtt.MqttClient
import com.example.lasercraft.ui.theme.LaserCraftTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.compose.rememberNavController
import com.example.lasercraft.images.presentation.engraver.EngraverImagePreviewScreen
import com.example.lasercraft.navigation.LaserCraftNavGraph
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mqttClient: MqttClient

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // handle granted permission
            } else {
                // handle not granted
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // persmission granted
            }

            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }

        setContent {
            val navController = rememberNavController()

            LaserCraftTheme {
                LaserCraftNavGraph(navController = navController)
            }
        }
    }
}
