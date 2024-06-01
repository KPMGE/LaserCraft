package com.example.lasercraft.images.presentation.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lasercraft.R
import com.example.lasercraft.common.ErrorDialog
import com.example.lasercraft.navigation.Screens

@Composable
fun CameraPreviewScreen(navController: NavHostController) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = Preview.Builder().build()
    val previewView = remember {
        PreviewView(context)
    }
    val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }

    val viewModel: CameraPreviewViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    when (state) {
        CameraPreviewScreenState.SUCCESS -> {
            navController.navigate(Screens.EngraverImagePreview.route)
        }
        CameraPreviewScreenState.ERROR -> {
            ErrorDialog(
                title = stringResource(id = R.string.app_top_bar), description = stringResource(
                    id = R.string.camera_screen_preview_error_msg
                )
            )
        }
        else -> {
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
                Button(
                    modifier = Modifier.padding(bottom = 20.dp),
                    shape = CircleShape,
                    onClick = {
                        captureImage(imageCapture, context, onSuccess = { bitmap -> viewModel.processImage(bitmap) })
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(50.dp),
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    onSuccess: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("CAMERAX", "Image captured successfully")
                onSuccess(image.toBitmap())
                // make sure to close the image.
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CAMERAX", "Failed: $exception")
            }
        })
}

private fun Context.getCameraProvider(): ProcessCameraProvider {
    return ProcessCameraProvider.getInstance(this).get()
}