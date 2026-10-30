package com.rojas.syncpass.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/cognito/exchange")
    suspend fun exchangeCognito(@Body body: CognitoExchangeRequest): LoginResponse

    @POST("api/auth/cognito/link-request")
    suspend fun requestCognitoLink(@Body body: CognitoLinkAccessRequest): CognitoLinkRequestDto

    @GET("api/auth/me")
    suspend fun me(): UserDto
}
