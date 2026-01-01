package com.rtsp.cctv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.ui.theme.RtspCctvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val tokenStore = remember { TokenStore(context) }
            val isDark = remember { mutableStateOf(tokenStore.isDarkMode()) }

            RtspCctvTheme(darkTheme = isDark.value) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CctvApp(
                        isDark = isDark.value,
                        onThemeChange = { enabled ->
                            tokenStore.setDarkMode(enabled)
                            isDark.value = enabled
                        }
                    )
                }
            }
        }
    }
}
