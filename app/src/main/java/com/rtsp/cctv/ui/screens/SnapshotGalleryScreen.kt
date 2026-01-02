package com.rtsp.cctv.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapshotGalleryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val scope = rememberCoroutineScope()
    
    val snapshots = remember { mutableStateOf<List<Snapshot>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val selectedSnapshot = remember { mutableStateOf<Snapshot?>(null) }

    fun refresh() {
        scope.launch {
            isLoading.value = true
            runCatching { api.getSnapshots() }
                .onSuccess { snapshots.value = it["items"] ?: emptyList() }
                .onFailure { 
                    Toast.makeText(context, "Error al cargar capturas", Toast.LENGTH_SHORT).show()
                }
            isLoading.value = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Capturas") },
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
        if (isLoading.value) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (snapshots.value.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes capturas guardadas", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(snapshots.value) { snapshot ->
                    SnapshotCard(
                        snapshot = snapshot,
                        token = tokenStore.getToken() ?: "",
                        onClick = { selectedSnapshot.value = snapshot },
                        onDelete = {
                            scope.launch {
                                runCatching { api.deleteSnapshot(snapshot.id) }
                                    .onSuccess {
                                        Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
                                        refresh()
                                    }
                            }
                        }
                    )
                }
            }
        }

        // Full Screen Preview
        selectedSnapshot.value?.let { snapshot ->
            Dialog(
                onDismissRequest = { selectedSnapshot.value = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data("${ApiConfig.BASE_URL}snapshots/${snapshot.id}/image")
                                .addHeader("Authorization", "Bearer ${tokenStore.getToken()}")
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
fun SnapshotCard(
    snapshot: Snapshot,
    token: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${ApiConfig.BASE_URL}snapshots/${snapshot.id}/image")
                    .addHeader("Authorization", "Bearer $token")
                    .crossfade(true)
                    .build(),
                contentDescription = "Capture",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop
            )
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
        
        Column(Modifier.padding(8.dp)) {
            Text(
                text = snapshot.camera_name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = snapshot.created_at,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
