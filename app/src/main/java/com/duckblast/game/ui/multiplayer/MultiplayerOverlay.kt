package com.duckblast.game.ui.multiplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duckblast.game.multiplayer.MultiplayerManager

/**
 * Top-left in-game live scoreboard for LAN sessions. Shows each remote player
 * with their current score, level, and alive/dead status. Hides itself when
 * the list is empty.
 */
@Composable
fun MultiplayerOverlay(
    players: List<MultiplayerManager.RemotePlayer>,
    modifier: Modifier = Modifier
) {
    if (players.isEmpty()) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC000033))
            .border(1.dp, Color(0xFFFCFC00), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .widthIn(min = 160.dp, max = 240.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "LIVE — LAN",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFCFC00),
            fontWeight = FontWeight.Bold
        )
        players.sortedByDescending { it.score }.take(4).forEach { player ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(alive = player.isAlive)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = player.name.take(8).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp)
                )
                Text(
                    text = player.score.toString().padStart(5, '0'),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCFC00),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "L${player.level}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatusDot(alive: Boolean) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(8.dp)
    ) {
        drawCircle(
            color = if (alive) Color(0xFF00CC00) else Color(0xFFD03030)
        )
    }
}
