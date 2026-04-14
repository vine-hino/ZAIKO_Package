package com.vine.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onUserIdChanged(value: String) {
        _uiState.update {
            it.copy(
                userId = value,
                errorMessage = null,
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                errorMessage = null,
            )
        }
    }

    fun login() {
        val current = _uiState.value

        when {
            current.userId.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "ユーザーIDを入力してください") }
            }

            current.password.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "パスワードを入力してください") }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        errorMessage = null,
                    )
                }
            }
        }
    }
}