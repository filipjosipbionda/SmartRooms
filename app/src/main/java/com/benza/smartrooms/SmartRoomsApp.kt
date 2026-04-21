package com.benza.smartrooms

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.benza.smartrooms.navigation.SmartRoomsNavHost

/**
 * Root composable that owns the app-wide navigation controller.
 */
@Composable
internal fun SmartRoomsApp() {
    val navController = rememberNavController()
    SmartRoomsNavHost(navController = navController)
}
