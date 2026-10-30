package com.rojas.syncpass.ui.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
    val isSuccess: Boolean = false,

    val isNewPasswordRequired: Boolean = false,
    val challengeSession: String? = null,
    val challengeUsername: String? = null,
    val newPassword: String = "",
    val confirmNewPassword: String = "",

    val isIdentityNotLinked: Boolean = false,
    val pendingIdToken: String = "",
    val accessRequestMessage: String? = null,
    val accessRequestError: String? = null
) {
    val canSubmit: Boolean =
        username.isNotBlank() && password.length >= 6 && !isLoading

    val canSubmitNewPassword: Boolean =
        newPassword.length >= 8 &&
            confirmNewPassword.isNotBlank() &&
            !isLoading
}