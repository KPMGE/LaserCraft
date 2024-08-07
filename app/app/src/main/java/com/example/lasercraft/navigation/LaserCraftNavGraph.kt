package com.example.lasercraft.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.example.lasercraft.HomeScreen
import com.example.lasercraft.images.presentation.camera.CameraPreviewScreen
import com.example.lasercraft.images.presentation.engraver.EngraverImagePreviewScreen
import com.example.lasercraft.images.presentation.picker.presentation.SingleImagePickerScreen

sealed class Screens(
    val route: String
) {
    data object EngraverImagePreview : Screens("engraver_image_preview")
    data object Home : Screens("home")
    data object Camera : Screens("camera")
    data object ImagePicker : Screens("image picker")
}

@Composable
fun LaserCraftNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screens.Home.route,
    ) {
        composable(route = Screens.EngraverImagePreview.route) {
            EngraverImagePreviewScreen(navController)
        }
        composable(route = Screens.Home.route) {
            HomeScreen(navController)
        }
        composable(route = Screens.Camera.route) {
            CameraPreviewScreen(navController)
        }
        composable(route = Screens.ImagePicker.route, deepLinks = listOf(
            navDeepLink {
                action = Intent.ACTION_SEND
                mimeType = "image/*"
            },
            navDeepLink {
                action = Intent.ACTION_SEND
                mimeType = "text/*"
            }
        )) {
            SingleImagePickerScreen(navController)
        }
    }
}
