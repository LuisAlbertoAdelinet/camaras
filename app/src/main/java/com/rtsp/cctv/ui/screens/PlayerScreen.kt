@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.rtsp.cctv.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.OptIn

// Note: OptIn is a compiler feature, file-level annotation doesn't strictly need the import here if fully qualified at line 1.

/**
 * Capture a View to Bitmap including all transformations
 */
fun captureViewToBitmap(view: View): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Save a Bitmap to the device's gallery (Pictures folder)
 * Returns true if successful
 */
suspend fun saveBitmapToGallery(
    context: android.content.Context,
    bitmap: Bitmap,
    cameraName: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "CAM_${cameraName}_$timestamp.jpg"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LisanCam")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let { imageUri ->
            resolver.openOutputStream(imageUri)?.use { outputStream: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            true
        } ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun PlayerScreen(cameraId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val camera = remember { mutableStateOf<Camera?>(null) }
    
    // State for forced fullscreen mode
    val isForceFullscreen = remember { mutableStateOf(false) }
    
    // Detect orientation
    val configuration = LocalConfiguration.current
    val isNaturalLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // Consider fullscreen if naturally landscape OR forced
    val isLandscape = isNaturalLandscape || isForceFullscreen.value

    // Handle forced fullscreen - lock to landscape
    DisposableEffect(isForceFullscreen.value) {
        val activity = context as? Activity
        if (activity != null) {
            if (isForceFullscreen.value) {
                // Force landscape orientation
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                // Restore to unspecified (follow device settings)
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        onDispose {
            // Restore orientation when leaving
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Handle fullscreen UI visibility for landscape mode
    DisposableEffect(isLandscape) {
        val activity = context as? Activity
        val window = activity?.window
        val decorView = window?.decorView
        
        if (activity != null && window != null && decorView != null) {
            if (isLandscape) {
                // Enter fullscreen: hide status bar and navigation bar using legacy API
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                // Exit fullscreen: show system bars
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        
        onDispose {
            // Restore system bars when leaving the screen
            val act = context as? Activity
            val win = act?.window
            val decor = win?.decorView
            if (decor != null && win != null) {
                @Suppress("DEPRECATION")
                decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

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
    val isRecording = remember { mutableStateOf(false) }
    
    // Reference to the player container for screenshot capture
    val playerContainerRef = remember { mutableStateOf<View?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        camera.value?.let { cam ->
            RtspPlayer(
                rtspUrl = cam.rtspUrl, 
                isLandscape = isLandscape, 
                modifier = Modifier.fillMaxSize(),
                onViewCreated = { view -> playerContainerRef.value = view }
            )
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

                // Fullscreen Toggle Button
                IconButton(
                    onClick = { 
                        isForceFullscreen.value = !isForceFullscreen.value
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isForceFullscreen.value) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isForceFullscreen.value) "Salir pantalla completa" else "Pantalla completa"
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom Actions - Simplified: Save and Record
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save Snapshot Button - Captures current view with zoom/transformations
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val containerView = playerContainerRef.value
                            if (containerView == null) {
                                Toast.makeText(context, "Esperando video...", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            
                            isSaving.value = true
                            val bitmap = captureViewToBitmap(containerView)
                            
                            if (bitmap != null) {
                                val cameraName = camera.value?.name?.replace(" ", "_") ?: "camera"
                                val saved = saveBitmapToGallery(context, bitmap, cameraName)
                                bitmap.recycle()
                                
                                if (saved) {
                                    Toast.makeText(context, "Captura guardada en Galería ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Error al capturar", Toast.LENGTH_SHORT).show()
                            }
                            isSaving.value = false
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (isSaving.value) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capturar")
                    }
                }

                // Record Button (Server-Side)
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            try {
                                if (isRecording.value) {
                                    // Stop recording
                                    api.stopRecording(cameraId)
                                    isRecording.value = false
                                    Toast.makeText(context, "Grabación detenida", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Start recording
                                    api.startRecording(cameraId)
                                    isRecording.value = true
                                    Toast.makeText(context, "Grabando... (Máx 60s)", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                isRecording.value = false
                            }
                        }
                    },
                    containerColor = if (isRecording.value) Color.Red else Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (isRecording.value) {
                        Icon(Icons.Default.Stop, contentDescription = "Detener")
                    } else {
                        Icon(Icons.Default.Videocam, contentDescription = "Grabar")
                    }
                }
            }
        }
    }
}

@Composable
fun RtspPlayer(
    rtspUrl: String, 
    isLandscape: Boolean = false, 
    modifier: Modifier = Modifier,
    onViewCreated: ((View) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Use primitive state holders for better performance (avoids boxing)
    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }

    val player = remember {
        val mediaSourceFactory = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true)
            .setSocketFactory(SocketFactory.getDefault())

        // Optimized load control for low-latency RTSP streaming
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,   // minBufferMs - lower = faster start
                5000,   // maxBufferMs - lower = less RAM
                500,    // bufferForPlaybackMs - start playback quickly
                1000    // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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
    
    // Determine resize mode based on orientation
    val resizeMode = if (isLandscape) {
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    } else {
        AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale.floatValue *= zoom
                    scale.floatValue = scale.floatValue.coerceIn(1f, 5f)
                    
                    // Solo permitir desplazamiento si hay zoom
                    if (scale.floatValue > 1f) {
                        offsetX.floatValue += pan.x
                        offsetY.floatValue += pan.y
                    } else {
                        offsetX.floatValue = 0f
                        offsetY.floatValue = 0f
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    this.useController = false
                    this.setShowNextButton(false)
                    this.setShowPreviousButton(false)
                    this.resizeMode = resizeMode
                    this.setShutterBackgroundColor(android.graphics.Color.BLACK)
                    
                    player.addListener(object : Player.Listener {
                        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                            post { requestLayout() }
                        }
                    })
                    
                    // Report view created
                    onViewCreated?.invoke(this)
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
                view.requestLayout()
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.floatValue,
                    scaleY = scale.floatValue,
                    translationX = offsetX.floatValue,
                    translationY = offsetY.floatValue
                )
        )
    }
}
