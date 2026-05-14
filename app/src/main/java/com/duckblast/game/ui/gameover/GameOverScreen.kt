package com.duckblast.game.ui.gameover

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duckblast.game.ui.theme.DuckBeak
import com.duckblast.game.ui.theme.DuckBlastAccent
import com.duckblast.game.ui.theme.DuckBlastError
import com.duckblast.game.ui.theme.DuckBlastOutline
import com.duckblast.game.ui.theme.DuckBlastPrimary
import com.duckblast.game.ui.theme.DuckBlastSecondary
import com.duckblast.game.ui.theme.DuckBlastSurface
import com.duckblast.game.ui.theme.DuckBlastText
import com.duckblast.game.ui.theme.SkyBottom
import com.duckblast.game.ui.theme.SkyTop
import kotlin.math.PI
import kotlin.math.sin
import org.koin.androidx.compose.koinViewModel

@Composable
fun GameOverScreen(
    score: Long,
    level: Int,
    ducksHit: Int,
    shotsFired: Int,
    durationSec: Int,
    perfect: Boolean,
    previousBest: Long,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
    viewModel: GameOverViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isNewRecord = score > 0 && score > previousBest

    LaunchedEffect(Unit) { viewModel.load() }

    var visible by remember { mutableStateOf(false) }
    val titleScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "title-scale"
    )
    LaunchedEffect(Unit) { visible = true }

    val animatedScore by animateIntAsState(
        targetValue = score.toInt().coerceAtLeast(0),
        animationSpec = tween(1500, easing = LinearOutSlowInEasing),
        label = "score-countup"
    )
    val accuracyPct = if (shotsFired > 0) (ducksHit * 100f / shotsFired) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GAME OVER",
                style = MaterialTheme.typography.displaySmall,
                color = DuckBlastError,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.scale(titleScale)
            )

            if (isNewRecord) NewRecordBadge()

            HunterLine(
                name = state.profileName,
                avatarColorHex = state.avatarColorHex,
                initial = state.avatarInitial
            )

            StatsCard(
                animatedScore = animatedScore.toLong().coerceAtLeast(score),
                level = level,
                ducksHit = ducksHit,
                shotsFired = shotsFired,
                accuracyPct = accuracyPct,
                bestScore = maxOf(state.bestScore, score),
                durationSec = durationSec,
                perfect = perfect
            )

            DogReactionPanel()

            Spacer(modifier = Modifier.weight(1f))

            ActionButton(
                label = "PLAY AGAIN",
                background = DuckBlastPrimary,
                contentColor = DuckBlastSurface,
                onClick = onPlayAgain
            )
            ActionButton(
                label = "SHARE",
                background = DuckBlastSecondary,
                contentColor = DuckBlastSurface,
                onClick = { shareScore(context, score, level, accuracyPct.toInt()) }
            )
            ActionButton(
                label = "MAIN MENU",
                background = DuckBlastSurface,
                contentColor = DuckBlastText,
                onClick = onMenu
            )
        }
    }
}

@Composable
private fun NewRecordBadge() {
    val pulse = rememberInfiniteTransition(label = "glow")
    val glow by pulse.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-alpha"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DuckBlastAccent.copy(alpha = glow))
            .border(2.dp, DuckBlastOutline, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "★ NEW RECORD ★",
            style = MaterialTheme.typography.titleMedium,
            color = DuckBlastText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HunterLine(name: String, avatarColorHex: String, initial: String) {
    val color = runCatching { Color(android.graphics.Color.parseColor(avatarColorHex)) }
        .getOrDefault(DuckBlastPrimary)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .border(2.dp, DuckBlastOutline, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = DuckBlastText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatsCard(
    animatedScore: Long,
    level: Int,
    ducksHit: Int,
    shotsFired: Int,
    accuracyPct: Float,
    bestScore: Long,
    durationSec: Int,
    perfect: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DuckBlastSurface)
            .border(2.dp, DuckBlastOutline, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatRow(label = "YOUR SCORE", value = animatedScore.toString(), accent = true)
        StatRow(label = "BEST SCORE", value = bestScore.toString())
        StatRow(label = "LEVEL REACHED", value = level.toString())
        StatRow(label = "DUCKS HIT", value = "$ducksHit / $shotsFired SHOTS")
        StatRow(label = "ACCURACY", value = "${accuracyPct.toInt()}%")
        StatRow(label = "DURATION", value = formatDuration(durationSec))
        if (perfect) {
            Text(
                text = "★ PERFECT GAME ★",
                style = MaterialTheme.typography.labelLarge,
                color = DuckBlastAccent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, accent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = DuckBlastText
        )
        Text(
            text = value,
            style = if (accent) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleSmall,
            color = if (accent) DuckBlastPrimary else DuckBlastText,
            fontWeight = if (accent) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (accent) 22.sp else 14.sp
        )
    }
}

@Composable
private fun DogReactionPanel() {
    val infinite = rememberInfiniteTransition(label = "dog-laugh")
    val bob by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val w = size.width
        val h = size.height
        // Grass strip
        drawRect(color = Color(0xFF00CC00), topLeft = Offset(0f, h * 0.62f), size = Size(w, h * 0.20f))
        drawRect(color = Color(0xFF884400), topLeft = Offset(0f, h * 0.82f), size = Size(w, h * 0.18f))
        // Dog head
        val cx = w * 0.5f
        val cy = h * 0.55f + bob
        val headW = 60f
        val headH = 50f
        drawRect(
            color = Color(0xFFC68A4F),
            topLeft = Offset(cx - headW / 2f, cy - headH),
            size = Size(headW, headH)
        )
        // Ears
        drawRect(
            color = Color(0xFF884400),
            topLeft = Offset(cx - headW / 2f - 6f, cy - headH - 14f),
            size = Size(20f, 26f)
        )
        // Mouth open
        drawRect(
            color = Color(0xFF441111),
            topLeft = Offset(cx - headW * 0.25f, cy - headH * 0.30f),
            size = Size(headW * 0.5f, headH * 0.18f)
        )
        // Closed laughing eyes (X marks)
        drawLine(Color.Black,
            Offset(cx - headW * 0.20f, cy - headH * 0.65f),
            Offset(cx - headW * 0.05f, cy - headH * 0.50f),
            strokeWidth = 3f)
        drawLine(Color.Black,
            Offset(cx - headW * 0.05f, cy - headH * 0.65f),
            Offset(cx - headW * 0.20f, cy - headH * 0.50f),
            strokeWidth = 3f)
        drawLine(Color.Black,
            Offset(cx + headW * 0.05f, cy - headH * 0.65f),
            Offset(cx + headW * 0.20f, cy - headH * 0.50f),
            strokeWidth = 3f)
        drawLine(Color.Black,
            Offset(cx + headW * 0.20f, cy - headH * 0.65f),
            Offset(cx + headW * 0.05f, cy - headH * 0.50f),
            strokeWidth = 3f)
        // Nose
        drawCircle(Color.Black, radius = 5f, center = Offset(cx + headW * 0.15f, cy - headH * 0.20f))
        // Snout
        drawRect(
            color = Color(0xFFE2B988),
            topLeft = Offset(cx, cy - headH * 0.40f),
            size = Size(headW * 0.30f, headH * 0.30f)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = contentColor
        )
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun shareScore(context: Context, score: Long, level: Int, accuracyPct: Int) {
    val text = "I scored $score on Duck Blast — Level $level, ${accuracyPct}% accuracy."
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share score"))
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
