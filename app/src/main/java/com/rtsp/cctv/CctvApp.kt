package com.rtsp.cctv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rtsp.cctv.data.TokenStore
import com.rtsp.cctv.ui.screens.CameraGridScreen
import com.rtsp.cctv.ui.screens.LoginScreen
import com.rtsp.cctv.ui.screens.PlayerScreen
import com.rtsp.cctv.ui.screens.ProfileScreen
import com.rtsp.cctv.ui.screens.SnapshotGalleryScreen

@Composable
fun CctvApp(isDark: Boolean, onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    
    // Shared cameras state for the whole session
    val cameras = remember { mutableStateOf<List<com.rtsp.cctv.data.Camera>>(emptyList()) }
    val isCamerasLoaded = remember { mutableStateOf(false) }

    val startRoute = if (tokenStore.hasToken()) "grid" else "login"

    NavHost(navController = navController, startDestination = startRoute) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("grid") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("grid") {
            CameraGridScreen(
                cameras = cameras.value,
                isLoaded = isCamerasLoaded.value,
                onRefresh = { list -> 
                    cameras.value = list
                    isCamerasLoaded.value = true
                },
                onOpenCamera = { cameraId ->
                    navController.navigate("player/$cameraId")
                },
                onOpenProfile = {
                    navController.navigate("profile")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                isDark = isDark,
                onThemeChange = onThemeChange,
                onBack = { navController.popBackStack() },
                onOpenSnapshots = { navController.navigate("snapshots") },
                onLogout = {
                    tokenStore.clear()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("snapshots") {
            SnapshotGalleryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "player/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getInt("cameraId") ?: 0
            PlayerScreen(
                cameraId = cameraId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
