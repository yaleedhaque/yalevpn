# YaleVPN

Real WireGuard VPN client for Android with Shizuku research lab **and a full root feature suite** for rooted devices.

## Features

- **WireGuard Tunnel** — Real wg-go backend via `com.wireguard.android:tunnel`, not a mock
- **Key Generation** — On-device Curve25519 keypair generation
- **Profile Editor** — Full interface/peer configuration (private key, address, DNS, peer key, endpoint, allowed IPs, keepalive) + wg-quick import (file / paste)
- **Shizuku Research Lab** — Android settings provider experiments (android_id spoof, mock location detection) via ADB/shell uid
- **Root Lab** — every feature that needs root, gated on `su` (Magisk / KernelSU / APatch):
  - **Kernel WireGuard** — `wg-quick up/down` for a fat, battery-efficient kernel-mode tunnel (enabled when `wg` + `wg-quick` are installed)
  - **Firewall (iptables/ip6tables)** — idempotent `YALEVPN` chain with kill switch (block all non-tun traffic, auto-opens the WG endpoint), block-LAN and block-ping presets; one-tap reset
  - **IPv6 control** — system-wide IPv6 disable/enable via `/proc/sys` (leak mitigation)
  - **Spoofing** — `android_id` read/random/restore, Magisk `resetprop` device-model spoof with `--delete`-based restore, and **Mock GPS** (custom lat/lng → GPS test provider with live Start/Update/Stop)
  - **Root-only device info** — serial, android_id, Wi-Fi/Bluetooth MAC, IMEI (best-effort), SIM operator, kernel, SELinux
  - **Root console** — run any shell snippet as root with a live log
- **Material 3 UI** — Jetpack Compose dark theme, Gold/Ember/Success accents, bottom nav (VPN / Research Lab / Root Lab)

Privileged commands prefer `su`; if no root is present they automatically fall back to Shizuku's shell uid so the lab stays usable for read-mostly experiments.

## Architecture

- **VpnManager** — Singleton managing GoBackend lifecycle, runs `setState()` off the main thread via coroutine IO dispatcher (avoids the 2s service-start timeout)
- **RootController** — su discovery (probes `su` in PATH + common locations), concurrent stdout/stderr capture, 25s timeout, state flow (`Checking / Ready(uid) / Unavailable / Error`); Shizuku-shell fallback
- **ShizukuController / ShizukuService** — Shizuku binder connection + `run()`/`shell()` passthrough to the settings provider
- **MockGpsController** — privileges-up the mock-location appop via root/Shizuku shell, then registers a GPS test provider that pushes custom lat/lng fixes (Start / Update / Stop)
- **VpnProfileStore** — DataStore-backed persistent profile + keypair generation
- **Jetpack Compose** — Single-activity, bottom nav (VPN + Research Lab + Root Lab tabs)

## Prerequisites

- Android 8.0+ (API 26)
- Root (Magisk/KernelSU/APatch) for the Root Lab — or Shizuku for read-mostly fallback
- WireGuard server to connect to
- `wg` + `wg-quick` binaries for kernel-mode tunnelling (e.g. MagiskWireguard module)

## Build

```bash
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug
```

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Enter your WireGuard config (private key, address, peer public key, endpoint) — or import a wg-quick profile
2. Tap **Connect** — authorizes VPN on first use
3. Research Lab tab — read/spoof android_id when Shizuku is connected
4. Root Lab tab — first open triggers a Magisk root-grant prompt; approve it, then use kernel mode, firewall presets, IPv6 toggle, spoofing (incl. Mock GPS: type coordinates → Start spoof → Grant location when asked) and the root console

## Safety

- The firewall is written to its own `YALEVPN` chain only — **Disable firewall** clears exactly those rules and nothing else. Don't combine the kill switch with a disconnected tunnel (it blocks new traffic by design — connect a tunnel or hit Disable).
- `resetprop` changes are live and revert to build defaults with **Restore (delete)**.
- Mock GPS only affects apps that read the standard GPS provider — Google's fused location provider is not mockable; mock apps that rely on it may still see the real location. **Stop** removes the provider so the real fix returns.
- IPv6 disable is system-wide and persists until re-enabled here.

## Permissions

- `INTERNET` — WireGuard tunnel traffic
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` — lets the app register the mock GPS provider (granted on demand in Root Lab → Mock GPS)
- VPN Service (declared by GoBackend in merged manifest) — system VPN authorization
- No extra root-permission declaration needed — `su` is invoked at runtime

## License

MIT — Developed by Md. Yaleed Haque