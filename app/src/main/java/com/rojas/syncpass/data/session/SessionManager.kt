package com.rojas.syncpass.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore("syncpass_session")

data class Session(
    val userId: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: String,
    val token: String,
    val offline: Boolean
) {
    val isCoordinator: Boolean get() = role == ROLE_COORDINATOR
    val roleLabel: String get() = if (isCoordinator) "Docente · Coordinador" else "Estudiante · Monitor"

    companion object {
        const val ROLE_COORDINATOR = "DOCENTE_COORDINADOR"
        const val ROLE_STUDENT = "ESTUDIANTE_VOLUNTARIO_MONITOR"
    }
}

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

class SessionManager(context: Context, scope: CoroutineScope) {

    private val dataStore = context.applicationContext.sessionDataStore

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    private var tokenCache: String? = null

    @Volatile
    private var sessionCache: Session? = null

    val lastSync: Flow<Long?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_SYNC] }

    init {
        scope.launch { restore() }
    }

    fun currentToken(): String? = tokenCache
    fun currentSession(): Session? = sessionCache

    private suspend fun restore() {
        val prefs = try {
            dataStore.data.first()
        } catch (e: Exception) {
            emptyPreferences()
        }
        val id = prefs[Keys.USER_ID]
        val token = prefs[Keys.TOKEN].orEmpty()
        val username = prefs[Keys.USERNAME].orEmpty()
        if (id == null || username.isBlank()) {
            _state.value = SessionState.LoggedOut
            return
        }
        val session = Session(
            userId = id,
            username = username,
            email = prefs[Keys.EMAIL].orEmpty(),
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            role = prefs[Keys.ROLE].orEmpty(),
            token = token,
            offline = prefs[Keys.OFFLINE] ?: token.isBlank()
        )
        sessionCache = session
        tokenCache = token.takeIf { !session.offline }
        _state.value = SessionState.LoggedIn(session)
    }

    suspend fun save(session: Session) {
        sessionCache = session
        tokenCache = session.token.takeIf { !session.offline && it.isNotBlank() }
        dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = session.userId
            prefs[Keys.USERNAME] = session.username
            prefs[Keys.EMAIL] = session.email
            prefs[Keys.FULL_NAME] = session.fullName
            prefs[Keys.ROLE] = session.role
            prefs[Keys.TOKEN] = session.token
            prefs[Keys.OFFLINE] = session.offline
        }
        _state.value = SessionState.LoggedIn(session)
    }

    suspend fun markLastSync(timestamp: Long = System.currentTimeMillis()) {
        dataStore.edit { it[Keys.LAST_SYNC] = timestamp }
    }

    suspend fun logout() {
        sessionCache = null
        tokenCache = null
        dataStore.edit { it.clear() }
        _state.value = SessionState.LoggedOut
    }

    private object Keys {
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val FULL_NAME = stringPreferencesKey("full_name")
        val ROLE = stringPreferencesKey("role")
        val TOKEN = stringPreferencesKey("token")
        val OFFLINE = booleanPreferencesKey("offline")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }
}
