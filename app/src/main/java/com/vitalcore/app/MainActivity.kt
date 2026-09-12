package com.vitalcore.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalcore.app.integrations.strava.StravaAuthManager
import com.vitalcore.app.ui.navigation.VitalCoreNavGraph
import com.vitalcore.app.ui.theme.VitalCoreTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity host. All screens are Composables reached through
 * [VitalCoreNavGraph]; this Activity wires up the theme, edge-to-edge
 * display, the Android-13+ notification permission, and receiving the
 * Strava OAuth redirect (see integrations/strava/StravaAuthManager).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val stravaViewModel: StravaRedirectViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleStravaRedirect(intent)
        setContent {
            VitalCoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VitalCoreNavGraph()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStravaRedirect(intent)
    }

    private fun handleStravaRedirect(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "vitalcore" && uri.host == "strava-callback") {
            stravaViewModel.handleRedirect(uri)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Tiny Activity-scoped ViewModel whose only job is forwarding the Strava OAuth redirect into StravaAuthManager. */
@HiltViewModel
class StravaRedirectViewModel @Inject constructor(
    private val stravaAuthManager: StravaAuthManager,
) : ViewModel() {
    var lastResultMessage by mutableStateOf<String?>(null)
        private set

    fun handleRedirect(uri: Uri) {
        viewModelScope.launch {
            val result = stravaAuthManager.handleRedirect(uri)
            lastResultMessage = result.toString()
        }
    }
}
