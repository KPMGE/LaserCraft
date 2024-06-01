package com.example.lasercraft.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.lasercraft.ui.theme.LaserCraftTheme

@Composable
fun ErrorDialog(title: String, description: String, onClick: () -> Unit = {}) {
    val showDialog = remember { mutableStateOf(true) }

    Column {
        if (showDialog.value) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { showDialog.value = false },
                title = { Text(title) },
                text = { Text(description) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                        onClick = {
                            onClick()
                            showDialog.value = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDialogPreview() {
    LaserCraftTheme {
        ErrorDialog(
            title = "Error",
            description = "An error occurred"
        )
    }
}