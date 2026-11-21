package com.rojas.syncpass.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    @Test
    fun `message returns server error body message when available`() {
        val errorBody = """{"message":"Correo ya registrado"}""".toResponseBody("application/json".toMediaType())
        val response: Response<String> = Response.error(400, errorBody)
        val exception = HttpException(response)

        val message = ApiErrors.message(exception)

        assertEquals("Correo ya registrado", message)
    }

    @Test
    fun `message returns status mapping when no error body`() {
        val response: Response<String> = Response.error(401, "".toResponseBody("application/json".toMediaType()))
        val exception = HttpException(response)

        val message = ApiErrors.message(exception)

        assertTrue(message.contains("incorrectos"))
    }

    @Test
    fun `message returns 403 mapping`() {
        val response: Response<String> = Response.error(403, "".toResponseBody("application/json".toMediaType()))
        val exception = HttpException(response)

        val message = ApiErrors.message(exception)

        assertTrue(message.contains("permiso"))
    }

    @Test
    fun `message returns 404 mapping`() {
        val response: Response<String> = Response.error(404, "".toResponseBody("application/json".toMediaType()))
        val exception = HttpException(response)

        val message = ApiErrors.message(exception)

        assertTrue(message.contains("no existe"))
    }

    @Test
    fun `message returns generic mapping for other codes`() {
        val response: Response<String> = Response.error(500, "".toResponseBody("application/json".toMediaType()))
        val exception = HttpException(response)

        val message = ApiErrors.message(exception)

        assertTrue(message.contains("servidor"))
    }

    @Test
    fun `message returns offline text for IOException`() {
        val message = ApiErrors.message(IOException())

        assertTrue(message.contains("Sin conexi"))
    }

    @Test
    fun `message returns generic text for unexpected errors`() {
        val message = ApiErrors.message(RuntimeException())

        assertTrue(message.contains("inexperado") || message.contains("nuevamente"))
    }
}
