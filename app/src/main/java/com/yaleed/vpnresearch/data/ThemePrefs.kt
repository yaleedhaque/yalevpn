package com.yaleed.vpnresearch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.themeStore: DataStore<Preferences> by preferencesDataStore(name = "yel_theme")

/** Persists the manually-chosen theme ("dark" | "light"); absence = follow system. */
object ThemePrefs {

    private val K_MODE = stringPreferencesKey("mode")
    private val _mode = MutableStateFlow<String?>(null)
    val mode: StateFlow<String?> = _mode
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        scope.launch { _mode.value = context.themeStore.data.first()[K_MODE] }
    }

    fun set(context: Context, mode: String?) {
        scope.launch {
            context.themeStore.edit {
                if (mode == null) it.remove(K_MODE) else it[K_MODE] = mode
            }
            _mode.value = mode
        }
    }
}