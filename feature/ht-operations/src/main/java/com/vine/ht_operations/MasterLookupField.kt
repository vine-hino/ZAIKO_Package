package com.vine.ht_operations.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*

import androidx.compose.ui.Modifier
import com.vine.connector_api.MasterLookupItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterLookupField(
    label: String,
    state: LookupUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (MasterLookupItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = state.candidates.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {},
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(label) },
            trailingIcon = {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { },
        ) {
            state.candidates.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text("${item.code}  ${item.name}")
                    },
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}
