package com.duckblast.game.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.duckblast.game.ui.multiplayer.MultiplayerOverlay
import org.koin.androidx.compose.koinViewModel

@Composable
fun GameScreen(
    multiplayer: Boolean,
    seed: Long,
    onNavigateToMenu: () -> Unit,
    onNavigateToGameOver: (GameNavigation.ToGameOver) -> Unit,
    viewModel: GameViewModel = koinViewModel()
) {
    val overlay by viewModel.overlay.collectAsState()
    val remotePlayers by viewModel.remotePlayers.collectAsState()

    LaunchedEffect(seed, multiplayer) {
        viewModel.initialize(seed, multiplayer)
    }
    LaunchedEffect(Unit) {
        viewModel.navigation.collect { nav ->
            when (nav) {
                GameNavigation.ToMenu -> onNavigateToMenu()
                is GameNavigation.ToGameOver -> onNavigateToGameOver(nav)
            }
        }
    }

    BackHandler {
        if (overlay is GameOverlayState.Paused) viewModel.quit() else viewModel.togglePause()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx -> GameSurfaceView(ctx, viewModel) },
            modifier = Modifier.fillMaxSize()
        )

        if (multiplayer) {
            MultiplayerOverlay(
                players = remotePlayers,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp, top = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, end = 12.dp)
        ) {
            val isPaused = overlay is GameOverlayState.Paused
            FilledIconButton(
                onClick = viewModel::togglePause,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xCC000033),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause"
                )
            }
        }

        if (overlay is GameOverlayState.Paused) {
            PauseOverlay(
                onResume = viewModel::togglePause,
                onQuit = viewModel::quit
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000033))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.displayMedium,
                color = Color(0xFFFCFC00)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF884400),
                    contentColor = Color.White
                )
            ) {
                Text("RESUME", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onQuit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF000033)
                )
            ) {
                Text("QUIT", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Tip — long-press a hunter on the profile screen to delete them.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA8D8EA)
            )
        }
    }
}
