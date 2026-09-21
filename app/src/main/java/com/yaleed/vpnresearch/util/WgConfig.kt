package com.yaleed.vpnresearch.util

import com.yaleed.vpnresearch.data.VpnProfile

fun buildWgConfig(p: VpnProfile): String {
    val lines = mutableListOf<String>()
    lines += "[Interface]"
    lines += "PrivateKey = ${p.privateKey.trim()}"
    lines += "Address = ${p.address.trim()}"
    if (p.dns.isNotBlank()) lines += "DNS = ${p.dns.trim()}"
    if (p.mtu.isNotBlank()) lines += "MTU = ${p.mtu.trim()}"
    lines += "[Peer]"
    lines += "PublicKey = ${p.peerPublicKey.trim()}"
    lines += "AllowedIPs = ${p.allowedIps.trim()}"
    if (p.endpoint.isNotBlank()) lines += "Endpoint = ${p.endpoint.trim()}"
    if (p.persistentKeepalive.isNotBlank()) lines += "PersistentKeepalive = ${p.persistentKeepalive.trim()}"
    return lines.joinToString("\n")
}