# YaleVPN

Real WireGuard VPN client for Android with integrated Shizuku research lab.

## Features

- **WireGuard Tunnel** — Real wg-go backend via `com.wireguard.android:tunnel`, not a mock
- **Key Generation** — On-device Curve25519 keypair generation
- **Profile Editor** — Full interface/peer configuration (private key, address, DNS, peer key, endpoint, allowed IPs, keepalive)
- **Shizuku Research Lab** — Android settings provider experiments (android_id spoof, mock location detection) via ADB/shell uid
- **Material 3 UI** — Jetpack Compose with dark theme, Gold/Ember/Success accents

## Architecture

- **VpnManager** — Singleton managing GoBackend lifecycle, runs `setState()` off the main thread via coroutine IO dispatcher (avoids 2s service-start timeout)
- **ShizukuController** — Manages Shizuku binder connection, reads/writes `settings put secure android_id`
- **VpnProfileStore** — DataStore-backed persistent profile + keypair generation
- **Jetpack Compose** — Single-activity, bottom nav (VPN + Research Lab tabs)

## Prerequisites

- Android 8.0+ (API 26)
- Shizuku running (for Research Lab features)
- WireGuard server to connect to

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

1. Enter your WireGuard config (private key, address, peer public key, endpoint)
2. Tap **Connect** — authorizes VPN on first use
3. Research Lab tab — read/spoof android_id when Shizuku is connected

## Permissions

- `INTERNET` — WireGuard tunnel traffic
- VPN Service (declared by GoBackend in merged manifest) — system VPN authorization

## License

MIT — Developed by Md. Yaleed Haque
