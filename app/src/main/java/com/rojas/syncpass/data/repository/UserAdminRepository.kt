package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.data.remote.AdminUserCreateRequest
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.AdminUserDto
import com.rojas.syncpass.data.remote.AuthApiService
import com.rojas.syncpass.data.remote.CognitoLinkAccessRequest
import com.rojas.syncpass.data.remote.CognitoLinkApproveRequest
import com.rojas.syncpass.data.remote.CognitoLinkRejectRequest
import com.rojas.syncpass.data.remote.CognitoLinkRequestDto
import com.rojas.syncpass.data.remote.SyncPassApiService
import com.rojas.syncpass.data.remote.UserActiveRequest
import java.io.IOException

class UserAdminRepository(
    private val api: SyncPassApiService,
    private val authApi: AuthApiService,
    private val networkMonitor: NetworkMonitor
) {

    suspend fun getUsers(): Result<List<AdminUserDto>> = runCatching {
        requireOnline()
        api.adminUsers()
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun createUser(request: AdminUserCreateRequest): Result<AdminUserDto> = runCatching {
        requireOnline()
        api.createAdminUser(request)
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun setActive(id: Long, active: Boolean): Result<AdminUserDto> = runCatching {
        requireOnline()
        api.setAdminUserActive(id, UserActiveRequest(active))
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun getPendingRequests(): Result<List<CognitoLinkRequestDto>> = runCatching {
        requireOnline()
        api.pendingCognitoLinkRequests()
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun approveRequest(
        id: Long,
        userId: Long?,
        newUser: AdminUserCreateRequest?
    ): Result<CognitoLinkRequestDto> = runCatching {
        requireOnline()
        api.approveCognitoLinkRequest(id, CognitoLinkApproveRequest(userId, newUser))
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun rejectRequest(id: Long, reason: String): Result<CognitoLinkRequestDto> = runCatching {
        requireOnline()
        api.rejectCognitoLinkRequest(id, CognitoLinkRejectRequest(reason))
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun requestAccess(idToken: String): Result<CognitoLinkRequestDto> = runCatching {
        requireOnline()
        authApi.requestCognitoLink(CognitoLinkAccessRequest(idToken))
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    private fun requireOnline() {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IOException("Sin conexión. Conéctate para realizar esta acción.")
        }
    }
}
