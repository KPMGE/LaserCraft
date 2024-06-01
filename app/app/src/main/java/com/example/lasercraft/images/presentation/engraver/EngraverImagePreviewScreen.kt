package com.example.lasercraft.images.presentation.engraver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.lasercraft.R
import com.example.lasercraft.navigation.Screens

@Composable
fun EngraverImagePreviewScreen(navController: NavHostController) {
    val viewModel: EngraverImagePreviewViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsState().value

    BackHandler {
        navController.navigate(Screens.Home.route)
    }

    Scaffold(
        topBar = { TopBar(onBackClick = { navController.navigate(Screens.Home.route) }) },
        bottomBar = {
            when (state) {
                is EngraverImagePreviewState.LOADING -> {}
                is EngraverImagePreviewState.ERROR -> {
                    BottomBar(
                        buttonText = stringResource(id = R.string.engraver_preview_try_again_msg),
                        buttonColor = MaterialTheme.colorScheme.error,
                        onClick = { navController.popBackStack() }
                    )
                }
                is EngraverImagePreviewState.SUCCESS -> BottomBar(
                    buttonText = stringResource(id = R.string.engraver_preview_confirm_msg),
                    buttonColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.engraveImage() }
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
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = stringResource(id = R.string.engraver_preview_loading_msg))
                    }
                }
                is EngraverImagePreviewState.ERROR -> {
                    PreviewImageCard {
                        Text(text = stringResource(id = R.string.engraver_preview_error_msg))
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
private fun TopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                style = MaterialTheme.typography.titleMedium,
                text = stringResource(id = R.string.app_top_bar),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "back button")
            }
        },
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
private fun BottomBar(buttonText: String, buttonColor: Color, onClick: () -> Unit) {
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
                onClick = onClick
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
private fun BitmapImage(bitmap: ImageBitmap) {
    Image(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 10.dp),
        bitmap = bitmap,
        contentDescription = "laser engraver processed image",
    )
}