package com.ggplayer.app

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

object DeviceInfo {

    /** True on Fire TV, Android TV and any other remote-controlled television. */
    fun isTv(context: Context): Boolean {
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        val pm = context.packageManager
        return pm.hasSystemFeature("amazon.hardware.fire_tv") ||
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}
