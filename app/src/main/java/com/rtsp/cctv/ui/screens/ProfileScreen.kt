package com.rtsp.cctv.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.network.ApiClient
import com.rtsp.cctv.network.ChangePasswordRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val api = remember { ApiClient(tokenStore).api }
    val scope = rememberCoroutineScope()

    val profileData = remember { mutableStateOf<Map<String, Any>?>(null) }
    val newPassword = remember { mutableStateOf("") }
    val isChanging = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { api.getProfile() }
            .onSuccess { profileData.value = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Info Card
            ProfileInfoCard(
                username = profileData.value? web_application_development["username"]?.toString() ?: "Cargando...",
                role = profileData.value? web_application_development["role"]?.toString() ?: "..."
            )

            // Change Password Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Cambiar Contraseña", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    OutlinedTextField(
                        value = newPassword.value,
                        onValueChange = { newPassword.value = it },
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (newPassword.value.length < 4) {
                                Toast.makeText(context, "Clave muy corta", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isChanging.value = true
                            scope.launch {
                                runCatching {
                                    api.changePassword(ChangePasswordRequest(newPassword.value))
                                }.onSuccess {
                                    Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                                    newPassword.value = ""
                                }.onFailure {
                                    Toast.makeText(context, "Error al cambiar contraseña", Toast.LENGTH_SHORT).show()
                                }
                                isChanging.value = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        enabled = !isChanging.value
                    ) {
                        if (isChanging.value) {
                            CircularProgressIndicator(size = 24.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Actualizar")
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar Sesión")
            }
        }
    }
}

@Composable
fun ProfileInfoCard(username: String, role: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(username, style = MaterialTheme.typography.headlineSmall)
                Text(role.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
