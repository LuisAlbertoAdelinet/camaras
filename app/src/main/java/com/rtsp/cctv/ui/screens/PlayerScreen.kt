@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.rtsp.cctv.ui.screens

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import kotlinx.coroutines.launch
import kotlin.OptIn

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

    val scope = rememberCoroutineScope()
    val isSaving = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        camera.value?.let { cam ->
            RtspPlayer(rtspUrl = cam.rtspUrl, modifier = Modifier.fillMaxSize())
        } ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        // Overlay Controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }

                Text(
                    text = camera.value?.name ?: "Cargando...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.width(48.dp)) // Empujar título al centro
            }

            Spacer(Modifier.weight(1f))

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { showSnapshot.value = true },
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Preview Snapshot")
                }

                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            isSaving.value = true
                            runCatching { api.saveSnapshot(cameraId) }
                                .onSuccess { 
                                    Toast.makeText(context, "Captura guardada en la nube", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { 
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                            isSaving.value = false
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (isSaving.value) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Guardar Snapshot")
                    }
                }

                FloatingActionButton(
                    onClick = { 
                        Toast.makeText(context, "Grabación: Próximamente", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Grabar")
                }
            }
        }
    }
}

@Composable
fun RtspPlayer(rtspUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    val scale = remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }

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

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale.value *= zoom
                    scale.value = scale.value.coerceIn(1f, 5f)
                    
                    // Solo permitir desplazamiento si hay zoom
                    if (scale.value > 1f) {
                        offsetX.value += pan.x
                        offsetY.value += pan.y
                    } else {
                        offsetX.value = 0f
                        offsetY.value = 0f
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    this.useController = false // No queremos controles nativos ahora
                    this.setShowNextButton(false)
                    this.setShowPreviousButton(false)
                    this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    this.setShutterBackgroundColor(android.graphics.Color.BLACK)
                    
                    // FORZAR TEXTURE_VIEW PARA ZOOM PODEROSO (Eliminado por error de compitacion)
                    // this.setVideoTextureView(android.view.TextureView(ctx))
                    
                    player.addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                            post { requestLayout() }
                        }
                    })
                }
            },
            update = { view ->
                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                view.requestLayout()
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value,
                    scaleY = scale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value
                )
        )
    }
}
