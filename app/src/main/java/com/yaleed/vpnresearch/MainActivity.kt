package com.yaleed.vpnresearch

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.yaleed.vpnresearch.data.VpnProfileStore
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
        ShizukuController.start(application)

        setContent {
            YaleVPNTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(onRequestVpnAuth = ::launchVpnAuth)
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