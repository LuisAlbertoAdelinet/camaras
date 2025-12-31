@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.rtsp.cctv.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.rtsp.cctv.data.Camera
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.network.ApiClient

@Composable
fun PlayerScreen(cameraId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val camera = remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(cameraId) {
        runCatching { api.getCamera(cameraId) }
            .onSuccess { camera.value = it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            Button(onClick = { /* TODO: snapshot */ }) {
                Text("Snapshot")
            }
            Button(onClick = { /* TODO: grabar */ }) {
                Text("Grabar")
            }
        }
        camera.value?.let { cam ->
            RtspPlayer(rtspUrl = cam.rtspUrl, modifier = Modifier.fillMaxSize())
        } ?: run {
            Text(
                text = "Cargando...",
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun RtspPlayer(rtspUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(rtspUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
            }
        },
        modifier = modifier
    )
}
