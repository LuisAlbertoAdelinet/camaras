package com.rtsp.cctv.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.network.ApiClient
import com.rtsp.cctv.network.ApiConfig
import com.rtsp.cctv.network.Snapshot
import com.rtsp.cctv.network.Recording
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotGalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val snapshots = remember { mutableStateOf<List<Snapshot>>(emptyList()) }
    val recordings = remember { mutableStateOf<List<Recording>>(emptyList()) }
    val isLoading = remember { mutableStateOf(false) }
    val selectedSnapshot = remember { mutableStateOf<Snapshot?>(null) }

    fun refresh() {
        scope.launch {
            isLoading.value = true
            if (selectedTab == 0) {
                runCatching { api.getSnapshots() }
                    .onSuccess { snapshots.value = it["items"] ?: emptyList() }
                    .onFailure { Toast.makeText(context, "Error cargar capturas", Toast.LENGTH_SHORT).show() }
            } else {
                runCatching { api.getRecordings() }
                    .onSuccess { recordings.value = it["items"] ?: emptyList() }
                    .onFailure { Toast.makeText(context, "Error cargar grabaciones", Toast.LENGTH_SHORT).show() }
            }
            isLoading.value = false
        }
    }

    LaunchedEffect(selectedTab) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Mis Capturas" else "Mis Grabaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Imágenes") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Videos") }
                )
            }

            if (isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (selectedTab == 0) {
                    if (snapshots.value.isEmpty()) {
                        EmptyState("No tienes capturas guardadas")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(snapshots.value, key = { it.id }) { snapshot ->
                                SnapshotCard(
                                    snapshot = snapshot,
                                    token = tokenStore.getToken() ?: "",
                                    onClick = { selectedSnapshot.value = snapshot },
                                    onDelete = {
                                        scope.launch {
                                            api.deleteSnapshot(snapshot.id)
                                            refresh()
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    if (recordings.value.isEmpty()) {
                        EmptyState("No tienes grabaciones guardadas")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(1), // List for videos
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(recordings.value, key = { it.id }) { recording ->
                                RecordingCard(
                                    recording = recording,
                                    onPlay = {
                                        val token = tokenStore.getToken() ?: ""
                                        val url = "${ApiConfig.BASE_URL}recordings/${recording.id}/video?token=$token"
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.setDataAndType(Uri.parse(url), "video/mp4")
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        
                                        try {
                                            context.startActivity(intent)
                                            Toast.makeText(context, "Abriendo reproductor...", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "No hay app para reproducir video", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = {
                                        scope.launch {
                                            api.deleteRecording(recording.id)
                                            refresh()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Snapshot Preview
        selectedSnapshot.value?.let { snapshot ->
            Dialog(
                onDismissRequest = { selectedSnapshot.value = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiConfig.BASE_URL}snapshots/${snapshot.id}/image?token=${tokenStore.getToken()}")
                                .build(),
                            contentDescription = "Full Screen Snapshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = { selectedSnapshot.value = null },
                            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
fun SnapshotCard(snapshot: Snapshot, token: String, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${ApiConfig.BASE_URL}snapshots/${snapshot.id}/image?token=$token")
                    .crossfade(true)
                    .build(),
                contentDescription = "Capture",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f), contentColor = Color.White)
            ) { Icon(Icons.Default.Delete, contentDescription = "Eliminar") }
        }
        Column(Modifier.padding(8.dp)) {
            Text(snapshot.camera_name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(formatDate(snapshot.created_at), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun RecordingCard(recording: Recording, onPlay: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.padding(8.dp).fillMaxWidth().clickable { onPlay() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(recording.camera_name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Duración: ${recording.duration}s", style = MaterialTheme.typography.bodyMedium)
                Text(formatDate(recording.created_at), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

fun formatDate(dateStr: String): String {
    return try {
        // Asumiendo formato SQL YYYY-MM-DD HH:MM:SS
        val input = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val date = input.parse(dateStr)
        output.format(date!!)
    } catch (e: Exception) {
        dateStr
    }
}
