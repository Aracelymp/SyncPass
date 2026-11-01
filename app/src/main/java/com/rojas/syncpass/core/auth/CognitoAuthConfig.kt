package com.rojas.syncpass.core.auth

import android.content.Context
import android.net.Uri
import com.rojas.syncpass.BuildConfig
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

object CognitoAuthConfig {
    const val AWS_REGION = "us-east-1"
    val USER_POOL_DOMAIN = BuildConfig.COGNITO_USER_POOL_DOMAIN
    val CLIENT_ID = BuildConfig.COGNITO_CLIENT_ID
    const val REDIRECT_URI = "com.rojas.syncpass://callback"
    const val SCOPES = "openid email"

    fun getServiceConfig(): AuthorizationServiceConfiguration {
        val authEndpoint = Uri.parse("$USER_POOL_DOMAIN/oauth2/authorize")
        val tokenEndpoint = Uri.parse("$USER_POOL_DOMAIN/oauth2/token")
        return AuthorizationServiceConfiguration(authEndpoint, tokenEndpoint)
    }

    fun buildAuthRequest(serviceConfig: AuthorizationServiceConfiguration): AuthorizationRequest {
        return AuthorizationRequest.Builder(
            serviceConfig,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
            .setScope(SCOPES)
            .build()
    }

    fun getAuthService(context: Context): AuthorizationService {
        return AuthorizationService(context)
    }
}
