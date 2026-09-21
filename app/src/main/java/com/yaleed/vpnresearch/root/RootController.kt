package com.yaleed.vpnresearch.root

import android.content.Context
import com.yaleed.vpnresearch.shizuku.ShizukuController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

sealed interface RootState {
    data object Checking : RootState
    data object Unavailable : RootState
    data class Ready(val uid: Int, val suPath: String) : RootState
    data class Error(val message: String) : RootState
}

data class ShellResult(val exit: Int, val raw: String) {
    val ok: Boolean get() = exit == 0
    val text: String get() = raw.trim()
}

/**
 * Privileged shell access for the Root Lab. Prefers the su binary (Magisk / KernelSU /
 * APatch); falls back to the Shizuku shell uid when su is not available so the lab still
 * works read-mostly on unrooted phones.
 */
object RootController {

    private const val SU_TIMEOUT_SECONDS = 25L
    private const val PROBE_TIMEOUT_SECONDS = 15L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<RootState>(RootState.Unavailable)
    val state: StateFlow<RootState> = _state

    @Volatile
    private var suPath: String? = null

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    fun cacheDir(): File = appContext.cacheDir

    /** Lazily probes for a working su binary. Safe to call repeatedly. */
    fun probe() {
        if (_state.value is RootState.Checking) return
        _state.value = RootState.Checking
        scope.launch {
            val path = findSu()
            if (path != null && verifyRoot(path)) {
                suPath = path
                val uid = runBlockingId(path)
                _state.value = if (uid == 0) RootState.Ready(0, path) else RootState.Unavailable
            } else {
                suPath = null
                _state.value = RootState.Unavailable
            }
        }
    }

    private suspend fun findSu(): String? = withContext(Dispatchers.IO) {
        val candidates = arrayOf(
            "su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/bin/.magisk/su",
            "/system/xbin/.magisk/su",
            "/system/bin/sush",
            "/vendor/bin/su",
        )
        for (c in candidates) {
            val r = exec(c, listOf("-c", "id"), PROBE_TIMEOUT_SECONDS) ?: continue
            if (r.exit == 0 && parseUid(r.raw) == 0) return@withContext c
        }
        null
    }

    private fun verifyRoot(path: String): Boolean {
        val r = exec(path, listOf("-c", "id"), PROBE_TIMEOUT_SECONDS) ?: return false
        return r.exit == 0 && parseUid(r.raw) == 0
    }

    private fun runBlockingId(path: String): Int {
        val r = exec(path, listOf("-c", "id"), PROBE_TIMEOUT_SECONDS) ?: return -1
        return parseUid(r.raw)
    }

    /**
     * Runs an arbitrary shell snippet with elevated privileges. Returns the combined
     * stdout/stderr as a ShellResult. Never throws on execution errors.
     */
    suspend fun runPriv(snippet: String): ShellResult = withContext(Dispatchers.IO) {
        val sp = suPath
        if (sp != null) {
            return@withContext exec(sp, listOf("-c", snippet), SU_TIMEOUT_SECONDS)
                ?: ShellResult(126, "root exec failed")
        }
        if (ShizukuController.isConnected) {
            val r = ShizukuController.shell(snippet)
            return@withContext ShellResult(if (r.startsWith("exit=0")) 0 else 1, r)
        }
        ShellResult(127, "Root unavailable — grant Magisk/KernelSU access (or start Shizuku).")
    }

    suspend fun readProp(name: String): String? {
        val r = runPriv("getprop \"$name\"")
        if (!r.ok) return null
        val v = r.text
        return if (v.isBlank() || v == "null") null else v
    }

    suspend fun readAndroidId(): String {
        val r = runPriv("settings get secure android_id")
        val v = r.text
        return if (r.ok && v.isNotBlank() && v != "null") v.trim() else r.text.take(160)
    }

    suspend fun writeAndroidId(hex: String): String {
        val r = runPriv("settings put secure android_id $hex")
        return if (r.ok) "ok" else "write failed: ${r.text.take(160)}"
    }

    private fun exec(bin: String, args: List<String>, timeoutSeconds: Long): ShellResult? {
        return try {
            val p = Runtime.getRuntime().exec((listOf(bin) + args).toTypedArray())
            val (out, err) = drain(p)
            val finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                ShellResult(-1, "timeout after ${timeoutSeconds}s")
            } else {
                val exit = p.exitValue()
                val combined = if (err.isBlank()) out else {
                    if (out.isNotBlank()) "$out\n[stderr]\n$err" else err
                }
                ShellResult(exit, combined)
            }
        } catch (e: Exception) {
            ShellResult(-1, "ERR ${e.message}")
        }
    }

    private fun drain(p: Process): Pair<String, String> {
        val out = StringBuilder()
        val err = StringBuilder()
        val t1 = Thread {
            p.inputStream.bufferedReader().forEachLine { out.append(it).append('\n') }
        }.apply { start() }
        val t2 = Thread {
            p.errorStream.bufferedReader().forEachLine { err.append(it).append('\n') }
        }.apply { start() }
        try {
            t1.join((SU_TIMEOUT_SECONDS + 2) * 1000)
            t2.join(2000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return out.toString() to err.toString()
    }

    private fun parseUid(raw: String): Int {
        val m = Regex("uid=(\\d+)").find(raw) ?: return -1
        return m.groupValues[1].toIntOrNull() ?: -1
    }
}