@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.rtsp.cctv.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import javax.net.SocketFactory
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

    val showSnapshot = remember { mutableStateOf(false) }

    if (showSnapshot.value) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSnapshot.value = false }) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val requestBuilder = coil.request.ImageRequest.Builder(context)
                        .data(com.rtsp.cctv.network.snapshotUrl(cameraId))
                        .crossfade(true)
                    
                    tokenStore.getToken()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    
                    coil.compose.AsyncImage(
                        model = requestBuilder.build(),
                        contentDescription = "Snapshot",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            Button(onClick = { showSnapshot.value = true }) {
                Text("Snapshot")
            }
            Button(onClick = { 
                android.widget.Toast.makeText(context, "Grabación: Próximamente", android.widget.Toast.LENGTH_SHORT).show()
             }) {
                Text("Grabar")
            }
        }
        camera.value?.let { cam ->
            RtspPlayer(rtspUrl = cam.rtspUrl, modifier = Modifier.fillMaxSize())
        } ?: run {
            Text(
                text = "Cargando...",
                modifier = Modifier.padding(12.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun RtspPlayer(rtspUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember {
        val mediaSourceFactory = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setSocketFactory(SocketFactory.getDefault())

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(rtspUrl)
                setMediaItem(mediaItem)
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
                this.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        modifier = modifier
    )
}
