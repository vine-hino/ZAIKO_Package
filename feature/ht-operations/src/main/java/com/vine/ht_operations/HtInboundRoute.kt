package com.vine.ht_operations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.designsystem.component.ZaikoScreenScaffold

@Composable
fun HtInboundRoute(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    viewModel: HtInboundViewModel = hiltViewModel(),
) {
    val completedMessage = viewModel.completedMessage
    LaunchedEffect(completedMessage) {
        completedMessage?.let {
            viewModel.consumeCompleted()
            onComplete(it)
        }
    }

    val canSave = viewModel.productLookup.selected != null &&
            viewModel.locationLookup.selected != null &&
            viewModel.quantityText.toIntOrNull()?.let { it > 0 } == true &&
            !viewModel.isSubmitting

    ZaikoScreenScaffold(
        title = "HT 入庫登録",
        onBack = onBack,
    ) { padding ->
        ScrollForm(padding = padding) {
            MasterLookupField(
                label = "商品",
                state = viewModel.productLookup,
                onQueryChange = viewModel::onProductQueryChanged,
                onSelect = viewModel::selectProduct,
            )

            Spacer(modifier = Modifier.height(12.dp))

            MasterLookupField(
                label = "ロケーション",
                state = viewModel.locationLookup,
                onQueryChange = viewModel::onLocationQueryChanged,
                onSelect = viewModel::selectLocation,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.quantityText,
                onValueChange = viewModel::onQuantityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("数量") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.noteText,
                onValueChange = viewModel::onNoteChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("備考") },
            )

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = viewModel::submitInbound,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (viewModel.isSubmitting) {
                    CircularProgressIndicator()
                } else {
                    Text("登録")
                }
            }
        }
    }
}