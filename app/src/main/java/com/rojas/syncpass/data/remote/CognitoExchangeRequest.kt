package com.rojas.syncpass.data.remote

import com.google.gson.annotations.SerializedName

data class CognitoExchangeRequest(
    @SerializedName("idToken")
    val idToken: String? = null,
    @SerializedName("accessToken")
    val accessToken: String? = null
)
