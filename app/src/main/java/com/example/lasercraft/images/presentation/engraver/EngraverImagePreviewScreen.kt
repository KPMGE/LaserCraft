package com.example.lasercraft.images.presentation.engraver

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lasercraft.ui.theme.LaserCraftTheme

@Composable
fun EngraverImagePreviewScreen() {
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = { TopBar() },
        bottomBar = { BottomBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PreviewImageCard(isLoading = isLoading)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                style = MaterialTheme.typography.titleSmall,
                text = "LaserCraft",
            )
        },
        navigationIcon = {
            Icon(Icons.Rounded.ArrowBack, "back button")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewImageCard(isLoading: Boolean) {
    OutlinedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.7f),
        enabled = false,
        onClick = { /*TODO*/ }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp))
            } else {
                // Display the image once loaded
            }
        }
    }
}

@Composable
private fun BottomBar() {
    BottomAppBar(
        containerColor = Color.White,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp),
                onClick = { /*TODO*/ }
            ) {
                Text(text = "Retry")
            }
        }
    }
}

@Composable
private fun BitmapImage(bitmap: ImageBitmap) {
    Image(
        bitmap = bitmap,
        contentDescription = "laser engraver processed image",
        modifier = Modifier.size(200.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun EngraverImagePreviewScreenPreview() {
    LaserCraftTheme {
        EngraverImagePreviewScreen()
    }
}
