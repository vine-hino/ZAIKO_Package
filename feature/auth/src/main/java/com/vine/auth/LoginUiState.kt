package com.vine.auth

data class LoginUiState(
    val userId: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
) {
    val canLogin: Boolean
        get() = userId.isNotBlank() && password.isNotBlank() && !isLoading
}