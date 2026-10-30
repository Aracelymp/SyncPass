package com.rojas.syncpass.data.remote

data class AdminUserDto(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val cognitoLinked: Boolean
)

data class AdminUserCreateRequest(
    val username: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class UserActiveRequest(
    val active: Boolean
)

data class CognitoLinkRequestDto(
    val id: Long,
    val cognitoSub: String,
    val email: String,
    val fullName: String,
    val status: String,
    val createdAt: String?,
    val decidedAt: String?,
    val decidedBy: String?,
    val linkedUserId: Long?,
    val linkedUsername: String?,
    val rejectionReason: String?
)

data class CognitoLinkAccessRequest(
    val idToken: String
)

data class CognitoLinkApproveRequest(
    val userId: Long? = null,
    val newUser: AdminUserCreateRequest? = null
)

data class CognitoLinkRejectRequest(
    val reason: String
)
