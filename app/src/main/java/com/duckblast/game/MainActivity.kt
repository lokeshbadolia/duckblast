package com.duckblast.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.duckblast.game.ui.navigation.DuckBlastNavGraph
import com.duckblast.game.ui.theme.DuckBlastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DuckBlastRoot() }
    }
}

@Composable
private fun DuckBlastRoot() {
    DuckBlastTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            DuckBlastNavGraph(navController = navController)
        }
    }
}
