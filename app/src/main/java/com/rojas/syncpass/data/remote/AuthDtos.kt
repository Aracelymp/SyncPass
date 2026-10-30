package com.rojas.syncpass.data.remote

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 604800,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class ApiErrorDto(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null
)

data class MessageResponse(val message: String)
