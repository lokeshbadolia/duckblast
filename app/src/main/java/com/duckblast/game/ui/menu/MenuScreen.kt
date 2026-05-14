package com.duckblast.game.ui.menu

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duckblast.game.data.model.Profile
import com.duckblast.game.ui.theme.BushGreen
import com.duckblast.game.ui.theme.DuckBlastAccent
import com.duckblast.game.ui.theme.DuckBlastOutline
import com.duckblast.game.ui.theme.DuckBlastPrimary
import com.duckblast.game.ui.theme.DuckBlastSecondary
import com.duckblast.game.ui.theme.DuckBlastSurface
import com.duckblast.game.ui.theme.DuckBlastText
import com.duckblast.game.ui.theme.GrassDark
import com.duckblast.game.ui.theme.GrassLight
import com.duckblast.game.ui.theme.GroundBrown
import com.duckblast.game.ui.theme.SkyBottom
import com.duckblast.game.ui.theme.SkyTop
import com.duckblast.game.ui.theme.TreeDark
import org.koin.androidx.compose.koinViewModel

@Composable
fun MenuScreen(
    onPlay: () -> Unit,
    onMultiplayer: () -> Unit,
    onScoreboard: () -> Unit,
    onChangeHunter: () -> Unit,
    viewModel: MenuViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startMenuMusic()
        onDispose { viewModel.stopMenuMusic() }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        ParallaxBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DUCK BLAST",
                style = MaterialTheme.typography.displaySmall,
                color = DuckBlastPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            PlayerCard(profile = state.profile)

            Spacer(modifier = Modifier.height(28.dp))

            MenuButton(
                label = "PLAY",
                background = DuckBlastPrimary,
                contentColor = DuckBlastSurface
            ) {
                viewModel.onPlayPressed()
                onPlay()
            }
            Spacer(modifier = Modifier.height(12.dp))
            MenuButton(
                label = "MULTIPLAYER",
                background = DuckBlastSecondary,
                contentColor = DuckBlastSurface
            ) {
                viewModel.click()
                onMultiplayer()
            }
            Spacer(modifier = Modifier.height(12.dp))
            MenuButton(
                label = "SCOREBOARD",
                background = DuckBlastSurface,
                contentColor = DuckBlastText
            ) {
                viewModel.click()
                onScoreboard()
            }
            Spacer(modifier = Modifier.height(12.dp))
            MenuButton(
                label = "CHANGE HUNTER",
                background = DuckBlastSurface,
                contentColor = DuckBlastText
            ) {
                viewModel.click()
                onChangeHunter()
            }
        }

        // Anchor the dog so its body peeks above the ground band (drawn at
        // h * 0.86f in ParallaxBackdrop). Padding is screen-relative so the
        // position holds on tall and short phones alike.
        DogPeek(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.11f)
        )
    }
}

@Composable
private fun PlayerCard(profile: Profile?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DuckBlastSurface)
            .border(2.dp, DuckBlastOutline, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val accentColor = runCatching {
            Color(android.graphics.Color.parseColor(profile?.avatarColorHex ?: "#884400"))
        }.getOrDefault(DuckBlastPrimary)

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(accentColor)
                .border(2.dp, DuckBlastOutline, RoundedCornerShape(27.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile?.initial ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile?.name?.uppercase() ?: "NO HUNTER",
                style = MaterialTheme.typography.titleMedium,
                color = DuckBlastText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HI ${profile?.highScore ?: 0} • LVL ${profile?.highestLevelReached ?: 0}",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText
            )
        }
        if (profile != null && profile.streakDays > 0) {
            StreakBadge(days = profile.streakDays)
        }
    }
}

@Composable
private fun StreakBadge(days: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DuckBlastAccent)
            .border(2.dp, DuckBlastOutline, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "★ $days",
            style = MaterialTheme.typography.labelSmall,
            color = DuckBlastText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MenuButton(
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = background,
            contentColor = contentColor
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ParallaxBackdrop() {
    val infinite = rememberInfiniteTransition(label = "menu-parallax")
    val farOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "far"
    )
    val nearOffset by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "near"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Sky
        drawRect(brush = Brush.verticalGradient(listOf(SkyTop, SkyBottom)), size = Size(w, h))

        // Distant tree silhouette band
        val farBandY = h * 0.62f
        val farBandH = h * 0.10f
        val farShift = -farOffset * w
        repeat(6) { idx ->
            val left = (idx * w / 4f) + farShift
            drawRect(
                color = TreeDark,
                topLeft = Offset(left, farBandY),
                size = Size(w / 4f * 0.9f, farBandH)
            )
            drawRect(
                color = TreeDark,
                topLeft = Offset(left + w / 4f, farBandY + farBandH * 0.3f),
                size = Size(w / 4f * 0.7f, farBandH * 0.7f)
            )
        }

        // Hills
        val hillsTop = h * 0.72f
        drawRect(color = GrassDark, topLeft = Offset(0f, hillsTop), size = Size(w, h - hillsTop))
        // Foreground grass band
        val grassY = h * 0.80f
        drawRect(color = GrassLight, topLeft = Offset(0f, grassY), size = Size(w, h * 0.05f))

        // Bushes that drift
        val bushBandY = h * 0.78f
        val nearShift = -nearOffset * w * 1.5f
        repeat(8) { idx ->
            val cx = (idx * w / 5f) + nearShift
            drawCircle(
                color = BushGreen,
                radius = w * 0.06f,
                center = Offset(cx, bushBandY)
            )
            drawCircle(
                color = BushGreen,
                radius = w * 0.04f,
                center = Offset(cx + w * 0.04f, bushBandY + h * 0.005f)
            )
        }

        // Ground
        val groundY = h * 0.86f
        drawRect(color = GroundBrown, topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
    }
}

@Composable
private fun DogPeek(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "dog")
    val bob by infinite.animateFloat(
        initialValue = -10f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val bodyColor = Color(0xFFC9874F)        // tan — contrasts with both grass and GroundBrown
    val earColor = Color(0xFF6B3410)         // darker brown for ear definition
    val snoutColor = Color(0xFFEFCBA0)       // pale cream
    Canvas(
        modifier = modifier
            .size(width = 96.dp, height = 56.dp)
            .clickable(enabled = false) {}
    ) {
        val w = size.width
        val h = size.height
        // Body — peeking dog silhouette
        drawRect(
            color = bodyColor,
            topLeft = Offset(w * 0.15f, h * 0.40f + bob),
            size = Size(w * 0.70f, h * 0.55f)
        )
        // Ear
        drawRect(
            color = earColor,
            topLeft = Offset(w * 0.18f, h * 0.20f + bob),
            size = Size(w * 0.12f, h * 0.25f)
        )
        // Eye
        drawCircle(
            color = Color.White,
            radius = w * 0.05f,
            center = Offset(w * 0.40f, h * 0.55f + bob)
        )
        drawCircle(
            color = Color.Black,
            radius = w * 0.025f,
            center = Offset(w * 0.41f, h * 0.55f + bob)
        )
        // Snout
        drawRect(
            color = snoutColor,
            topLeft = Offset(w * 0.55f, h * 0.55f + bob),
            size = Size(w * 0.20f, h * 0.25f)
        )
        // Nose
        drawCircle(
            color = Color.Black,
            radius = w * 0.04f,
            center = Offset(w * 0.72f, h * 0.62f + bob)
        )
    }
}
