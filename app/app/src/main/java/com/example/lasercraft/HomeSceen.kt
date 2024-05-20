package com.example.lasercraft

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.lasercraft.navigation.Screens

@Composable
fun HomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(250.dp),
                onClick = { navController.navigate(Screens.Camera.route) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Take a picture")
                    Spacer(modifier = Modifier.height(10.dp))
                    Icon(
                        modifier = Modifier.size(60.dp),
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Button(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(250.dp),
                onClick = { navController.navigate(Screens.ImagePicker.route) }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pick an image")
                    Spacer(modifier = Modifier.height(10.dp))
                    Icon(
                        modifier = Modifier.size(60.dp),
                        imageVector = Icons.Rounded.Collections,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = "LaserCraft",
            )
        }
    )
}

