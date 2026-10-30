package com.rojas.syncpass.core.auth

/**
 * User-facing messages for the Cognito flow. OAuth codes, descriptions and
 * tokens must never be shown or logged.
 */
object CognitoAuthMessages {
    const val CANCELLED = "Inicio de sesión con Cognito cancelado."
    const val NETWORK_ERROR =
        "No se pudo conectar con Cognito. Revisa tu conexión e inténtalo de nuevo."
    const val AUTHORIZATION_FAILED =
        "No se pudo completar el inicio de sesión con Cognito. Inténtalo de nuevo."
    const val TOKEN_EXCHANGE_FAILED =
        "No se pudo completar el inicio de sesión con Cognito. Inténtalo de nuevo."
    const val INVALID_TOKEN =
        "El token de Cognito no es válido o expiró. Inténtalo de nuevo."
    const val IDENTITY_NOT_LINKED =
        "No se pudo iniciar sesión: la identidad de Cognito no está vinculada o la cuenta de SyncPass no está autorizada. Contacta a coordinación."

    fun authorizationMessage(cancelled: Boolean, networkError: Boolean): String = when {
        cancelled -> CANCELLED
        networkError -> NETWORK_ERROR
        else -> AUTHORIZATION_FAILED
    }

    fun tokenExchangeMessage(networkError: Boolean): String =
        if (networkError) NETWORK_ERROR else TOKEN_EXCHANGE_FAILED

    fun backendFailure(status: Int): String? = when (status) {
        401 -> INVALID_TOKEN
        403 -> IDENTITY_NOT_LINKED
        else -> null
    }
}
