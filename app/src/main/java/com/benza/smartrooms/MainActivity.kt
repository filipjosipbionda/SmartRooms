package com.benza.smartrooms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.benza.smartrooms.ui.theme.SmartRoomsTheme

/**
 * Main Android activity that hosts the Compose app.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRoomsTheme {
                SmartRoomsApp()
            }
        }
    }
}
