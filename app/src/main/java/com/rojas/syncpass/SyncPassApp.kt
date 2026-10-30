package com.rojas.syncpass

import android.app.Application
import com.rojas.syncpass.core.AppContainer
import com.rojas.syncpass.data.session.SessionState
import com.rojas.syncpass.data.sync.SyncScheduler
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class SyncPassApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedulePeriodic(this)

        container.applicationScope.launch {
            container.networkMonitor.online.filter { it }.collect {
                SyncScheduler.syncNow(this@SyncPassApp)
            }
        }
        container.applicationScope.launch {
            container.sessionManager.state.collect { state ->
                if (state is SessionState.LoggedIn && !state.session.offline) {
                    SyncScheduler.syncNow(this@SyncPassApp)
                }
            }
        }
    }
}
