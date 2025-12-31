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

@Composable
fun CctvApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }

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
                onOpenCamera = { cameraId ->
                    navController.navigate("player/$cameraId")
                }
            )
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
