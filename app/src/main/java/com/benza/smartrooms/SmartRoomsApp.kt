package com.benza.smartrooms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import com.benza.smartrooms.navigation.SmartRoomsNavHost

/**
 * Root composable that owns the app-wide navigation controller.
 */
@Composable
internal fun SmartRoomsApp() {
    val navController = rememberFreshNavController()
    SmartRoomsNavHost(navController = navController)
}

@Composable
private fun rememberFreshNavController(): NavHostController {
    val context = LocalContext.current

    return remember {
        NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
        }
    }
}
