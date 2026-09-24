package com.yaleed.vpnresearch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
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
import androidx.compose.ui.text.font.FontWeight
import com.yaleed.vpnresearch.ui.design.BrandMark
import com.yaleed.vpnresearch.ui.design.DsActionButton
import com.yaleed.vpnresearch.ui.design.DsButtonRow
import com.yaleed.vpnresearch.ui.design.DsButtonVariant
import com.yaleed.vpnresearch.ui.design.DsCard
import com.yaleed.vpnresearch.ui.design.DsCardLabel
import com.yaleed.vpnresearch.ui.design.DsPill
import com.yaleed.vpnresearch.ui.design.DsSectionHeader
import com.yaleed.vpnresearch.ui.design.DsStat
import com.yaleed.vpnresearch.ui.design.DsTopBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaleed.vpnresearch.ui.theme.brandAccent
import com.wireguard.android.backend.Tunnel
import com.yaleed.vpnresearch.data.VpnProfile
import com.yaleed.vpnresearch.data.VpnProfileStore
import com.yaleed.vpnresearch.root.RootController
import com.yaleed.vpnresearch.root.RootState
import com.yaleed.vpnresearch.shizuku.ShizukuController
import com.yaleed.vpnresearch.shizuku.ShizukuState
import com.yaleed.vpnresearch.ui.theme.Gold
import com.yaleed.vpnresearch.ui.theme.brandAccent
import com.yaleed.vpnresearch.util.buildWgConfig
import com.yaleed.vpnresearch.vpn.VpnManager
import java.io.File
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRequestVpnAuth: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        topBar = { DsTopBar(darkTheme = darkTheme, onToggleTheme = onToggleTheme) },
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
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Rounded.Terminal, contentDescription = "Root Lab") },
                    label = { Text("Root Lab") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Rounded.Help, contentDescription = "Help") },
                    label = { Text("Help") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> VpnScreen(onRequestVpnAuth = onRequestVpnAuth)
                1 -> ResearchScreen()
                2 -> RootLabScreen()
                else -> HelpScreen()
            }
        }
    }
}

@Composable
private fun VpnScreen(onRequestVpnAuth: () -> Unit) {
    val status by VpnManager.status.collectAsState()
    val authRequired by VpnManager.authRequired.collectAsState()
    val profile by VpnProfileStore.profile.collectAsState()
    var generatedPublicKey by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var importMsg by remember { mutableStateOf<String?>(null) }
    var importOk by remember { mutableStateOf(false) }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 34.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "YaleVPN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = brandAccent(),
                )
                Text(
                    text = "Real WireGuard tunnel · personal research",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(2.dp))

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
                contentColor = if (connected) MaterialTheme.colorScheme.onError else Color(0xFF201500),
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

        DsSectionHeader("Interface")
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
                color = brandAccent(),
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Advanced settings (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = brandAccent(),
            )
            Spacer(Modifier.weight(1f))
            Icon(
                if (showAdvanced) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (showAdvanced) "Hide advanced settings" else "Show advanced settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = showAdvanced) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileField(
                    label = "DNS (optional)",
                    value = profile.dns,
                    placeholder = "1.1.1.1",
                    onValueChange = { v -> VpnProfileStore.update { it.copy(dns = v) } },
                )
                ProfileField(
                    label = "MTU (optional)",
                    value = profile.mtu,
                    placeholder = "1280 (WARP default) · 1420 fast",
                    onValueChange = { v -> VpnProfileStore.update { it.copy(mtu = v) } },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        DsSectionHeader("Peer")
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

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Text("Import config", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Paste a wg-quick config, or push import.conf to the app's external files dir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("wg-quick config") },
            placeholder = { Text("[Interface]\nPrivateKey = …\nAddress = 10.0.0.2/32\nDNS = 1.1.1.1\n\n[Peer]\nPublicKey = …\nEndpoint = engage.cloudflareclient.com:2408\nAllowedIPs = 0.0.0.0/0, ::/0\nPersistentKeepalive = 25", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            maxLines = 8,
        )
        DsButtonRow {
            DsActionButton(
                text = "Import file",
                onClick = {
                    val file = File(context.getExternalFilesDir(null), "import.conf")
                    if (!file.exists()) {
                        importMsg = "No import.conf found in app external files dir"
                        importOk = false
                    } else {
                        val r = importConfigText(file.readText())
                        importMsg = r.message
                        importOk = r.ok
                    }
                },
                modifier = Modifier.weight(1f), icon = Icons.Rounded.FolderOpen,
                variant = DsButtonVariant.Primary,
            )
            DsActionButton(
                text = "Import paste",
                onClick = {
                    val r = importConfigText(importText)
                    importMsg = r.message
                    importOk = r.ok
                },
                modifier = Modifier.weight(1f), icon = Icons.Rounded.ContentPaste,
                enabled = importText.isNotBlank(), variant = DsButtonVariant.Outlined,
            )
        }
        importMsg?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (importOk) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
            )
        }

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
    DsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (state == Tunnel.State.UP) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    when (state) {
                        Tunnel.State.UP -> "Secured"
                        Tunnel.State.DOWN -> "Unsecured"
                        Tunnel.State.TOGGLE -> "Connecting"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (state) {
                        Tunnel.State.UP -> MaterialTheme.colorScheme.secondary
                        Tunnel.State.DOWN -> MaterialTheme.colorScheme.onSurface
                        Tunnel.State.TOGGLE -> brandAccent()
                    },
                )
            }
            DsPill(
                text = when (state) {
                    Tunnel.State.UP -> "Tunnel up"
                    Tunnel.State.DOWN -> "Idle"
                    Tunnel.State.TOGGLE -> "Working…"
                },
                tint = when (state) {
                    Tunnel.State.UP -> MaterialTheme.colorScheme.secondary
                    Tunnel.State.DOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                    Tunnel.State.TOGGLE -> brandAccent()
                },
            )
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state == Tunnel.State.UP) {
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                DsStat("▼ received", formatBytes(rx), modifier = Modifier.weight(1f), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(16.dp))
                DsStat("▲ sent", formatBytes(tx), modifier = Modifier.weight(1f), tint = brandAccent())
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
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = trailing,
    )
}

@Composable
private fun ResearchScreen() {
    val state by ShizukuController.state.collectAsState()
    val rootStateLocal by RootController.state.collectAsState()
    val scope = rememberCoroutineScope()
    val researchContext = LocalContext.current
    val log = remember { mutableStateListOf<String>() }
    var current by remember { mutableStateOf<String?>(null) }
    var original by remember { mutableStateOf<String?>(null) }
    var busyRead by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RootController.init(researchContext)
        if (rootStateLocal !is RootState.Ready) RootController.probe()
    }

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
            color = brandAccent(),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Capability experiments against the Android settings provider (uid shell/root).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (val s = state) {
                        is ShizukuState.Connected -> {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Connected · uid ${s.uid} (${if (s.uid == 0) "root" else "shell"})",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        ShizukuState.Connecting -> {
                            CircularProgressIndicator(Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Starting service…")
                        }
                        ShizukuState.PermissionRequired -> {
                            Text("Permission required — approve the Shizuku dialog.", color = brandAccent())
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
                    DsActionButton(
                        text = if (busyRead) "Reading…" else "Read",
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
                        icon = Icons.Rounded.Visibility,
                        enabled = state is ShizukuState.Connected && !busyRead,
                        variant = DsButtonVariant.Outlined,
                    )
                }
                current?.let {
                    Text(it, fontFamily = FontFamily.Monospace, color = brandAccent())
                }
                DsButtonRow {
                    DsActionButton(
                        text = "Spoof (random)",
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
                        modifier = Modifier.weight(1f), icon = Icons.Rounded.Casino,
                        enabled = state is ShizukuState.Connected,
                        variant = DsButtonVariant.Primary,
                    )
                    DsActionButton(
                        text = "Restore",
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
                        modifier = Modifier.weight(1f), icon = Icons.Rounded.Save,
                        enabled = state is ShizukuState.Connected,
                        variant = DsButtonVariant.Outlined,
                    )
                }
            }
        }

        Text("Capability matrix", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CapabilityRow("android_id spoof", "settings provider (shell/root)", ok = true)
                CapabilityRow(
                    "mock location",
                    "custom lat/lng → GPS test provider (Root Lab tab)",
                    ok = rootStateLocal is RootState.Ready || state is ShizukuState.Connected,
                )
                CapabilityRow("IMEI / serial", if (rootStateLocal is RootState.Ready) "root read (Root Lab → Device info)" else "root-only", ok = rootStateLocal is RootState.Ready)
                CapabilityRow("Wi-Fi MAC override", if (rootStateLocal is RootState.Ready) "root read; spoof device-dependent" else "needs CAP_NET_ADMIN", ok = rootStateLocal is RootState.Ready)
                CapabilityRow("kernel WireGuard", if (rootStateLocal is RootState.Ready) "wg-quick mode (Root Lab)" else "needs root + wg-quick", ok = rootStateLocal is RootState.Ready)
            }
        }

        Text("Log", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(12.dp).fillMaxWidth()) {
                if (log.isEmpty()) {
                    Text("No operations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
        Text(if (ok) "✓" else "✗", color = if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class ImportResult(val ok: Boolean, val message: String)

private fun importConfigText(text: String): ImportResult {
    var section = ""
    var priv: String? = null
    var addr: String? = null
    var dns: String? = null
    var mtu: String? = null
    var peerPub: String? = null
    var endpoint: String? = null
    var allowed: String? = null
    var ka: String? = null
    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) continue
        if (line.startsWith("[") && line.endsWith("]")) { section = line; continue }
        val idx = line.indexOf('=')
        if (idx <= 0) continue
        val key = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim()
        if (value.isEmpty()) continue
        when (section) {
            "[Interface]" -> when (key) {
                "PrivateKey" -> priv = value
                "Address" -> addr = value
                "DNS" -> dns = value
                "MTU" -> mtu = value
            }
            "[Peer]" -> when (key) {
                "PublicKey" -> peerPub = value
                "Endpoint" -> endpoint = value
                "AllowedIPs" -> allowed = value
                "PersistentKeepalive" -> ka = value
            }
        }
    }
    if (priv == null || addr == null || peerPub == null) {
        return ImportResult(false, "Missing required fields (PrivateKey / Address / Peer PublicKey)")
    }
    VpnProfileStore.update {
        it.copy(
            privateKey = priv,
            address = addr,
            dns = dns ?: "",
            mtu = mtu ?: "",
            peerPublicKey = peerPub,
            endpoint = endpoint ?: "",
            allowedIps = allowed ?: "0.0.0.0/0, ::/0",
            persistentKeepalive = ka ?: "",
        )
    }
    return ImportResult(true, "Imported — profile fields populated")
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