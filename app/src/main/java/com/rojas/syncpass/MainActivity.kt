package com.rojas.syncpass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rojas.syncpass.ui.SyncPassAppRoot
import com.rojas.syncpass.ui.theme.SyncPassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SyncPassApp).container
        setContent {
            SyncPassTheme {
                SyncPassAppRoot(container)
            }
        }
    }
}
