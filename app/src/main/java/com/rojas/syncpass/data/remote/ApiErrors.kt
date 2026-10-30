package com.rojas.syncpass.data.remote

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

object ApiErrors {
    private val gson = Gson()

    fun message(error: Throwable): String = when (error) {
        is HttpException -> {
            val body = try {
                error.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            val parsed = body?.let {
                runCatching { gson.fromJson(it, ApiErrorDto::class.java) }.getOrNull()
            }
            parsed?.message?.takeIf { it.isNotBlank() } ?: when (error.code()) {
                401 -> "Usuario o contraseña incorrectos"
                403 -> "Tu rol no tiene permiso para esta acción"
                404 -> "El recurso solicitado no existe"
                else -> "El servidor respondió con un error (${error.code()})"
            }
        }
        is IOException -> "Sin conexión. Los datos se guardarán y sincronizarán cuando vuelvas a estar en línea."
        else -> "Ocurrió un error inesperado. Intenta nuevamente."
    }
}
