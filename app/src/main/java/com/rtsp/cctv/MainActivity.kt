package com.rtsp.cctv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.rtsp.cctv.ui.theme.RtspCctvTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RtspCctvTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CctvApp()
                }
            }
        }
    }
}
