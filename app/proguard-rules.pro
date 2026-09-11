# WireGuard tunnel backend (wg-go native) — keep all so reflection/metadata survives.
-keep class com.wireguard.** { *; }
-dontwarn com.wireguard.**

# Shizuku binder + provider.
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**