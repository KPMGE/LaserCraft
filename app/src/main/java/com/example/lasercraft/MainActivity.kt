package com.example.lasercraft

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lasercraft.camera.presentation.CameraPreviewScreen
import com.example.lasercraft.ui.theme.LaserCraftTheme

enum class ScreenState {
    PENDING_CAMERA_PERMISSION,
    OPEN_CAMERA,
    IDLE
}

class MainActivity : ComponentActivity() {
    private val state = mutableStateOf(ScreenState.PENDING_CAMERA_PERMISSION)

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
                state.value = ScreenState.IDLE
            }

            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }

        setContent {
            LaserCraftTheme {
                when(state.value) {
                    ScreenState.IDLE, ScreenState.PENDING_CAMERA_PERMISSION -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Button(
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(170.dp),
                                shape = MaterialTheme.shapes.large,
                                onClick = { state.value = ScreenState.OPEN_CAMERA },
                            ) {
                                Text("Open camera")
                            }
                        }
                    }
                    ScreenState.OPEN_CAMERA -> {
                        CameraPreviewScreen(onCaptureClick = {  state.value = ScreenState.IDLE })
                    }
                }

            }
        }
    }
}