package com.yaleed.vpnresearch.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import rikka.shizuku.Shizuku

sealed interface ShizukuState {
    data object Unsupported : ShizukuState
    data object NotStarted : ShizukuState
    data object PermissionRequired : ShizukuState
    data object Connecting : ShizukuState
    data class Connected(val uid: Int) : ShizukuState
    data class Error(val message: String) : ShizukuState
}

object ShizukuController {

    private const val REQ_CODE = 20260911

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotStarted)
    val state: StateFlow<ShizukuState> = _state

    @Volatile
    private var service: IShizukuService? = null

    private lateinit var appContext: Context
    private lateinit var serviceArgs: Shizuku.UserServiceArgs

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkSelfPermission()
    }

    private val permissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (requestCode != REQ_CODE) return
            if (grantResult == PackageManager.PERMISSION_GRANTED) bindService()
            else _state.value = ShizukuState.PermissionRequired
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IShizukuService.Stub.asInterface(binder)
            val uid = try {
                Shizuku.getUid()
            } catch (e: Exception) {
                -1
            }
            _state.value = ShizukuState.Connected(uid)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _state.value = ShizukuState.NotStarted
        }

        override fun onBindingDied(name: ComponentName?) {
            service = null
            _state.value = ShizukuState.NotStarted
        }
    }

    val isConnected: Boolean
        get() = service != null

    fun start(context: Context) {
        if (Shizuku.isPreV11()) {
            _state.value = ShizukuState.Unsupported
            return
        }
        appContext = context.applicationContext
        serviceArgs = Shizuku.UserServiceArgs(
            ComponentName(appContext, ShizukuService::class.java),
        )
            .processNameSuffix("yalevpn")
            .tag("yalevpn_research")
            .debuggable(false)
            .daemon(false)
            .version(1)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        if (Shizuku.pingBinder()) checkSelfPermission()
    }

    fun stop() {
        try {
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            if (::serviceArgs.isInitialized) Shizuku.unbindUserService(serviceArgs, connection, true)
        } catch (_: Exception) {
            // Already torn down.
        }
        service = null
        _state.value = ShizukuState.NotStarted
    }

    private fun checkSelfPermission() {
        if (_state.value is ShizukuState.Connected) return
        if (_state.value is ShizukuState.Connecting) return
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindService()
        } else {
            _state.value = ShizukuState.PermissionRequired
            Shizuku.requestPermission(REQ_CODE)
        }
    }

    private fun bindService() {
        if (service != null || _state.value is ShizukuState.Connecting) return
        _state.value = ShizukuState.Connecting
        try {
            Shizuku.bindUserService(serviceArgs, connection)
        } catch (e: Exception) {
            _state.value = ShizukuState.Error(e.message ?: "bind failed")
        }
    }

    suspend fun readAndroidId(): String {
        val s = service ?: return "no service"
        return try {
            s.readAndroidId()
        } catch (e: Exception) {
            "ERR ${e.message}"
        }
    }

    /** Runs a shell snippet inside the Shizuku (shell/root uid) process. */
    suspend fun shell(cmd: String): String {
        val s = service ?: return "exit=-1\nERR no service"
        return try {
            s.run(arrayOf("/system/bin/sh", "-c", cmd))
        } catch (e: Exception) {
            "exit=-1\nERR ${e.message}"
        }
    }

    suspend fun writeAndroidId(hex: String): String {
        val s = service ?: return "no service"
        return try {
            val code = s.writeAndroidId(hex)
            if (code == 0) "ok" else "write failed (exit $code)"
        } catch (e: Exception) {
            "ERR ${e.message}"
        }
    }
}