package com.duckblast.game.ui.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duckblast.game.multiplayer.MultiplayerManager
import com.duckblast.game.ui.theme.DuckBlastAccent
import com.duckblast.game.ui.theme.DuckBlastError
import com.duckblast.game.ui.theme.DuckBlastOutline
import com.duckblast.game.ui.theme.DuckBlastPrimary
import com.duckblast.game.ui.theme.DuckBlastSecondary
import com.duckblast.game.ui.theme.DuckBlastSurface
import com.duckblast.game.ui.theme.DuckBlastText
import com.duckblast.game.ui.theme.SkyTop
import org.koin.androidx.compose.koinViewModel

@Composable
fun LobbyScreen(
    onStartGame: (seed: Long) -> Unit,
    onBack: () -> Unit,
    viewModel: LobbyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigation.collect { seed -> onStartGame(seed) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyTop)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "MULTIPLAYER LOBBY",
                style = MaterialTheme.typography.headlineMedium,
                color = DuckBlastText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Same Wi-Fi only — UDP 239.255.12.99:47777",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (state.mode) {
                LobbyMode.IDLE -> IdleSection(
                    onHost = viewModel::host,
                    onJoin = viewModel::join,
                    onBack = onBack
                )
                LobbyMode.HOSTING, LobbyMode.CLIENT -> ActiveSection(
                    isHost = state.mode == LobbyMode.HOSTING,
                    players = state.players,
                    onStart = viewModel::startGame,
                    onLeave = {
                        viewModel.leave()
                        onBack()
                    }
                )
                LobbyMode.COUNTDOWN -> CountdownSection(seconds = state.countdown)
            }
        }
    }
}

@Composable
private fun IdleSection(
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Highest score across the round wins.",
            style = MaterialTheme.typography.bodyMedium,
            color = DuckBlastText,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onHost,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuckBlastPrimary,
                contentColor = DuckBlastSurface
            )
        ) { Text("HOST GAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Button(
            onClick = onJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuckBlastSecondary,
                contentColor = DuckBlastSurface
            )
        ) { Text("JOIN GAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuckBlastSurface,
                contentColor = DuckBlastText
            )
        ) { Text("BACK", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun ActiveSection(
    isHost: Boolean,
    players: List<MultiplayerManager.RemotePlayer>,
    onStart: () -> Unit,
    onLeave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = if (isHost) "HOSTING" else "JOINED — WAITING FOR HOST",
            style = MaterialTheme.typography.titleMedium,
            color = DuckBlastPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Players in range: ${players.size}",
            style = MaterialTheme.typography.labelMedium,
            color = DuckBlastText
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (players.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Looking for hunters on this network…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DuckBlastText
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(players, key = { it.profileId }) { player ->
                    PlayerRow(player)
                }
            }
        }
        if (isHost) {
            Button(
                onClick = onStart,
                enabled = players.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DuckBlastPrimary,
                    contentColor = DuckBlastSurface,
                    disabledContainerColor = DuckBlastSurface,
                    disabledContentColor = DuckBlastText
                )
            ) {
                Text(
                    if (players.isNotEmpty()) "START" else "WAITING FOR PLAYERS…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = onLeave,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuckBlastError,
                contentColor = DuckBlastSurface
            )
        ) { Text("LEAVE", style = MaterialTheme.typography.titleSmall) }
    }
}

@Composable
private fun PlayerRow(player: MultiplayerManager.RemotePlayer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DuckBlastSurface)
            .border(1.dp, DuckBlastOutline, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (player.isHost) DuckBlastAccent else DuckBlastSecondary)
                .border(2.dp, DuckBlastOutline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = DuckBlastText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = player.ip,
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText
            )
        }
        if (player.isHost) {
            Text(
                text = "HOST",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CountdownSection(seconds: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STARTING IN",
            style = MaterialTheme.typography.titleMedium,
            color = DuckBlastText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (seconds > 0) seconds.toString() else "GO!",
            style = MaterialTheme.typography.displayLarge,
            color = DuckBlastPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
