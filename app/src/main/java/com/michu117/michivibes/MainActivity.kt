package com.michu117.michivibes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.michu117.michivibes.ui.navigation.NavGraph
import com.michu117.michivibes.ui.theme.MichivibesTheme
import com.michu117.michivibes.utils.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MusicWrappedApp

        setContent {
            val theme by app.dataStoreManager.theme.collectAsState(
                initial = ThemeMode.SYSTEM
            )

            MichivibesTheme(themeMode = theme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
