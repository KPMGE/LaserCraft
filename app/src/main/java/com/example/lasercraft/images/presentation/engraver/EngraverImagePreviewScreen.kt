package com.example.lasercraft.images.presentation.engraver

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lasercraft.ui.theme.LaserCraftTheme

@Composable
fun EngraverImagePreviewScreen() {
    val viewModel: EngraverImagePreviewViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsState().value

    Scaffold(
        topBar = { TopBar() },
        bottomBar = {
            when (state) {
                is EngraverImagePreviewState.LOADING -> null
                is EngraverImagePreviewState.ERROR -> BottomBar(
                    buttonText = "Retry",
                    buttonColor = MaterialTheme.colorScheme.error
                )
                is EngraverImagePreviewState.SUCCESS -> BottomBar(
                    buttonText = "Confirm",
                    buttonColor = MaterialTheme.colorScheme.primary
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is EngraverImagePreviewState.LOADING -> {
                    PreviewImageCard {
                        CircularProgressIndicator(modifier = Modifier.size(50.dp))
                    }
                }
                is EngraverImagePreviewState.ERROR -> {
                    PreviewImageCard {
                        Text(text = "Error while getting image, try again")
                    }
                }

                is EngraverImagePreviewState.SUCCESS -> {
                    PreviewImageCard {
                        BitmapImage(bitmap = state.image)
                    }
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
private fun PreviewImageCard(content: @Composable (ColumnScope.() -> Unit)) {
    OutlinedCard(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.7f),
        enabled = false,
        onClick = { }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
private fun BottomBar(buttonText: String, buttonColor: Color) {
    BottomAppBar(
        containerColor = Color.White,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                onClick = { /*TODO*/ }
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
private fun BitmapImage(bitmap: ImageBitmap) {
    Image(
        modifier = Modifier.fillMaxSize().padding(all = 10.dp),
        bitmap = bitmap,
        contentDescription = "laser engraver processed image",
    )
}

@Preview(showBackground = true)
@Composable
private fun EngraverImagePreviewScreenPreview() {
    LaserCraftTheme {
        EngraverImagePreviewScreen()
    }
}
