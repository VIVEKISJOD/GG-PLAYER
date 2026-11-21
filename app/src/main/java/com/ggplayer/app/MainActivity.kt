package com.ggplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ggplayer.app.data.AppSettings
import com.ggplayer.app.data.LayoutMode
import com.ggplayer.app.data.ProviderStore
import com.ggplayer.app.ui.GGApp
import com.ggplayer.app.ui.GGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        ProviderStore.init(this)
        val detectedTv = DeviceInfo.isTv(this)
        setContent {
            // Settings can force TV or phone layout, otherwise the device decides.
            val tv = when (AppSettings.layoutMode) {
                LayoutMode.Auto -> detectedTv
                LayoutMode.Tv -> true
                LayoutMode.Phone -> false
            }
            GGTheme(isTv = tv) {
                GGApp(deviceIsTv = detectedTv)
            }
        }
    }
}
