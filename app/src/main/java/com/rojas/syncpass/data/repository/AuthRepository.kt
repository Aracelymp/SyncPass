package com.rojas.syncpass.data.repository

import com.rojas.syncpass.core.auth.CognitoAuthMessages
import com.rojas.syncpass.core.network.NetworkMonitor
import com.rojas.syncpass.core.util.Hash
import com.rojas.syncpass.data.local.AppDatabase
import com.rojas.syncpass.data.local.DemoLocalSeeder
import com.rojas.syncpass.data.local.UserDao
import com.rojas.syncpass.data.local.toEntity
import com.rojas.syncpass.data.remote.ApiErrors
import com.rojas.syncpass.data.remote.AuthApiService
import com.rojas.syncpass.data.remote.CognitoExchangeRequest
import com.rojas.syncpass.data.remote.CognitoLinkAccessRequest
import com.rojas.syncpass.data.remote.CognitoLinkRequestDto
import com.rojas.syncpass.data.remote.LoginRequest
import com.rojas.syncpass.data.session.Session
import com.rojas.syncpass.data.session.SessionManager
import retrofit2.HttpException
import java.io.IOException
import com.rojas.syncpass.core.auth.CognitoDirectAuthService

sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data class Failure(val message: String) : LoginResult
    data class IdentityNotLinked(val idToken: String) : LoginResult

    data class NewPasswordRequired(
        val session: String,
        val username: String
    ) : LoginResult
}
class AuthRepository(
    private val api: AuthApiService,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor,
    private val cognitoDirectAuthService: CognitoDirectAuthService
) {
suspend fun loginWithCognito(
    username: String,
    password: String
): LoginResult {
    if (!networkMonitor.isCurrentlyOnline()) {
        return LoginResult.Failure(
            "Se requiere conexión a internet para iniciar sesión con Cognito."
        )
    }

    return when (
        val result = cognitoDirectAuthService.signIn(username, password)
    ) {
        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.Authenticated -> {
            val idToken = result.idToken
            if (idToken.isNotBlank()) {
                exchangeCognitoIdToken(idToken)
            } else {
                exchangeCognitoAccessToken(result.accessToken)
            }
        }

        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.NewPasswordRequired -> {
            LoginResult.NewPasswordRequired(
                session = result.session,
                username = result.username
            )
        }

        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.Failure -> {
            LoginResult.Failure(result.message)
        }
    }
}

suspend fun completeCognitoNewPassword(
    username: String,
    newPassword: String,
    challengeSession: String
): LoginResult {
    return when (
        val result = cognitoDirectAuthService.completeNewPassword(
            username = username,
            newPassword = newPassword,
            session = challengeSession
        )
    ) {
        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.Authenticated -> {
            val idToken = result.idToken
            if (idToken.isNotBlank()) {
                exchangeCognitoIdToken(idToken)
            } else {
                exchangeCognitoAccessToken(result.accessToken)
            }
        }

        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.NewPasswordRequired -> {
            LoginResult.Failure(
                "Cognito todavía solicita cambiar la contraseña. Inténtalo nuevamente."
            )
        }

        is com.rojas.syncpass.core.auth.CognitoDirectAuthResult.Failure -> {
            LoginResult.Failure(result.message)
        }
    }
}
    suspend fun login(username: String, password: String): LoginResult {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || password.isBlank()) {
            return LoginResult.Failure("Ingresa tu usuario y contraseña")
        }
        DemoLocalSeeder.seedIfEmpty(database)

        if (networkMonitor.isCurrentlyOnline()) {
            try {
                val response = api.login(LoginRequest(cleanUsername, password))
                val remote = response.user
                val local = userDao.findByUsernameOrEmail(cleanUsername)
                // Server ids normally match the seed. If the server was recreated,
                // remove a stale row that could collide with the unique username.
                if (local != null && local.id != remote.id) {
                    userDao.deleteByUsername(local.username)
                }
                val now = System.currentTimeMillis()
                userDao.upsert(remote.toEntity(password, now))
                val session = Session(
                    userId = remote.id,
                    username = remote.username,
                    email = remote.email,
                    fullName = remote.fullName,
                    role = remote.role,
                    token = response.accessToken,
                    offline = false
                )
                sessionManager.save(session)
                return LoginResult.Success(session)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    return LoginResult.Failure("Usuario o contraseña incorrectos")
                }
                return LoginResult.Failure(ApiErrors.message(e))
            } catch (e: IOException) {
                // Server unreachable: fall back to the locally verified credential.
            } catch (e: Exception) {
                return LoginResult.Failure(ApiErrors.message(e))
            }
        }

        return localLogin(cleanUsername, password)
    }

    private suspend fun localLogin(username: String, password: String): LoginResult {
        val user = userDao.findByUsernameOrEmail(username)
            ?: return LoginResult.Failure(
                "Sin conexión y esta cuenta no está registrada en este dispositivo. " +
                    "Conéctate para iniciar sesión por primera vez."
            )
        val storedHash = user.localPasswordHash
            ?: return LoginResult.Failure(
                "Esta cuenta aún no tiene acceso sin conexión. Inicia sesión una vez con internet."
            )
        if (storedHash != Hash.localPassword(user.username, password)) {
            return LoginResult.Failure("Usuario o contraseña incorrectos")
        }
        val session = Session(
            userId = user.id,
            username = user.username,
            email = user.email,
            fullName = user.fullName,
            role = user.role,
            token = "",
            offline = true
        )
        sessionManager.save(session)
        userDao.updateCredential(user.id, storedHash, System.currentTimeMillis())
        return LoginResult.Success(session)
    }

    suspend fun exchangeCognitoIdToken(idToken: String): LoginResult {
        if (idToken.isBlank()) {
            return LoginResult.Failure("Token de Cognito inválido")
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            return LoginResult.Failure("Se requiere conexión para iniciar sesión con Cognito")
        }
        return try {
            val response = api.exchangeCognito(CognitoExchangeRequest(idToken = idToken))
            val remote = response.user
            val now = System.currentTimeMillis()
            val local = userDao.findByUsernameOrEmail(remote.username)
            if (local != null && local.id != remote.id) {
                userDao.deleteByUsername(local.username)
            }
            userDao.upsert(remote.toEntity(loginAt = now, existing = local))
            val session = Session(
                userId = remote.id,
                username = remote.username,
                email = remote.email,
                fullName = remote.fullName,
                role = remote.role,
                token = response.accessToken,
                offline = false
            )
            sessionManager.save(session)
            LoginResult.Success(session)
        } catch (e: HttpException) {
            if (e.code() == 403) {
                return LoginResult.IdentityNotLinked(idToken)
            }
            return LoginResult.Failure(
                CognitoAuthMessages.backendFailure(e.code()) ?: ApiErrors.message(e)
            )
        } catch (e: IOException) {
            LoginResult.Failure("Sin conexión con el servidor")
        } catch (e: Exception) {
            LoginResult.Failure(ApiErrors.message(e))
        }
    }

    suspend fun requestCognitoAccess(idToken: String): Result<CognitoLinkRequestDto> = runCatching {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IOException("Se requiere conexión para solicitar acceso")
        }
        api.requestCognitoLink(CognitoLinkAccessRequest(idToken))
    }.recoverCatching { throw RepositoryException(ApiErrors.message(it)) }

    suspend fun exchangeCognitoAccessToken(accessToken: String): LoginResult {
        if (accessToken.isBlank()) {
            return LoginResult.Failure("Token de Cognito inválido")
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            return LoginResult.Failure("Se requiere conexión para iniciar sesión con Cognito")
        }
        return try {
            val response = api.exchangeCognito(CognitoExchangeRequest(accessToken = accessToken))
            val remote = response.user
            val now = System.currentTimeMillis()
            val local = userDao.findByUsernameOrEmail(remote.username)
            if (local != null && local.id != remote.id) {
                userDao.deleteByUsername(local.username)
            }
            userDao.upsert(remote.toEntity(loginAt = now, existing = local))
            val session = Session(
                userId = remote.id,
                username = remote.username,
                email = remote.email,
                fullName = remote.fullName,
                role = remote.role,
                token = response.accessToken,
                offline = false
            )
            sessionManager.save(session)
            LoginResult.Success(session)
        } catch (e: HttpException) {
            return LoginResult.Failure(
                CognitoAuthMessages.backendFailure(e.code()) ?: ApiErrors.message(e)
            )
        } catch (e: IOException) {
            LoginResult.Failure("Sin conexión con el servidor")
        } catch (e: Exception) {
            LoginResult.Failure(ApiErrors.message(e))
        }
    }

    suspend fun logout() {
        sessionManager.logout()
    }
}

