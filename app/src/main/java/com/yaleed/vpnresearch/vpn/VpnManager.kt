package com.yaleed.vpnresearch.vpn

import android.content.Context
import android.net.VpnService
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.BackendException
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.BufferedReader
import java.io.StringReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VpnStatus(
    val state: Tunnel.State = Tunnel.State.DOWN,
    val error: String? = null,
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
)

object VpnManager {

    private const val TUNNEL_NAME = "yalevpn"

    private val _status = MutableStateFlow(VpnStatus())
    val status: StateFlow<VpnStatus> = _status

    private val _authRequired = MutableStateFlow(false)
    val authRequired: StateFlow<Boolean> = _authRequired

    private lateinit var applicationContext: Context
    private lateinit var backend: Backend

    private var pendingConfig: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME
        override fun onStateChange(newState: Tunnel.State) {
            _status.value = _status.value.copy(state = newState)
        }
    }

    fun init(context: Context) {
        if (::backend.isInitialized) return
        applicationContext = context.applicationContext
        backend = GoBackend(applicationContext)
    }

    /** Parses the wg-quick text and brings the tunnel up, requesting VPN authorization if needed. */
    fun requestConnect(configText: String) {
        pendingConfig = configText
        _status.value = VpnStatus(state = Tunnel.State.TOGGLE)
        if (VpnService.prepare(applicationContext) != null) {
            _authRequired.value = true
            return
        }
        startTunnel()
    }

    fun handleAuthResult(ok: Boolean) {
        if (ok) startTunnel()
        else _status.value = VpnStatus(error = "VPN authorization denied")
    }

    fun clearAuthRequest() {
        _authRequired.value = false
    }

    private fun startTunnel() {
        val text = pendingConfig
        if (text == null) {
            _status.value = VpnStatus(error = "No configuration")
            return
        }
        val config = try {
            Config.parse(BufferedReader(StringReader(text)))
        } catch (e: Exception) {
            _status.value = VpnStatus(error = "Invalid config: ${e.message}")
            return
        }
        _status.value = VpnStatus(state = Tunnel.State.TOGGLE)
        scope.launch {
            try {
                backend.setState(tunnel, Tunnel.State.UP, config)
                _status.value = VpnStatus(state = Tunnel.State.UP)
            } catch (e: BackendException) {
                if (e.reason == BackendException.Reason.VPN_NOT_AUTHORIZED) {
                    _authRequired.value = true
                    _status.value = VpnStatus()
                } else {
                    _status.value = VpnStatus(
                        state = Tunnel.State.DOWN,
                        error = "Backend error (${e.reason}): ${e.message}",
                    )
                }
            } catch (e: Exception) {
                _status.value = VpnStatus(state = Tunnel.State.DOWN, error = e.message ?: "Unknown error")
            }
        }
    }

    fun disconnect() {
        if (::backend.isInitialized) {
            scope.launch {
                try {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                } catch (_: Exception) {
                    // Best-effort teardown.
                }
            }
        }
        _status.value = VpnStatus()
        _authRequired.value = false
    }

    fun refreshStats() {
        if (!::backend.isInitialized || _status.value.state != Tunnel.State.UP) return
        scope.launch {
            try {
                val s = backend.getStatistics(tunnel)
                _status.value = _status.value.copy(rxBytes = s.totalRx(), txBytes = s.totalTx())
            } catch (_: Exception) {
                // Statistics can be transiently unavailable after a state change.
            }
        }
    }
}