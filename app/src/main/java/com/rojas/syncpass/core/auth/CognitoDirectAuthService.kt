
package com.rojas.syncpass.core.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface CognitoDirectAuthResult {
    data class Authenticated(
        val idToken: String,
        val accessToken: String
    ) : CognitoDirectAuthResult

    data class NewPasswordRequired(
        val session: String,
        val username: String
    ) : CognitoDirectAuthResult

    data class Failure(val message: String) : CognitoDirectAuthResult
}

class CognitoDirectAuthService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun signIn(username: String, password: String): CognitoDirectAuthResult =
        withContext(Dispatchers.IO) {
            if (username.isBlank() || password.isBlank()) {
                return@withContext CognitoDirectAuthResult.Failure(
                    "Ingresa tu usuario y contraseña."
                )
            }

            try {
                val response = callCognito(
                    target = "AWSCognitoIdentityProviderService.InitiateAuth",
                    body = JSONObject()
                        .put("AuthFlow", "USER_PASSWORD_AUTH")
                        .put("ClientId", CognitoAuthConfig.CLIENT_ID)
                        .put(
                            "AuthParameters",
                            JSONObject()
                                .put("USERNAME", username.trim())
                                .put("PASSWORD", password)
                        )
                )

                parseAuthResponse(response, username.trim())
            } catch (e: CognitoServiceException) {
                CognitoDirectAuthResult.Failure(
                    when (e.code) {
                        "NotAuthorizedException", "UserNotFoundException" ->
                            "Usuario o contraseña incorrectos."
                        "UserNotConfirmedException" ->
                            "La cuenta de Cognito todavía no está confirmada."
                        "PasswordResetRequiredException" ->
                            "Debes restablecer la contraseña de tu cuenta."
                        "InvalidParameterException" ->
                            "Cognito rechazó la solicitud. Revisa la configuración del cliente."
                        "TooManyRequestsException", "LimitExceededException" ->
                            "Demasiados intentos. Espera un momento y vuelve a intentarlo."
                        else -> e.message ?: "No se pudo iniciar sesión con Cognito."
                    }
                )
            } catch (e: Exception) {
                CognitoDirectAuthResult.Failure(
                    "No se pudo conectar con Cognito. Comprueba tu conexión e inténtalo nuevamente."
                )
            }
        }

    suspend fun completeNewPassword(
        username: String,
        newPassword: String,
        session: String
    ): CognitoDirectAuthResult = withContext(Dispatchers.IO) {
        if (newPassword.length < 8) {
            return@withContext CognitoDirectAuthResult.Failure(
                "La nueva contraseña debe tener al menos 8 caracteres."
            )
        }

        try {
            val response = callCognito(
                target = "AWSCognitoIdentityProviderService.RespondToAuthChallenge",
                body = JSONObject()
                    .put("ChallengeName", "NEW_PASSWORD_REQUIRED")
                    .put("ClientId", CognitoAuthConfig.CLIENT_ID)
                    .put("Session", session)
                    .put(
                        "ChallengeResponses",
                        JSONObject()
                            .put("USERNAME", username)
                            .put("NEW_PASSWORD", newPassword)
                    )
            )

            parseAuthResponse(response, username)
        } catch (e: CognitoServiceException) {
            CognitoDirectAuthResult.Failure(
                when (e.code) {
                    "InvalidPasswordException" ->
                        "La contraseña no cumple la política de Cognito."
                    "NotAuthorizedException" ->
                        "El desafío expiró o ya no es válido. Inicia sesión nuevamente."
                    else -> e.message ?: "No se pudo cambiar la contraseña."
                }
            )
        } catch (e: Exception) {
            CognitoDirectAuthResult.Failure(
                "No se pudo completar el cambio de contraseña."
            )
        }
    }

    private fun parseAuthResponse(
        response: JSONObject,
        username: String
    ): CognitoDirectAuthResult {
        val challenge = response.optString("ChallengeName")

        if (challenge == "NEW_PASSWORD_REQUIRED") {
            val session = response.optString("Session")
            if (session.isBlank()) {
                return CognitoDirectAuthResult.Failure(
                    "Cognito no devolvió la sesión del cambio de contraseña."
                )
            }

            return CognitoDirectAuthResult.NewPasswordRequired(session, username)
        }

        val authentication = response.optJSONObject("AuthenticationResult")
            ?: return CognitoDirectAuthResult.Failure(
                "Cognito no devolvió los tokens de autenticación."
            )

        val idToken = authentication.optString("IdToken")
        val accessToken = authentication.optString("AccessToken")

        if (idToken.isBlank() && accessToken.isBlank()) {
            return CognitoDirectAuthResult.Failure(
                "Cognito devolvió una respuesta sin tokens."
            )
        }

        return CognitoDirectAuthResult.Authenticated(
            idToken = idToken,
            accessToken = accessToken
        )
    }

    private fun callCognito(
        target: String,
        body: JSONObject
    ): JSONObject {
        val request = Request.Builder()
            .url(
                "https://cognito-idp.${CognitoAuthConfig.AWS_REGION}.amazonaws.com/"
            )
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/x-amz-json-1.1")
            .header("X-Amz-Target", target)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val json = if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)

            if (!response.isSuccessful) {
                val code = json.optString("__type")
                    .substringAfterLast("#")
                    .substringAfterLast(":")
                val message = json.optString("message", "Error de autenticación")
                throw CognitoServiceException(code, message)
            }

            return json
        }
    }

    private class CognitoServiceException(
        val code: String,
        message: String
    ) : IOException(message)

    private companion object {
        val JSON_MEDIA_TYPE = "application/x-amz-json-1.1".toMediaType()
    }
}
