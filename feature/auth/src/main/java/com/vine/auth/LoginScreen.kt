package com.vine.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    LoginScreen(
        uiState = uiState,
        onUserIdChanged = viewModel::onUserIdChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onLoginClick = viewModel::login,
    )
}

@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onUserIdChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "在庫管理 HT ログイン",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = uiState.userId,
            onValueChange = onUserIdChanged,
            label = { Text("ユーザーID") },
            singleLine = true,
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            value = uiState.password,
            onValueChange = onPasswordChanged,
            label = { Text("パスワード") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )

        uiState.errorMessage?.let { message ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            enabled = uiState.canLogin,
            onClick = onLoginClick,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("ログイン")
            }
        }

        TextButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = {},
        ) {
            Text("パスワード再発行は後で実装")
        }
    }
}