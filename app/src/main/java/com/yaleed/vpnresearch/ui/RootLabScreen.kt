package com.yaleed.vpnresearch.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wireguard.android.backend.Tunnel
import com.yaleed.vpnresearch.data.VpnProfileStore
import com.yaleed.vpnresearch.root.RootController
import com.yaleed.vpnresearch.root.RootState
import com.yaleed.vpnresearch.shizuku.ShizukuController
import com.yaleed.vpnresearch.shizuku.ShizukuState
import com.yaleed.vpnresearch.ui.theme.Gold
import com.yaleed.vpnresearch.ui.theme.SlateDim
import com.yaleed.vpnresearch.ui.theme.Success
import com.yaleed.vpnresearch.util.buildWgConfig
import com.yaleed.vpnresearch.vpn.VpnManager
import java.io.File
import java.net.InetAddress
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun RootLabScreen() {
    val rootState by RootController.state.collectAsState()
    val shizukuState by ShizukuController.state.collectAsState()
    val profile by VpnProfileStore.profile.collectAsState()
    val vpnState by VpnManager.status.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val tunnelUp = vpnState.state == Tunnel.State.UP

    var wgQuickAvailable by remember { mutableStateOf(false) }
    var resetpropAvailable by remember { mutableStateOf(false) }
    var ipv6Disabled by remember { mutableStateOf(false) }

    var banner by remember { mutableStateOf<String?>(null) }
    var bannerOk by remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    // Firewall toggles
    var fwKillSwitch by remember { mutableStateOf(false) }
    var fwBlockLan by remember { mutableStateOf(false) }
    var fwBlockPing by remember { mutableStateOf(false) }

    // Spoof state
    var currentAndroidId by remember { mutableStateOf<String?>(null) }
    var originalAndroidId by remember { mutableStateOf<String?>(null) }
    var currentModel by remember { mutableStateOf<String?>(null) }
    var spoofModelInput by remember { mutableStateOf("") }
    var deviceInfo by remember { mutableStateOf<String?>(null) }
    var consoleInput by remember { mutableStateOf("") }

    fun pushResult(tag: String, r: com.yaleed.vpnresearch.root.ShellResult) {
        log.add(0, "[$tag] exit=${r.exit}")
        r.text.take(800).split('\n').filter { it.isNotBlank() }.forEach { log.add(0, "  $it") }
    }

    LaunchedEffect(Unit) {
        RootController.init(context)
        RootController.probe()
        withTimeoutOrNull(20000) { RootController.state.first { it !is RootState.Checking } }
        wgQuickAvailable = detectTool("wg-quick") && detectTool("wg")
        resetpropAvailable = detectTool("resetprop")
        ipv6Disabled = RootController.runPriv(
            "cat /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null",
        ).text.trim() == "1"
    }

    val rootReady = rootState is RootState.Ready
    LaunchedEffect(rootReady) {
        if (!rootReady) return@LaunchedEffect
        wgQuickAvailable = detectTool("wg-quick") && detectTool("wg")
        resetpropAvailable = detectTool("resetprop")
        ipv6Disabled = RootController.runPriv(
            "cat /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null",
        ).text.trim() == "1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Root Lab",
            style = MaterialTheme.typography.headlineMedium,
            color = Gold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Privileged tunnelling, firewall & device spoofing — runs through su (Magisk / KernelSU) with a Shizuku fallback.",
            style = MaterialTheme.typography.bodySmall,
            color = SlateDim,
        )

        RootStatusCard(rootState, shizukuState, onRecheck = { RootController.probe() })

        banner?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (bannerOk) Success else MaterialTheme.colorScheme.error,
            )
        }

        KernelWgCard(
            wgAvailable = wgQuickAvailable,
            connected = tunnelUp,
            onUp = {
                banner = null
                if (VpnProfileStore.requiredFieldsMissing()) {
                    banner = "Complete the profile on the VPN tab first."; bannerOk = false; return@KernelWgCard
                }
                try {
                    val conf = File(context.cacheDir, "wg-kernel.conf")
                    conf.writeText(buildWgConfig(profile))
                    scope.launch {
                        val r = RootController.runPriv("wg-quick up '${conf.absolutePath}'")
                        pushResult("wg-quick up", r)
                        val w = RootController.runPriv("wg show 2>/dev/null; ip -brief link show tun0")
                        pushResult("wg status", w)
                        banner = if (r.ok) {
                            ipv6Disabled = RootController.runPriv(
                                "cat /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null",
                            ).text.trim() == "1"
                            "Kernel tunnel up via wg-quick (tun0)."
                        } else {
                            "wg-quick up failed — see log."
                        }
                        bannerOk = r.ok
                    }
                } catch (e: Exception) {
                    banner = "ERR ${e.message}"; bannerOk = false
                }
            },
            onDown = {
                banner = null
                scope.launch {
                    val conf = File(context.cacheDir, "wg-kernel.conf")
                    val r = RootController.runPriv("wg-quick down '${conf.absolutePath}' 2>/dev/null || echo not-up")
                    pushResult("wg-quick down", r)
                    banner = "wg-quick down."; bannerOk = r.ok
                }
            },
        )

        FirewallCard(
            kill = fwKillSwitch,
            lan = fwBlockLan,
            ping = fwBlockPing,
            onKill = { fwKillSwitch = it },
            onLan = { fwBlockLan = it },
            onPing = { fwBlockPing = it },
            tunnelUp = tunnelUp,
            onApply = {
                banner = null
                scope.launch {
                    val ep = resolveEndpoint(profile.endpoint)
                    val r = RootController.runPriv(
                        buildFirewallScript(fwKillSwitch, fwBlockLan, fwBlockPing, ep.ips, ep.port),
                    )
                    pushResult("firewall", r)
                    val active = Regex("rules_active=(\\d+)")
                        .find(r.text)?.groupValues?.get(1)?.toIntOrNull()
                    banner = when {
                        !r.ok -> "Firewall apply failed — see log."
                        active != null && active == 0 && (fwKillSwitch || fwBlockLan || fwBlockPing) ->
                            "Apply produced no rules — iptables errored? Check log."
                        fwKillSwitch && !tunnelUp ->
                            "Firewall applied — VPN is NOT connected, so internet is blocked until you connect or Disable firewall."
                        fwKillSwitch && profile.endpoint.isNotBlank() && ep.ips.isEmpty() ->
                            "Firewall applied, but the WG endpoint could not be resolved — the kill switch may block the handshake (use a numeric endpoint)."
                        else -> "Firewall rules applied."
                    }
                    bannerOk = r.ok
                }
            },
            onDisable = {
                banner = null
                scope.launch {
                    val r = RootController.runPriv(FIREWALL_RESET)
                    pushResult("firewall reset", r)
                    banner = "All YALEVPN firewall rules cleared."; bannerOk = r.ok
                }
            },
            onShow = {
                scope.launch {
                    val r = RootController.runPriv(
                        "iptables -w -S 2>/dev/null | grep YALEVPN; echo '--- v6 ---'; ip6tables -w -S 2>/dev/null | grep YALEVPN; echo '--- chain ---'; iptables -w -S YALEVPN 2>/dev/null; echo '--- v6 chain ---'; ip6tables -w -S YALEVPN 2>/dev/null",
                    )
                    pushResult("rules", r)
                }
            },
        )

        Ipv6Card(
            disabled = ipv6Disabled,
            onRefresh = {
                scope.launch {
                    ipv6Disabled = RootController.runPriv(
                        "cat /proc/sys/net/ipv6/conf/all/disable_ipv6 2>/dev/null",
                    ).text.trim() == "1"
                }
            },
            onDisable = {
                scope.launch {
                    val r = RootController.runPriv(
                        "for f in /proc/sys/net/ipv6/conf/*/disable_ipv6; do echo 1 > \$f; done; echo ipv6=disabled",
                    )
                    pushResult("ipv6 off", r)
                    ipv6Disabled = true
                }
            },
            onEnable = {
                scope.launch {
                    val r = RootController.runPriv(
                        "for f in /proc/sys/net/ipv6/conf/*/disable_ipv6; do echo 0 > \$f; done; echo ipv6=enabled",
                    )
                    pushResult("ipv6 on", r)
                    ipv6Disabled = false
                }
            },
        )

        SpoofCard(
            resetprop = resetpropAvailable,
            androidId = currentAndroidId,
            onReadAndroidId = {
                scope.launch {
                    val v = RootController.readAndroidId()
                    currentAndroidId = v
                    if (v.isNotBlank() && !v.startsWith("exit") && !v.startsWith("ERR") && v != "null") {
                        if (originalAndroidId == null) originalAndroidId = v
                    }
                    log.add(0, "[android_id] read → $v")
                }
            },
            onSpoofAndroidId = {
                scope.launch {
                    val spoof = randomHex16()
                    log.add(0, "[android_id] spoofing $spoof")
                    val r = RootController.writeAndroidId(spoof)
                    pushResult("android_id write", com.yaleed.vpnresearch.root.ShellResult(if (r == "ok") 0 else 1, r))
                    currentAndroidId = RootController.readAndroidId()
                }
            },
            onRestoreAndroidId = {
                scope.launch {
                    val o = originalAndroidId
                    log.add(0, "[android_id] restore → $o")
                    if (o != null) RootController.writeAndroidId(o)
                    currentAndroidId = RootController.readAndroidId()
                }
            },
            model = currentModel,
            modelInput = spoofModelInput,
            onModelInput = { spoofModelInput = it },
            onReadModel = {
                scope.launch {
                    val m = RootController.readProp("ro.product.model")
                    currentModel = m
                    log.add(0, "[model] ${m ?: "(none)"}")
                }
            },
            onSpoofModel = {
                banner = null
                val v = spoofModelInput.trim()
                if (v.isBlank()) {
                    banner = "Enter a spoofed model string."; bannerOk = false; return@SpoofCard
                }
                scope.launch {
                    val r = RootController.runPriv(
                        "resetprop ro.product.model \"$v\"; resetprop ro.product.marketname \"$v\"; echo done",
                    )
                    pushResult("resetprop", r)
                    currentModel = RootController.readProp("ro.product.model")
                    banner = if (r.ok) "Model spoofed to \"$v\" (live)." else "resetprop failed — see log."
                    bannerOk = r.ok
                }
            },
            onRestoreModel = {
                scope.launch {
                    val r = RootController.runPriv(
                        "resetprop --delete ro.product.model; resetprop --delete ro.product.marketname; resetprop --delete ro.product.device; resetprop --delete ro.product.manufacturer",
                    )
                    pushResult("resetprop restore", r)
                    currentModel = RootController.readProp("ro.product.model")
                    banner = "Model props reset to build defaults."; bannerOk = r.ok
                }
            },
        )

        DeviceInfoCard(info = deviceInfo, onFetch = {
            scope.launch {
                val r = RootController.runPriv(DEVICE_INFO_SCRIPT)
                pushResult("device info", r)
                deviceInfo = r.text
            }
        })

        RootConsoleCard(
            input = consoleInput,
            onInput = { consoleInput = it },
            onRun = {
                val cmd = consoleInput.trim()
                if (cmd.isBlank()) return@RootConsoleCard
                consoleInput = ""
                scope.launch {
                    val r = RootController.runPriv(cmd)
                    log.add(0, "> $cmd")
                    pushResult("console", r)
                }
            },
        )

        RootLogCard(log = log.toList())

        Spacer(Modifier.height(8.dp))
        SignatureFooter()
    }
}

@Composable
private fun RootStatusCard(
    root: RootState,
    shizuku: ShizukuState,
    onRecheck: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (val w = root) {
                is RootState.Ready -> {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Success)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ROOT · uid ${w.uid}", color = Success, style = MaterialTheme.typography.titleSmall)
                        Text(w.suPath, style = MaterialTheme.typography.bodySmall, color = SlateDim)
                    }
                }
                RootState.Checking -> {
                    CircularProgressIndicator(Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Probing su…")
                }
                RootState.Unavailable -> {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("No root detected", color = MaterialTheme.colorScheme.error)
                        Text(
                            if (shizuku is ShizukuState.Connected) {
                                "Shizuku available — privileged commands will fall back to shell uid."
                            } else "Install Magisk/KernelSU and grant this app root access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateDim,
                        )
                    }
                }
                is RootState.Error -> {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text("Root error: ${w.message}", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onRecheck) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Re-check root")
            }
        }
    }
}

@Composable
private fun KernelWgCard(
    wgAvailable: Boolean,
    connected: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Kernel WireGuard (wg-quick)", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (wgAvailable) "wg + wg-quick found on device."
                        else "wg / wg-quick not installed — install via Magisk module (e.g. MagiskWireguard) to enable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateDim,
                    )
                }
            }
            if (connected) {
                Text(
                    "The app VPN (userspace) is still up. Disconnect it on the VPN tab first, or the two tunnels will both claim tun0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onUp,
                    enabled = wgAvailable && !connected,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kernel up")
                }
                Button(onClick = onDown, enabled = wgAvailable) {
                    Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kernel down")
                }
            }
        }
    }
}

@Composable
private fun FirewallCard(
    kill: Boolean,
    lan: Boolean,
    ping: Boolean,
    onKill: (Boolean) -> Unit,
    onLan: (Boolean) -> Unit,
    onPing: (Boolean) -> Unit,
    tunnelUp: Boolean,
    onApply: () -> Unit,
    onDisable: () -> Unit,
    onShow: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Text("Firewall · iptables chain YALEVPN", style = MaterialTheme.typography.titleSmall)
            }
            ToggleRow("Kill switch", "Block everything outside tun0 (opens your WG endpoint UDP).", kill, onKill)
            ToggleRow("Block LAN", "Reject 10/8, 172.16/12, 192.168/16 + fc00::/7, fe80::/10.", lan, onLan)
            ToggleRow("Block ping", "Reject ICMP & ICMPv6 echo traffic.", ping, onPing)
            if (!tunnelUp) {
                Text(
                    "VPN is not connected — combining with the kill switch will cut all new network traffic until you connect a tunnel or Disable the firewall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (kill) Gold else SlateDim,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApply,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply rules")
                }
                Button(onClick = onDisable, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White)) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Disable firewall")
                }
                Button(onClick = onShow) {
                    Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Rules")
                }
            }
        }
    }
}

@Composable
private fun Ipv6Card(
    disabled: Boolean,
    onRefresh: () -> Unit,
    onDisable: () -> Unit,
    onEnable: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lan, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("IPv6", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (disabled) "IPv6 is DISABLED system-wide." else "IPv6 is on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (disabled) Success else SlateDim,
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDisable,
                    enabled = !disabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                ) { Text("Disable IPv6") }
                Button(onClick = onEnable, enabled = disabled) { Text("Enable IPv6") }
            }
        }
    }
}

@Composable
private fun SpoofCard(
    resetprop: Boolean,
    androidId: String?,
    onReadAndroidId: () -> Unit,
    onSpoofAndroidId: () -> Unit,
    onRestoreAndroidId: () -> Unit,
    model: String?,
    modelInput: String,
    onModelInput: (String) -> Unit,
    onReadModel: () -> Unit,
    onSpoofModel: () -> Unit,
    onRestoreModel: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Text("Spoofing", style = MaterialTheme.typography.titleSmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("android_id", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Button(onClick = onReadAndroidId) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Read")
                }
            }
            androidId?.let {
                Text(it, fontFamily = FontFamily.Monospace, color = Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSpoofAndroidId,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                ) {
                    Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Spoof (random)")
                }
                Button(onClick = onRestoreAndroidId) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Device model (resetprop)", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Button(onClick = onReadModel) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Read")
                }
            }
            if (!resetprop) {
                Text(
                    "resetprop not found — install Magisk to spoof build props.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            model?.let {
                Text("current: $it", fontFamily = FontFamily.Monospace, color = Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedTextField(
                value = modelInput,
                onValueChange = onModelInput,
                label = { Text("Spoofed model") },
                placeholder = { Text("e.g. Pixel 7 Pro", color = SlateDim) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSpoofModel,
                    enabled = resetprop,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Spoof model")
                }
                Button(onClick = onRestoreModel, enabled = resetprop) { Text("Restore (delete)") }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(info: String?, onFetch: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Text("Root-only device info", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = onFetch) { Text("Fetch") }
            }
            info?.let {
                Text(it, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun RootConsoleCard(
    input: String,
    onInput: (String) -> Unit,
    onRun: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Code, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(10.dp))
                Text("Root console", style = MaterialTheme.typography.titleSmall)
            }
            OutlinedTextField(
                value = input,
                onValueChange = onInput,
                label = { Text("shell command") },
                placeholder = { Text("id · ip addr · getprop …", color = SlateDim) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = onRun,
                enabled = input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF1A1200)),
            ) { Text("Run as root") }
        }
    }
}

@Composable
private fun RootLogCard(log: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp).fillMaxWidth()) {
            if (log.isEmpty()) {
                Text("No operations yet.", color = SlateDim, style = MaterialTheme.typography.bodySmall)
            } else {
                log.take(24).forEach { line ->
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
}

@Composable
private fun ToggleRow(label: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onChange)
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

private suspend fun detectTool(tool: String): Boolean =
    RootController.runPriv("command -v $tool >/dev/null 2>&1 && echo yes").text.trim() == "yes"

private const val FIREWALL_RESET =
    "CH=YALEVPN\n" +
        "for attempt in 1 2 3; do\n" +
        "  iptables -w -F \$CH 2>/dev/null\n" +
        "  iptables -w -D OUTPUT -j \$CH 2>/dev/null\n" +
        "  iptables -w -X \$CH 2>/dev/null\n" +
        "  ip6tables -w -F \$CH 2>/dev/null\n" +
        "  ip6tables -w -D OUTPUT -j \$CH 2>/dev/null\n" +
        "  ip6tables -w -X \$CH 2>/dev/null\n" +
        "  left=\$(iptables -w -S 2>/dev/null | grep -c YALEVPN)\n" +
        "  [ \"\$left\" = \"0\" ] && break\n" +
        "  sleep 1\n" +
        "done\n" +
        "echo firewall=cleared left=\$left"

private fun splitEndpoint(ep: String): Pair<String, String> {
    var s = ep.trim().removePrefix("udp://").removePrefix("tcp://")
    if (s.startsWith("[")) {
        val close = s.indexOf(']')
        if (close > 1 && s.length > close + 2 && s[close + 1] == ':') {
            return s.substring(1, close) to s.substring(close + 2)
        }
        return s.substring(1, close) to ""
    }
    val lastColon = s.lastIndexOf(':')
    if (lastColon > 0 && s.substring(lastColon + 1).all { it.isDigit() }) {
        return s.substring(0, lastColon) to s.substring(lastColon + 1)
    }
    return s to ""
}

private data class ResolvedEndpoint(val ips: List<String>, val port: String)

private suspend fun resolveEndpoint(endpoint: String): ResolvedEndpoint = withContext(Dispatchers.IO) {
    val (host, port) = splitEndpoint(endpoint)
    if (host.isBlank() || !host.any { it.isLetter() }) {
        // Numeric IP (v4 or v6) — no lookup needed.
        return@withContext ResolvedEndpoint(if (host.isBlank()) emptyList() else listOf(host), port)
    }
    val ips = try {
        InetAddress.getAllByName(host).mapNotNull { it.hostAddress }.distinct()
    } catch (e: Exception) {
        emptyList()
    }
    ResolvedEndpoint(ips, port)
}

private fun buildFirewallScript(kill: Boolean, lan: Boolean, ping: Boolean, endpointIps: List<String>, endpointPort: String): String {
    val sb = StringBuilder()
    sb.appendLine("CH=YALEVPN")
    sb.appendLine("iptables -w -N \$CH 2>/dev/null; ip6tables -w -N \$CH 2>/dev/null")
    sb.appendLine("iptables -w -F \$CH 2>/dev/null; ip6tables -w -F \$CH 2>/dev/null")
    sb.appendLine("iptables -w -I OUTPUT -j \$CH 2>/dev/null; ip6tables -w -I OUTPUT -j \$CH 2>/dev/null")
    if (kill) {
        sb.appendLine("iptables -w -A \$CH -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT")
        sb.appendLine("iptables -w -A \$CH -o tun0 -j ACCEPT")
        sb.appendLine("ip6tables -w -A \$CH -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT")
        sb.appendLine("ip6tables -w -A \$CH -o tun0 -j ACCEPT")
        if (endpointPort.isNotEmpty() && endpointPort.all { it.isDigit() }) {
            for (ip in endpointIps) {
                val table = if (ip.contains(':')) "ip6tables" else "iptables"
                sb.appendLine("$table -w -A \$CH -p udp -d $ip --dport $endpointPort -j ACCEPT")
            }
        }
        sb.appendLine("iptables -w -A \$CH -j REJECT")
        sb.appendLine("ip6tables -w -A \$CH -j REJECT")
    }
    if (lan) {
        sb.appendLine("iptables -w -A \$CH -d 10.0.0.0/8 -j REJECT")
        sb.appendLine("iptables -w -A \$CH -d 172.16.0.0/12 -j REJECT")
        sb.appendLine("iptables -w -A \$CH -d 192.168.0.0/16 -j REJECT")
        sb.appendLine("ip6tables -w -A \$CH -d fc00::/7 -j REJECT")
        sb.appendLine("ip6tables -w -A \$CH -d fe80::/10 -j REJECT")
    }
    if (ping) {
        sb.appendLine("iptables -w -A \$CH -p icmp -j REJECT")
        sb.appendLine("ip6tables -w -A \$CH -p icmpv6 -j REJECT")
    }
    sb.appendLine("echo rules_active=\$(iptables -w -S \$CH 2>/dev/null | wc -l)")
    return sb.toString()
}

private const val DEVICE_INFO_SCRIPT =
    "echo '--- device ---'\n" +
        "getprop ro.product.manufacturer | sed 's/^/manufacturer: /'\n" +
        "getprop ro.product.model | sed 's/^/model: /'\n" +
        "getprop ro.build.version.release | sed 's/^/android: /'\n" +
        "getprop ro.build.version.sdk | sed 's/^/sdk: /'\n" +
        "getprop ro.build.display.id | sed 's/^/display: /'\n" +
        "echo '--- identifiers ---'\n" +
        "echo -n 'serial: '; getprop ro.serialno\n" +
        "echo -n 'serial_boot: '; getprop ro.boot.serialno\n" +
        "echo -n 'android_id: '; settings get secure android_id 2>/dev/null\n" +
        "echo -n 'mac_wlan0: '; cat /sys/class/net/wlan0/address 2>/dev/null || echo unknown\n" +
        "echo -n 'mac_wlan1: '; cat /sys/class/net/wlan1/address 2>/dev/null || echo unknown\n" +
        "echo -n 'bt_addr: '; cat /sys/class/bluetooth/hci0/address 2>/dev/null || echo unknown\n" +
        "echo -n 'imei(getprop): '; getprop gsm.imei 2>/dev/null\n" +
        "echo -n 'imei0: '; getprop ro.ril.miui.imei0 2>/dev/null\n" +
        "echo -n 'imei(dumpsys): '; dumpsys iphonesubinfo 2>/dev/null | grep -im1 imei || echo n/a\n" +
        "echo -n 'sim_operator: '; getprop gsm.sim.operator.alpha\n" +
        "echo '--- runtime ---'\n" +
        "echo -n 'kernel: '; uname -r\n" +
        "echo -n 'su: '; command -v su || echo none\n" +
        "echo -n 'root_bin: '; command -v magisk || command -v ksud || echo none\n" +
        "echo -n 'selinux: '; getenforce"

private fun randomHex16(): String {
    val r = SecureRandom()
    val chars = "0123456789abcdef"
    return buildString(16) { repeat(16) { append(chars[r.nextInt(16)]) } }
}