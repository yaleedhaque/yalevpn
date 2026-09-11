package com.yaleed.vpnresearch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wireguard.android.backend.Tunnel
import com.yaleed.vpnresearch.data.VpnProfile
import com.yaleed.vpnresearch.data.VpnProfileStore
import com.yaleed.vpnresearch.shizuku.ShizukuController
import com.yaleed.vpnresearch.shizuku.ShizukuState
import com.yaleed.vpnresearch.ui.theme.Gold
import com.yaleed.vpnresearch.ui.theme.SlateDim
import com.yaleed.vpnresearch.ui.theme.Success
import com.yaleed.vpnresearch.vpn.VpnManager
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(onRequestVpnAuth: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Rounded.Shield, contentDescription = "VPN") },
                    label = { Text("VPN") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Rounded.Science, contentDescription = "Research Lab") },
                    label = { Text("Research Lab") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> VpnScreen(onRequestVpnAuth = onRequestVpnAuth)
                else -> ResearchScreen()
            }
        }
    }
}

@Composable
private fun VpnScreen(onRequestVpnAuth: () -> Unit) {
    val scope = rememberCoroutineScope()
    val status by VpnManager.status.collectAsState()
    val authRequired by VpnManager.authRequired.collectAsState()
    val profile by VpnProfileStore.profile.collectAsState()
    var generatedPublicKey by remember { mutableStateOf<String?>(null) }

    val connected = status.state == Tunnel.State.UP
    val busy = status.state == Tunnel.State.TOGGLE

    LaunchedEffect(authRequired) {
        if (authRequired) {
            onRequestVpnAuth()
            VpnManager.clearAuthRequest()
        }
    }
    LaunchedEffect(connected) {
        while (connected) {
            VpnManager.refreshStats()
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "YaleVPN",
            style = MaterialTheme.typography.headlineMedium,
            color = Gold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Real WireGuard tunnel · personal research",
            style = MaterialTheme.typography.bodySmall,
            color = SlateDim,
        )

        StatusCard(status.state, status.error, status.rxBytes, status.txBytes)

        if (authRequired) {
            Text(
                text = "VPN authorization required — the system dialog should be open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = {
                if (connected) {
                    VpnManager.disconnect()
                } else {
                    VpnManager.requestConnect(buildWgConfig(profile))
                }
            },
            enabled = !busy && (connected || !VpnProfileStore.requiredFieldsMissing()),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connected) MaterialTheme.colorScheme.error else Gold,
                contentColor = if (connected) Color.White else Color(0xFF1A1200),
            ),
        ) {
            Icon(Icons.Rounded.Power, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    busy -> "Working…"
                    connected -> "Disconnect"
                    else -> "Connect"
                },
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text("Interface", style = MaterialTheme.typography.titleSmall, color = SlateDim)
        ProfileField(
            label = "Private key",
            value = profile.privateKey,
            placeholder = "generated on device",
            onValueChange = { v -> VpnProfileStore.update { it.copy(privateKey = v) } },
            trailing = {
                IconButton(onClick = {
                    generatedPublicKey = VpnProfileStore.generateKeyPair()
                }) {
                    Icon(Icons.Rounded.Casino, contentDescription = "Generate keypair")
                }
            },
        )
        generatedPublicKey?.let { vk ->
            Text(
                text = "Public key:  $vk",
                style = MaterialTheme.typography.bodySmall,
                color = Gold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ProfileField(
            label = "Address",
            value = profile.address,
            placeholder = "10.0.0.2/32",
            onValueChange = { v -> VpnProfileStore.update { it.copy(address = v) } },
        )
        ProfileField(
            label = "DNS (optional)",
            value = profile.dns,
            placeholder = "1.1.1.1",
            onValueChange = { v -> VpnProfileStore.update { it.copy(dns = v) } },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text("Peer", style = MaterialTheme.typography.titleSmall, color = SlateDim)
        ProfileField(
            label = "Peer public key",
            value = profile.peerPublicKey,
            placeholder = "base64",
            onValueChange = { v -> VpnProfileStore.update { it.copy(peerPublicKey = v) } },
        )
        ProfileField(
            label = "Endpoint (optional)",
            value = profile.endpoint,
            placeholder = "vpn.example.com:51820",
            onValueChange = { v -> VpnProfileStore.update { it.copy(endpoint = v) } },
        )
        ProfileField(
            label = "Allowed IPs",
            value = profile.allowedIps,
            placeholder = "0.0.0.0/0, ::/0",
            onValueChange = { v -> VpnProfileStore.update { it.copy(allowedIps = v) } },
        )
        ProfileField(
            label = "Persistent keepalive (optional)",
            value = profile.persistentKeepalive,
            placeholder = "25",
            onValueChange = { v -> VpnProfileStore.update { it.copy(persistentKeepalive = v) } },
        )

        Spacer(Modifier.height(8.dp))
        SignatureFooter()
    }
}

@Composable
private fun StatusCard(
    state: Tunnel.State,
    error: String?,
    rx: Long,
    tx: Long,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (state == Tunnel.State.UP) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                when (state) {
                    Tunnel.State.UP -> Text("CONNECTED", color = Success, style = MaterialTheme.typography.titleMedium)
                    Tunnel.State.DOWN -> Text("DISCONNECTED", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Tunnel.State.TOGGLE -> Text("CONNECTING…", color = Gold)
                }
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state == Tunnel.State.UP) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "↓ ${formatBytes(rx)}",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "↑ ${formatBytes(tx)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = SlateDim) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = trailing,
    )
}

@Composable
private fun ResearchScreen() {
    val state by ShizukuController.state.collectAsState()
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    var current by remember { mutableStateOf<String?>(null) }
    var original by remember { mutableStateOf<String?>(null) }
    var busyRead by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Shizuku Research Lab",
            style = MaterialTheme.typography.headlineSmall,
            color = Gold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Capability experiments against the Android settings provider (uid shell/root).",
            style = MaterialTheme.typography.bodySmall,
            color = SlateDim,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (val s = state) {
                        is ShizukuState.Connected -> {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Success)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Connected · uid ${s.uid} (${if (s.uid == 0) "root" else "shell"})",
                                color = Success,
                            )
                        }
                        ShizukuState.Connecting -> {
                            CircularProgressIndicator(Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Starting service…")
                        }
                        ShizukuState.PermissionRequired -> {
                            Text("Permission required — approve the Shizuku dialog.", color = Gold)
                        }
                        ShizukuState.Unsupported -> {
                            Text("Shizuku not supported on this Android version.", color = MaterialTheme.colorScheme.error)
                        }
                        ShizukuState.NotStarted -> {
                            Text("Not connected. Install Shizuku and grant access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is ShizukuState.Error -> {
                            Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Current android_id", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            busyRead = true
                            scope.launch {
                                val v = ShizukuController.readAndroidId()
                                current = v
                                if (v.isNotBlank() && !v.startsWith("ERR") && v != "null") {
                                    if (original == null) original = v
                                }
                                log.add(0, "[read] ${v.ifBlank { "(blank)" }}")
                                busyRead = false
                            }
                        },
                        enabled = state is ShizukuState.Connected && !busyRead,
                    ) {
                        Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (busyRead) "Reading…" else "Read")
                    }
                }
                current?.let {
                    Text(it, fontFamily = FontFamily.Monospace, color = Gold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val spoof = randomHex16()
                            log.add(0, "[write] setting $spoof")
                            scope.launch {
                                val r = ShizukuController.writeAndroidId(spoof)
                                log.add(0, "[write] → $r")
                                val nl = ShizukuController.readAndroidId()
                                current = nl.ifBlank { "(blank)" }
                            }
                        },
                        enabled = state is ShizukuState.Connected,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                    ) {
                        Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Spoof (random)")
                    }
                    Button(
                        onClick = {
                            val o = original
                            log.add(0, "[restore] → $o")
                            scope.launch {
                                val r = if (o == null) "nothing to restore" else ShizukuController.writeAndroidId(o)
                                log.add(0, "[restore] → $r")
                                val nl = ShizukuController.readAndroidId()
                                current = nl.ifBlank { "(blank)" }
                            }
                        },
                        enabled = state is ShizukuState.Connected,
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
            }
        }

        Text("Capability matrix", style = MaterialTheme.typography.titleSmall, color = SlateDim)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CapabilityRow("android_id spoof", "per-app ad id re-derives", ok = true)
                CapabilityRow("mock location", "setting + mock flag", ok = true)
                CapabilityRow("IMEI / serial", "root-only", ok = false)
                CapabilityRow("Wi-Fi MAC override", "needs CAP_NET_ADMIN", ok = false)
            }
        }

        Text("Log", style = MaterialTheme.typography.titleSmall, color = SlateDim)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                if (log.isEmpty()) {
                    Text("No operations yet.", color = SlateDim, style = MaterialTheme.typography.bodySmall)
                } else {
                    log.take(20).forEach { line ->
                        Text(
                            text = line,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SignatureFooter()
    }
}

@Composable
private fun CapabilityRow(label: String, detail: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(if (ok) "✓" else "✗", color = if (ok) Success else MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = SlateDim)
        }
    }
}

@Composable
private fun SignatureFooter() {
    Text(
        text = "Developed by Md. Yaleed Haque",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
        color = SlateDim,
    )
}

private fun buildWgConfig(p: VpnProfile): String {
    val lines = mutableListOf<String>()
    lines += "[Interface]"
    lines += "PrivateKey = ${p.privateKey.trim()}"
    lines += "Address = ${p.address.trim()}"
    if (p.dns.isNotBlank()) lines += "DNS = ${p.dns.trim()}"
    lines += "[Peer]"
    lines += "PublicKey = ${p.peerPublicKey.trim()}"
    lines += "AllowedIPs = ${p.allowedIps.trim()}"
    if (p.endpoint.isNotBlank()) lines += "Endpoint = ${p.endpoint.trim()}"
    if (p.persistentKeepalive.isNotBlank()) lines += "PersistentKeepalive = ${p.persistentKeepalive.trim()}"
    return lines.joinToString("\n")
}

private fun randomHex16(): String {
    val r = SecureRandom()
    val chars = "0123456789abcdef"
    return buildString(16) { repeat(16) { append(chars[r.nextInt(16)]) } }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
    return String.format(Locale.US, "%.2f GB", mb / 1024.0)
}