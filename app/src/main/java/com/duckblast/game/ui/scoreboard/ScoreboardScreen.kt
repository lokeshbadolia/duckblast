package com.duckblast.game.ui.scoreboard

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duckblast.game.ui.theme.DuckBlastAccent
import com.duckblast.game.ui.theme.DuckBlastError
import com.duckblast.game.ui.theme.DuckBlastOutline
import com.duckblast.game.ui.theme.DuckBlastPrimary
import com.duckblast.game.ui.theme.DuckBlastSurface
import com.duckblast.game.ui.theme.DuckBlastText
import com.duckblast.game.ui.theme.SkyTop
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GOLD = Color(0xFFFFD700)
private val SILVER = Color(0xFFC0C0C0)
private val BRONZE = Color(0xFFCD7F32)

@Composable
fun ScoreboardScreen(
    onBack: () -> Unit,
    viewModel: ScoreboardViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyTop)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "HALL OF FAME",
                style = MaterialTheme.typography.headlineMedium,
                color = DuckBlastText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            if (!state.loading && state.entries.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No scores yet — play a round to land on the board.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DuckBlastText,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.entries, key = { _, e -> e.record.id }) { _, entry ->
                        ScoreboardRow(entry = entry, current = entry.profile.id == state.selectedProfileId)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DuckBlastPrimary,
                    contentColor = DuckBlastSurface
                )
            ) {
                Text("BACK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ScoreboardRow(entry: ScoreboardEntry, current: Boolean) {
    val rankColor = when (entry.rank) {
        1 -> GOLD
        2 -> SILVER
        3 -> BRONZE
        else -> DuckBlastSurface
    }
    val rowBackground = if (current) Color(0xFFFFEDB5) else DuckBlastSurface
    val borderColor = if (current) DuckBlastAccent else DuckBlastOutline
    val borderWidth = if (current) 3.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank chip
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(rankColor)
                .border(2.dp, DuckBlastOutline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = DuckBlastText,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Avatar
        val avatarColor = runCatching {
            Color(android.graphics.Color.parseColor(entry.profile.avatarColorHex))
        }.getOrDefault(DuckBlastPrimary)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(avatarColor)
                .border(2.dp, DuckBlastOutline, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.profile.initial,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.profile.name.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = DuckBlastText,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "LVL ${entry.record.levelReached} • ACC ${(entry.record.accuracy * 100).toInt()}% • ${formatDate(entry.record.playedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText
            )
        }

        Text(
            text = entry.record.score.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = DuckBlastError,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    return SimpleDateFormat("MMM dd", Locale.US).format(Date(timestamp))
}
