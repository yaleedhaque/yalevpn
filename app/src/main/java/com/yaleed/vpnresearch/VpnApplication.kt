package com.yaleed.vpnresearch

import android.app.Application
import com.yaleed.vpnresearch.vpn.VpnManager

class VpnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VpnManager.init(this)
    }
}