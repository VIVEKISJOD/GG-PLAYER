package com.ggplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ggplayer.app.ui.GGApp
import com.ggplayer.app.ui.GGTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = DeviceInfo.isTv(this)
        setContent {
            GGTheme(isTv = tv) {
                GGApp()
            }
        }
    }
}
