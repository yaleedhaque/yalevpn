package com.yaleed.vpnresearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vpn_profile")

data class VpnProfile(
    val privateKey: String = "",
    val address: String = "",
    val dns: String = "",
    val peerPublicKey: String = "",
    val endpoint: String = "",
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: String = "",
)

object VpnProfileStore {

    private val K_PRIVATE = stringPreferencesKey("private_key")
    private val K_ADDRESS = stringPreferencesKey("address")
    private val K_DNS = stringPreferencesKey("dns")
    private val K_PEER_PUB = stringPreferencesKey("peer_public_key")
    private val K_ENDPOINT = stringPreferencesKey("endpoint")
    private val K_ALLOWED = stringPreferencesKey("allowed_ips")
    private val K_KA = stringPreferencesKey("persistent_keepalive")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profile = MutableStateFlow(VpnProfile())
    val profile: StateFlow<VpnProfile> = _profile

    private var store: DataStore<Preferences>? = null

    suspend fun init(context: Context) {
        store = context.dataStore
        val p = context.dataStore.data.first()
        _profile.value = VpnProfile(
            privateKey = p[K_PRIVATE] ?: "",
            address = p[K_ADDRESS] ?: "",
            dns = p[K_DNS] ?: "",
            peerPublicKey = p[K_PEER_PUB] ?: "",
            endpoint = p[K_ENDPOINT] ?: "",
            allowedIps = p[K_ALLOWED] ?: "0.0.0.0/0, ::/0",
            persistentKeepalive = p[K_KA] ?: "",
        )
    }

    fun update(transform: (VpnProfile) -> VpnProfile) {
        val next = transform(_profile.value)
        _profile.value = next
        persist(next)
    }

    /** Generates a fresh WireGuard keypair; returns the base64 public key. */
    fun generateKeyPair(): String {
        val pair = KeyPair()
        val priv = pair.privateKey.toBase64()
        val pub = pair.publicKey.toBase64()
        update { it.copy(privateKey = priv) }
        return pub
    }

    fun requiredFieldsMissing(): Boolean {
        val p = _profile.value
        return p.privateKey.isBlank() || p.address.isBlank() || p.peerPublicKey.isBlank() || p.allowedIps.isBlank()
    }

    private fun persist(p: VpnProfile) {
        val ds = store ?: return
        scope.launch {
            ds.edit { prefs ->
                prefs[K_PRIVATE] = p.privateKey
                prefs[K_ADDRESS] = p.address
                prefs[K_DNS] = p.dns
                prefs[K_PEER_PUB] = p.peerPublicKey
                prefs[K_ENDPOINT] = p.endpoint
                prefs[K_ALLOWED] = p.allowedIps
                prefs[K_KA] = p.persistentKeepalive
            }
        }
    }
}