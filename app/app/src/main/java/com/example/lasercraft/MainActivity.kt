package com.example.lasercraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.lasercraft.mqtt.MqttClient
import com.example.lasercraft.ui.theme.LaserCraftTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.rememberNavController
import com.example.lasercraft.navigation.LaserCraftNavGraph
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mqttClient: MqttClient

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mqttClient.connect(
            onSuccess = {
                Toast.makeText(this@MainActivity, "Mqtt connected successfully", Toast.LENGTH_LONG)
                    .show()
            },
            onError = {
                Toast.makeText(this@MainActivity, "Mqtt connection failed!", Toast.LENGTH_LONG)
                    .show()
            })

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // camera permission granted
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
