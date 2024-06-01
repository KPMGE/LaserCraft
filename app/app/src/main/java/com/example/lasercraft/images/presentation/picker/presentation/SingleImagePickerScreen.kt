package com.example.lasercraft.images.presentation.picker.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.lasercraft.R
import com.example.lasercraft.common.ErrorDialog
import com.example.lasercraft.navigation.Screens

private enum class State {
    IMAGE_PICKED,
    WAITING_IMAGE
}

@Composable
fun SingleImagePickerScreen(navController: NavHostController) {
    val viewModel: SingleImagePickerViewModel = hiltViewModel()
    var state by remember { mutableStateOf(State.WAITING_IMAGE) }
    val uiState = viewModel.uiState.collectAsState().value
    var uri by remember {
        mutableStateOf<Uri?>(null)
    }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            uri = it
            state = State.IMAGE_PICKED
        })

    LaunchedEffect(key1 = Unit) {
        singlePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    when (uiState) {
        SingleImagePickerScreenState.ERROR -> {
            ErrorDialog(
                title = stringResource(id = R.string.app_top_bar), description = stringResource(
                    id = R.string.single_image_picker_error_msg
                )
            )
        }

        SingleImagePickerScreenState.SUCCESS -> {
            navController.navigate(Screens.EngraverImagePreview.route)
        }

        SingleImagePickerScreenState.LOADING -> {}
    }

    if (state == State.IMAGE_PICKED) {
        Scaffold(
            topBar = {
                TopBar(
                    onBackClick = { navController.popBackStack() }
                )
            },
            bottomBar = {
                BottomBar(
                    buttonText = stringResource(id = R.string.single_image_picker_confirm_msg),
                    buttonColor = MaterialTheme.colorScheme.primary,
                    onButtonClick = {
                        uri?.let {
                            viewModel.processImage(it)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PreviewImageCard {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = uri,
                        contentDescription = null
                    )
                }
            }
        }
    }
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

@Composable
private fun BottomBar(buttonText: String, buttonColor: Color, onButtonClick: () -> Unit = {}) {
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
                onClick = onButtonClick
            ) {
                Text(text = buttonText)
            }
        }
    }
}
