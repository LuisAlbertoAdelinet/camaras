@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.rtsp.cctv.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rtsp.cctv.data.Camera
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.network.ApiClient
import com.rtsp.cctv.network.snapshotUrl
import javax.net.SocketFactory

// Note: OptIn is a compiler feature, file-level annotation doesn't strictly need the import here if fully qualified at line 1.

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
        Dialog(onDismissRequest = { showSnapshot.value = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val requestBuilder = ImageRequest.Builder(context)
                        .data(snapshotUrl(cameraId))
                        .crossfade(true)
                    
                    tokenStore.getToken()?.let { token ->
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    
                    AsyncImage(
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
                Toast.makeText(context, "Grabación: Próximamente", Toast.LENGTH_SHORT).show()
             }) {
                Text("Grabar")
            }
        }
        camera.value?.let { cam ->
            RtspPlayer(rtspUrl = cam.rtspUrl, modifier = Modifier.weight(1f).fillMaxWidth())
        } ?: run {
            Text(
                text = "Cargando...",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onBackground
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
                this.useController = true
                this.setShowNextButton(false)
                this.setShowPreviousButton(false)
            }
        },
        update = { view ->
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        },
        modifier = modifier
    )
}
