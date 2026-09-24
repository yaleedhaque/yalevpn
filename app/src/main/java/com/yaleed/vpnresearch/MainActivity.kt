package com.yaleed.vpnresearch

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.yaleed.vpnresearch.data.ThemePrefs
import com.yaleed.vpnresearch.data.VpnProfileStore
import com.yaleed.vpnresearch.root.RootController
import com.yaleed.vpnresearch.shizuku.ShizukuController
import com.yaleed.vpnresearch.ui.MainScreen
import com.yaleed.vpnresearch.ui.theme.YaleVPNTheme
import com.yaleed.vpnresearch.vpn.VpnManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vpnAuthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            VpnManager.handleAuthResult(result.resultCode == RESULT_OK)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VpnManager.init(application)
        lifecycleScope.launch { VpnProfileStore.init(application) }
        ThemePrefs.init(application)
        ShizukuController.start(application)
        RootController.init(application)

        setContent {
            // null = follow system; otherwise explicit dark/light (persists via DataStore).
            var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val savedMode by ThemePrefs.mode.collectAsState()
            LaunchedEffect(savedMode) {
                darkOverride = when (savedMode) {
                    "dark" -> true
                    "light" -> false
                    else -> null
                }
            }
            val darkTheme = darkOverride ?: isSystemInDarkTheme()
            YaleVPNTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        darkTheme = darkTheme,
                        onToggleTheme = {
                            val next = !darkTheme
                            darkOverride = next
                            ThemePrefs.set(application, if (next) "dark" else "light")
                        },
                        onRequestVpnAuth = ::launchVpnAuth,
                    )
                }
            }
        }
    }

    private fun launchVpnAuth() {
        val intent = VpnService.prepare(this) ?: return
        vpnAuthLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuController.stop()
    }
}