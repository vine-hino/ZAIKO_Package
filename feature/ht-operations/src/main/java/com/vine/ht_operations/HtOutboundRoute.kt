package com.vine.ht_operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vine.designsystem.component.ZaikoScreenScaffold

@Composable
fun HtOutboundRoute(
    onBack: () -> Unit,
    onComplete: (String) -> Unit,
    viewModel: HtOutboundViewModel = hiltViewModel(),
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
        title = "HT 出庫登録",
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.quantityText,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text("数量") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.noteText,
                onValueChange = viewModel::onNoteChanged,
                label = { Text("備考") },
                singleLine = true,
            )

            viewModel.errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
                onClick = viewModel::submitOutbound,
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