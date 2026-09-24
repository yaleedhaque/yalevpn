package com.yaleed.vpnresearch.root

import com.yaleed.vpnresearch.root.RootController.runPriv
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Anonymity/stealth hardening applied through root iptables + ip rules.
 *
 *  - DNS lock: force every UDP/TCP:53 to 1.1.1.1 so no resolver leak occurs (WireGuard is
 *    a pure IP tunnel; without this the OS can keep querying the DHCP/Wi-Fi resolver).
 *  - Leak block: drop mDNS/LLMNR/SSDP control traffic and hard-disable IPv6 egress
 *    (no v6 tunnel egress on WARP → v6 would leak the real location/IPv6).
 *  - Per-app bypass (split tunnel): mark chosen UIDs with fwmark 0x1 + `ip rule … fwmark 0x1
 *    table main pref 30000` so those apps route OUTSIDE the tunnel (real IP). Everything
 *    else keeps flowing through wg0.
 */
object AnonymityController {

    // Low-numbered fwmark reserved for our bypass (wg-quick typically uses 0xca6c).
    const val BYPASS_MARK = "0x1"
    const val BYPASS_PREF = "30000"
    const val DNS = "1.1.1.1"

    @Volatile var dnsLockOn = false; private set
    @Volatile var leakBlockOn = false; private set
    val bypassApps = ConcurrentHashMap<String, String>() // package -> uid (armed)

    suspend fun armDnsLock(enable: Boolean): ShellResult = withContext(Dispatchers.IO) {
        val rules = if (enable) {
            "iptables -w -t nat -C OUTPUT -p udp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null || " +
                "iptables -w -t nat -I OUTPUT 1 -p udp --dport 53 -j DNAT --to-destination $DNS:53; " +
                "iptables -w -t nat -C OUTPUT -p tcp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null || " +
                "iptables -w -t nat -I OUTPUT 1 -p tcp --dport 53 -j DNAT --to-destination $DNS:53; " +
                "echo dns-lock=on"
        } else {
            "iptables -w -t nat -D OUTPUT -p udp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null; " +
                "iptables -w -t nat -D OUTPUT -p tcp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null; " +
                "echo dns-lock=off"
        }
        runPriv(rules).also { if (it.exit == 0) dnsLockOn = enable }
    }

    suspend fun armLeakBlock(enable: Boolean): ShellResult = withContext(Dispatchers.IO) {
        val v6off =
            if (enable) "ip6tables -w -I OUTPUT 1 ! -o lo -j DROP"
            else "ip6tables -w -D OUTPUT ! -o lo -j DROP"
        val ports = listOf("5353" /* mDNS */, "5355" /* LLMNR */, "1900" /* SSDP */)
        val portRules = buildString {
            for (f in listOf("iptables", "ip6tables")) {
                for (p in ports) {
                    if (enable) {
                        append("$f -w -C OUTPUT -p udp --dport $p -j DROP 2>/dev/null || $f -w -I OUTPUT 1 -p udp --dport $p -j DROP; ")
                        append("$f -w -C OUTPUT -p tcp --dport $p -j DROP 2>/dev/null || $f -w -I OUTPUT 1 -p tcp --dport $p -j DROP; ")
                    } else {
                        append("$f -w -D OUTPUT -p udp --dport $p -j DROP 2>/dev/null; ")
                        append("$f -w -D OUTPUT -p tcp --dport $p -j DROP 2>/dev/null; ")
                    }
                }
            }
        }
        val script =
            if (enable) "$v6off; $portRules echo leak-block=on"
            else "$portRules $v6off; echo leak-block=off"
        runPriv(script).also { if (it.exit == 0) leakBlockOn = enable }
    }

    /** Split-tunnel: route the given packages OUTSIDE the WireGuard tunnel (real IP), rest stay in. */
    suspend fun armBypassPackages(packages: List<String>, enable: Boolean): ShellResult =
        withContext(Dispatchers.IO) {
            val resolved = packages.associateWith { resolveUid(it) }.filterValues { it > 0 }
            if (resolved.isEmpty() && enable) {
                return@withContext ShellResult(3, "no uids resolved for: $packages")
            }
            val sb = StringBuilder()
            for ((pkg, uid) in resolved) {
                val base =
                    "iptables -w -t mangle -m owner --uid-owner $uid -j MARK --set-mark $BYPASS_MARK"
                if (enable) {
                    sb.append("iptables -w -t mangle -C OUTPUT $base 2>/dev/null || iptables -w -t mangle -I OUTPUT 1 $base; ")
                    bypassApps[pkg] = "$uid"
                } else {
                    sb.append("iptables -w -t mangle -D OUTPUT $base 2>/dev/null; ")
                    bypassApps.remove(pkg)
                }
            }
            // Single low-pref rule routes marked traffic via the main table (bypasses wg0).
            val rule = "ip rule add pref $BYPASS_PREF fwmark $BYPASS_MARK table main"
            val ruleDel = "ip rule del pref $BYPASS_PREF fwmark $BYPASS_MARK table main"
            if (enable) {
                sb.append("$rule 2>/dev/null || true; echo bypass=on uids=${resolved.values.joinToString(",")}")
            } else {
                sb.append("$ruleDel 2>/dev/null || true; echo bypass-cleared")
            }
            runPriv(sb.toString())
        }

    suspend fun status(): ShellResult = withContext(Dispatchers.IO) {
        runPriv(
            "echo '== nat53 =='; iptables -w -t nat -S OUTPUT 2>/dev/null | grep -c 'dport 53'; " +
                "echo '== leakblock =='; iptables -w -S OUTPUT 2>/dev/null | grep -cE 'dport (5353|5355|1900)'; " +
                "echo '== v6off =='; ip6tables -w -S OUTPUT 2>/dev/null | grep -c 'DROP'; " +
                "echo '== bypass-rule =='; ip rule 2>/dev/null | grep -c 'pref $BYPASS_PREF'; " +
                "echo '== bypass-marks =='; iptables -w -t mangle -S OUTPUT 2>/dev/null | grep -c 'mark 0x1'",
        )
    }

    /** Tears down every stealth rule and restores normal routing. */
    suspend fun clearAll(): ShellResult = withContext(Dispatchers.IO) {
        val marks = runPriv("iptables -w -t mangle -S OUTPUT 2>/dev/null | grep '--uid-owner'")
            .raw.lineSequence().mapNotNull { line ->
                Regex("--uid-owner (\\d+)").find(line)?.groupValues?.get(1)
            }.distinct()
        val sb = StringBuilder()
        for (uid in marks) {
            sb.append("iptables -w -t mangle -D OUTPUT -m owner --uid-owner $uid -j MARK --set-mark $BYPASS_MARK 2>/dev/null; ")
        }
        sb.append("ip rule del pref $BYPASS_PREF fwmark $BYPASS_MARK table main 2>/dev/null || true; ")
        sb.append("iptables -w -t nat -D OUTPUT -p udp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null; ")
        sb.append("iptables -w -t nat -D OUTPUT -p tcp --dport 53 -j DNAT --to-destination $DNS:53 2>/dev/null; ")
        for (f in listOf("iptables", "ip6tables")) {
            for (p in listOf("5353", "5355", "1900")) {
                sb.append("$f -w -D OUTPUT -p udp --dport $p -j DROP 2>/dev/null; ")
                sb.append("$f -w -D OUTPUT -p tcp --dport $p -j DROP 2>/dev/null; ")
            }
        }
        sb.append("ip6tables -w -D OUTPUT ! -o lo -j DROP 2>/dev/null || true; echo cleared")
        val r = runPriv(sb.toString())
        dnsLockOn = false
        leakBlockOn = false
        bypassApps.clear()
        r
    }

    private suspend fun resolveUid(pkg: String): Int = withContext(Dispatchers.IO) {
        val r = runPriv("pm list packages -U 2>/dev/null")
        r.raw.lineSequence().mapNotNull { line ->
            val m = Regex("package:$pkg uid:([0-9]+)").find(line) ?: return@mapNotNull null
            m.groupValues[1].toInt()
        }.firstOrNull() ?: -1
    }

    suspend fun knownSystemUids(): ShellResult = withContext(Dispatchers.IO) {
        runPriv("pm list packages -U 2>/dev/null | grep -Ei 'maps|chrome|whatsapp|telegram|facebook|instagram|youtube|netflix|spotify|twitter|reddit|discord|firebase|gms' | head -40")
    }
}