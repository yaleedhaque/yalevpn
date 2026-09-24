package com.yaleed.vpnresearch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaleed.vpnresearch.ui.theme.brandAccent

private data class HelpSection(val title: String, val body: String, val accent: Color? = null)

@Composable
fun HelpScreen() {
    val sections = listOf(
        HelpSection(
            "What is YaleVPN?",
            "A WireGuard VPN for Android with a real tunnel engine (not a demo), a rooted-phone "
                + "power-lab, and no cloud. Three tabs:\n"
                + "• VPN — connect to any WireGuard server, including Cloudflare WARP (1.1.1.1).\n"
                + "• Research Lab — experiments against Android settings (needs Shizuku running).\n"
                + "• Root Lab — features that need root: firewall, IPv6 control, spoofing, kernel tunnel, root console.",
        ),
        HelpSection(
            "Quick start (60 seconds)",
            "1. VPN tab → enter or paste your WireGuard config, or tap Import paste/file.\n"
                + "2. Tap the gold Connect button. Android shows a VPN permission dialog once — tap OK/Allow.\n"
                + "3. Status card turns CONNECTED and shows ↓/↑ data.\n"
                + "4. Rooted phone? Open Root Lab and approve the Magisk prompt once. Now the whole power-lab unlocks.",
        ),
        HelpSection(
            "VPN tab — every field",
            "Private key — your device's secret key. Tap the dice icon to auto-generate a keypair (it shows your public key). Never share the private key.\n\n"
                + "Address — your tunnel IP, e.g. 10.0.0.2/32 (WARP: 172.16.0.2/32 + an IPv6). Give it a /30 or /32; WARP needs /32.\n\n"
                + "DNS (optional) — keep 1.1.1.1 (Cloudflare). Forces every DNS query through the tunnel, defeating ISP DNS blocking/poisoning. You can add several, comma-separated.\n\n"
                + "MTU (optional) — set 1280 (the official WARP value on every platform — best on mobile). If your connection is fast and every site loads, try 1320, 1400, 1420 for more speed. Too high = sites stop loading → lower it. Leave blank = 1420.\n\n"
                + "Peer public key — your server's (or WARP's) public key.\n"
                + "• WARP = bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=\n\n"
                + "Endpoint (optional) — server address:port. WARP = engage.cloudflareclient.com:2408 (also try 162.159.192.1:2408 or 162.159.193.1:2408).\n\n"
                + "Allowed IPs — what gets tunnelled. 0.0.0.0/0, ::/0 routes EVERYTHING through the VPN (standard). Narrow it to a server subnet if you only want specific traffic.\n\n"
                + "Persistent keepalive (optional) — 25 is perfect for mobile; keeps the tunnel alive when your ISP maps/NATs your address.",
        ),
        HelpSection(
            "Import a config",
            "Paste a whole wg-quick file into the box and tap Import paste, or push a file named import.conf to the app's external-files folder and tap Import file.\n"
                + "It reads [Interface] PrivateKey / Address / DNS / MTU and [Peer] PublicKey / Endpoint / AllowedIPs / PersistentKeepalive and fills the form.",
        ),
        HelpSection(
            "Research Lab tab",
            "Experiments against Android's settings provider. Requires the Shizuku app (non-root helper) OR the phone being rooted.\n"
                + "• Status card shows Connected (uid 0 = root, else shell).\n"
                + "• Current android_id + Read → shows it; Spoof (random) → rewrites it; Restore → puts the original back. This resets the tracking-ID apps see (ad IDs re-derive).\n"
                + "The capability matrix at the bottom shows what's on/off. IMEI/serial, MAC and kernel WireGuard unlock here only when root is detected.",
        ),
        HelpSection(
            "Root Lab — root status",
            "On first open the app probes for su (Magisk / KernelSU / APatch). Approve the 'Superuser request' once. The card turns ROOT · uid 0. Tap the ↻ to re-check.\n"
                + "No root? No problem — privileged commands fall back to Shizuku when it's connected.",
        ),
        HelpSection(
            "Root Lab — Kernel WireGuard",
            "The phone's normal tunnel ('VPN' tab) runs userspace WireGuard (wg-go) — it costs CPU, so it's slower than the kernel WireGuard your PC uses. That's the biggest reason WARP feels faster on the PC.\n\n"
                + "To fix: install a Magisk module that adds wg + wg-quick (e.g. MagiskWireguard), reboot, then here tap Kernel up. It applies the exact profile from the VPN tab (including MTU) through wg-quick inside the kernel. Kernel down stops it.\n"
                + "Files, the speed, and battery all improve. Note: disconnect the userspace 'VPN' tab tunnel first — both tunnels would want tun0.",
        ),
        HelpSection(
            "Root Lab — Firewall (iptables)",
            "Each toggle → Apply rules rebuilds a chain called YALEVPN. It's the only chain the app touches.\n"
                + "• Kill switch — everything except established traffic, tunnelled (tun0) traffic and your WG endpoint UDP is REJECTED. Use when connected, to guarantee no leaks if the tunnel drops.\n"
                + "• Block LAN — reject 10/8, 172.16/12, 192.168/16 (+ IPv6 private ranges).\n"
                + "• Block ping — reject ICMP/ICMPv6.\n"
                + "Disable firewall clears exactly the YALEVPN rules (it retries until clean). Rules shows the live rules in the log.\n"
                + "⚠ Danger: if you enable Kill switch while NOT connected, the phone loses new connections until you connect a tunnel or hit Disable firewall. That's the point of a kill switch.",
        ),
        HelpSection(
            "Root Lab — IPv6",
            "Disable IPv6 sets disable_ipv6=1 on every interface via /proc/sys. Some ISPs' IPv6 edge routes are slower or leak; forcing IPv4-only can be faster and avoids dual-stack leaks. Enable IPv6 turns it back. It's system-wide until you flip it back here.",
        ),
        HelpSection(
            "Root Lab — Spoofing",
            "android_id — Read (shows current), Spoof (random) rewrites it, Restore puts the original back. Same idea as Research Lab but goes through su when no Shizuku.\n"
                + "Device model — Magisk's resetprop rewrites build props live (ro.product.model etc.). If the resetprop row is missing, you're on KernelSU/APatch (no Magisk). Type a fake model (e.g. Pixel 7 Pro), tap Spoof model; Restore (delete) reverts to the real build values.",
        ),
        HelpSection(
            "Root Lab — Mock GPS",
            "Spoof your location to any coordinates — apps that read the GPS provider see the fake fix (Google's 'fused' provider is not spoofable, but gps-only apps and games are).\n"
                + "1. Type latitude / longitude / accuracy (Dhaka defaults; lat ±90, lng ±180).\n"
                + "2. Grant location (standard Android permission dialog).\n"
                + "3. Tap Start spoof. The app auto-grants the mock-location appop through root/Shizuku — no Developer-options selection needed on Android 8.1+.\n"
                + "4. Update re-pushes new coordinates live; Stop removes the provider and the real location returns. Grant appop / Check verifies the appop state.",
        ),
        HelpSection(
            "Root Lab — Device info",
            "Fetch shows root-only identifiers plain apps can't read: real serial numbers, android_id, Wi-Fi MACs (no location permission needed), IMEI (best-effort), SIM operator, kernel, SELinux. Handy for the research-lab matrix.",
        ),
        HelpSection(
            "Root Lab — Root console",
            "A tiny terminal. Type any shell command (id · ip addr · getprop ro.product.model · uptime …) and tap Run as root. Output and exit code land in the log. Great for verifying what the root features did.",
        ),
        HelpSection(
            "Speed & your ISP (throttle workaround)",
            "Why WARP/WireGuard is faster than your plain connection: your ISP throttles/shapes ordinary traffic (DNS filtering, DPI by SNI/flow), but WireGuard is an encrypted UDP tunnel — the ISP can only see opaque packets, so shaping/blocking by what you're doing stops.\n\n"
                + "To get PC-level speed on the phone, in order:\n"
                + "1. Kernel WireGuard (Root Lab) — removes userspace CPU cost. Biggest win on a rooted phone. (MagiskWireguard module first.)\n"
                + "2. MTU = 1280 (official WARP value). Then test upward if every site loads.\n"
                + "3. Endpoint: the anycast engage.cloudflareclient.com:2408 picks your nearest Cloudflare edge. If it feels slow, try direct IPs 162.159.192.1:2408 / 162.159.193.1:2408. Community tools (cf-scanner, WarpScout) find the best/least-congested POP.\n"
                + "4. DNS = 1.1.1.1 (kills ISP DNS throttling/poisoning).\n"
                + "5. IPv6 off (Root Lab) — some ISPs' v6 path is slower; v4-only can win.\n"
                + "6. Persistent keepalive 25 so your NAT entry never expires.\n\n"
                + "UDP-blocked or flow-shaped Wi-Fi? WARP's server also answers on alternate ports (the official client's fallback list). Try Endpoint = 162.159.192.1:1701, :4500, :500 or :2408. If only a VPN-over-TCP gets out, that needs a different tool — WARP WireGuard is UDP-only.",
        ),
        HelpSection(
            "Unblocking every site — the honest part",
            "WARP free exits through Cloudflare datacenter IPs. It defeats ISP-level throttling, DNS blocking and IP-geolocation-based over-blocking really well — but streaming services (Netflix etc.) and sites that explicitly block datacenter IPs can still refuse it. For those you need your own WireGuard server in the target region (or a reputable commercial VPN). Also: Cloudflare's free WARP terms forbid heavy torrenting.\n"
                + "A good combo: WARP for normal browsing + your own server for geo/Datacenter-blocked sites — both are just different Endpoint/Peer keys in this app.",
            accent = brandAccent(),
        ),
        HelpSection(
            "Troubleshooting",
            "• Tunnel won't connect → check Endpoint is host:port, Peer public key exact, Address valid (/32 for WARP), Allowed IPs non-empty.\n"
                + "• Connected but no internet → most sites failing = MTU too high. Drop it to 1280.\n"
                + "• Slow after tweaks → undo MTU to 1280, keep DNS 1.1.1.1, try a different Endpoint IP.\n"
                + "• Root Lab says 'No root detected' → install Magisk/KernelSU and re-open; or start Shizuku for fallback.\n"
                + "• Firewall cut your internet → tap Disable firewall, or connect a tunnel first.\n"
                + "• Resetprop button greyed → no Magisk on this device.",
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Help",
            style = MaterialTheme.typography.headlineMedium,
            color = brandAccent(),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = "Every feature, in plain language. Tap a section to expand it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val expanded = remember { mutableStateMapOf<Int, Boolean>().apply { put(0, true) } }
        sections.forEachIndexed { i, section ->
            val isExpanded = expanded[i] ?: false
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded[i] = !isExpanded }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = section.accent ?: MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse section" else "Expand section",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(visible = isExpanded) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp).fillMaxWidth()) {
                            Text(
                                section.body,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = "Developed by Md. Yaleed Haque",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}