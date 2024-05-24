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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lasercraft.navigation.Screens
import java.io.ByteArrayOutputStream

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

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageCapture)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        Button(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = { captureImage(imageCapture, context, onSuccess = { bytes ->
                viewModel.processImage(bytes)
                navController.navigate(Screens.EngraverImagePreview.route)
            }); }
        ) {
            Text(text = "Capture Image")
        }
    }
}

private fun captureImage(imageCapture: ImageCapture, context: Context, onSuccess: (ByteArray) -> Unit) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                Log.d("CAMERAX", "Image captured successfully")

                // resizing bitmap image
                val maxWidth = 1280
                val  maxHeight = 720
                val outputStream = ByteArrayOutputStream()
                val resizedBitmap = image.toBitmap().resize(maxWidth, maxHeight)
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

                // convert img bitmap into a byte array
                val imgBytes = outputStream.toByteArray()
                onSuccess(imgBytes)

                // make sure to close the image.
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.d("CAMERAX", "Failed: $exception")
            }
        })
}

fun Bitmap.resize(maxWidth: Int, maxHeight: Int): Bitmap {
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

private fun Context.getCameraProvider(): ProcessCameraProvider {
    return ProcessCameraProvider.getInstance(this).get()
}