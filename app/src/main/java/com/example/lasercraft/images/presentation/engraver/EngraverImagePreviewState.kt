package com.example.lasercraft.images.presentation.engraver

import androidx.compose.ui.graphics.ImageBitmap

sealed interface EngraverImagePreviewState {
    data object LOADING : EngraverImagePreviewState
    data object ERROR : EngraverImagePreviewState
    data class SUCCESS(val image: ImageBitmap) : EngraverImagePreviewState
}