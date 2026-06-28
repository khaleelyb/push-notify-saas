package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var websiteId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Website") },
        text = {
            Column {
                Text("Enter the unique ID of the website you want to subscribe to.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = websiteId,
                    onValueChange = { websiteId = it },
                    label = { Text("Website ID") },
                    placeholder = { Text("e.g. site-123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (websiteId.isNotBlank()) {
                        onConfirm(websiteId)
                    }
                },
                enabled = websiteId.isNotBlank()
            ) {
                Text("Subscribe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
