package com.yaleed.vpnresearch.loc

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import com.yaleed.vpnresearch.root.RootController
import com.yaleed.vpnresearch.root.ShellResult
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mock GPS — injects a custom lat/lng into the system as a GPS test-provider fix. Any app
 * (or shell command) reading the `gps` provider sees the spoofed coordinate for as long as
 * the provider is active.
 *
 * Two prerequisites are handled here and by the UI:
 *  1. The mock-location appop must be ALLOW for this package. The privileged shell path
 *     (root preferred, Shizuku shell uid as fallback) grants it via `cmd appops` + the
 *     legacy secure flag — the same mechanism GPS-spoof tools use via adb, so no
 *     Developer-options dance is needed.
 *  2. `android.permission.ACCESS_FINE_LOCATION` must be granted (requested by the UI).
 *
 * The provider belongs to this app's own uid, so everything stays fully on-device. stop()
 * removes the test provider and the real location returns.
 */
object MockGpsController {

    const val APP_PKG = "com.yaleed.vpnresearch"

    private const val PROVIDER = LocationManager.GPS_PROVIDER

    @Volatile
    private var active = false

    @Volatile
    private var activeLat = 0.0

    @Volatile
    private var activeLng = 0.0

    @Volatile
    private var activeAccuracy = 0f

    // Self-sustaining refresh: while active, re-push the fix every few seconds so the mock
    // GPS stays the NEWEST source (fused picks freshest) and re-assert the spoof environment
    // so the real network location can't sneak a fresher fix in between.
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null
    private var refreshContext: Context? = null

    val isActive: Boolean get() = active

    fun current(): Triple<Double, Double, Float>? =
        if (active) Triple(activeLat, activeLng, activeAccuracy) else null

    /** Grants the mock-location appop + legacy secure flag via root/shell uid. */
    suspend fun grant(): ShellResult = withContext(Dispatchers.IO) {
        RootController.runPriv(
            "cmd appops set '$APP_PKG' android:mock_location allow; " +
                "settings put secure allow_mock_location 1; " +
                "echo appop=ok",
        )
    }

    /** Revokes the appop (the provider itself is removed separately by stop()). */
    suspend fun revoke(): ShellResult = withContext(Dispatchers.IO) {
        RootController.runPriv(
            "cmd appops set '$APP_PKG' android:mock_location deny; " +
                "settings put secure allow_mock_location 0; " +
                "echo appop=denied",
        )
    }

    /** Reads the current mock_location appop mode. */
    suspend fun checkGrant(): ShellResult = withContext(Dispatchers.IO) {
        RootController.runPriv(
            "cmd appops get '$APP_PKG' android:mock_location; " +
                "settings get secure allow_mock_location",
        )
    }

    /** Registers the GPS test provider and pushes a fix. Idempotent. */
    @SuppressLint("MissingPermission")
    suspend fun start(context: Context, lat: Double, lng: Double, accuracyMeters: Float): String =
        withContext(Dispatchers.Main) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                ensureProvider(lm)
                lm.setTestProviderEnabled(PROVIDER, true)
                pushFix(lm, lat, lng, accuracyMeters)
                activate(lat, lng, accuracyMeters)
                startAutoRefresh(context)
                status()
            } catch (e: SecurityException) {
                logDenied()
                "Denied: grant mock-location first (Root Lab → Mock GPS → Grant), or pick this app in Developer options."
            } catch (e: Exception) {
                "ERR ${e.message}"
            }
        }

    /** Re-pushes coordinates (or just re-asserts) onto the live provider. */
    @SuppressLint("MissingPermission")
    suspend fun update(context: Context, lat: Double, lng: Double, accuracyMeters: Float): String =
        withContext(Dispatchers.Main) {
            if (!active) return@withContext "Not running — tap Start first."
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                ensureProvider(lm)
                pushFix(lm, lat, lng, accuracyMeters)
                activate(lat, lng, accuracyMeters)
                status()
            } catch (e: SecurityException) {
                "Denied: mock-location permission revoked mid-run."
            } catch (e: Exception) {
                "ERR ${e.message}"
            }
        }

    /** Tears the test provider down — the mock fix disappears system-wide. */
    suspend fun stop(context: Context): String = withContext(Dispatchers.Main) {
        refreshJob?.cancel()
        refreshJob = null
        refreshContext = null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            lm.removeTestProvider(PROVIDER)
        } catch (_: Exception) {
            // Provider already gone — nothing to remove.
        }
        active = false
        "Mock GPS stopped — real location restored."
    }

    /** Re-pushes the active fix every few seconds + re-asserts the spoof environment. */
    private fun startAutoRefresh(context: Context) {
        refreshContext = context.applicationContext
        refreshJob = refreshScope.launch {
            var tick = 0
            while (isActive && active) {
                delay(4000)
                tick++
                val lm = refreshContext?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    ?: continue
                try {
                    pushFix(lm, activeLat, activeLng, activeAccuracy)
                } catch (_: Exception) {
                    // Provider may be mid-teardown on stop(); keep trying.
                }
                if (tick % 5 == 0) {
                    // Every ~20s keep the real network source starved so the mock stays newest.
                    val ctx = refreshContext ?: continue
                    try {
                        withContext(Dispatchers.IO) {
                            RootController.runPriv(
                                "settings put global wifi_scan_always_enabled 0; " +
                                    "settings put secure network_location_scanning_enabled 0; " +
                                    "settings put secure location_scanning_enabled 0; " +
                                    "settings put secure location_providers_allowed +gps; " +
                                    "appops set '$APP_PKG' android:mock_location allow; " +
                                    "echo spoof-env=ok",
                            )
                        }
                    } catch (_: Exception) {
                        // Non-fatal; mock fix keeps running regardless.
                    }
                }
            }
        }
    }

    /** Registers the provider, replacing any leftover ghost from a killed process. */
    private fun ensureProvider(lm: LocationManager) {
        try {
            lm.addTestProvider(
                PROVIDER,
                false, false, false, false,
                true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
            )
        } catch (e: IllegalArgumentException) {
            // A test provider with this name is already bound (leftover from a dead
            // process) — clear it and register fresh.
            try {
                lm.removeTestProvider(PROVIDER)
            } catch (_: Exception) {
                // Ignore.
            }
            lm.addTestProvider(
                PROVIDER,
                false, false, false, false,
                true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun pushFix(lm: LocationManager, lat: Double, lng: Double, accuracyMeters: Float) {
        val loc = Location(PROVIDER).apply {
            latitude = lat
            longitude = lng
            accuracy = if (accuracyMeters > 0) accuracyMeters else 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
            // Best-effort speed/bearing so strict apps keep accepting the fix.
            speed = 0f
            bearing = 0f
        }
        lm.setTestProviderLocation(PROVIDER, loc)
    }

    private fun activate(lat: Double, lng: Double, accuracyMeters: Float) {
        activeLat = lat
        activeLng = lng
        activeAccuracy = if (accuracyMeters > 0) accuracyMeters else 5f
        active = true
    }

    private fun status(): String =
        String.format(Locale.US, "Mock GPS active at %.5f, %.5f (±%.0f m)", activeLat, activeLng, activeAccuracy)

    private fun logDenied() {
        active = false
    }

    fun formatCurrent(): String =
        String.format(Locale.US, "%.5f, %.5f (±%.0f m)", activeLat, activeLng, activeAccuracy)
}