package com.rtsp.cctv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rtsp.cctv.data.Camera
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.network.ApiClient
import com.rtsp.cctv.network.snapshotUrl

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraGridScreen(onOpenCamera: (Int) -> Unit, onOpenProfile: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val cameras = remember { mutableStateOf<List<Camera>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { api.getCameras() }
            .onSuccess { cameras.value = it.items }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Lisan Cam", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(cameras.value, key = { it.id }) { camera ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCamera(camera.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val requestBuilder = ImageRequest.Builder(context)
                            .data(snapshotUrl(camera.id))
                            .crossfade(true)

                        tokenStore.getToken()?.let { token ->
                            requestBuilder.addHeader("Authorization", "Bearer $token")
                        }

                        val request = requestBuilder.build()

                        AsyncImage(
                            model = request,
                            contentDescription = camera.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = camera.name,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
