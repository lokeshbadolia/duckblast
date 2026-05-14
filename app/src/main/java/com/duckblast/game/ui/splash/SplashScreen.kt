package com.duckblast.game.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duckblast.game.audio.SoundId
import com.duckblast.game.audio.SoundManager
import com.duckblast.game.data.repository.ProfileRepository
import com.duckblast.game.ui.theme.DuckBeak
import com.duckblast.game.ui.theme.DuckBrown
import com.duckblast.game.ui.theme.DuckGreen
import com.duckblast.game.ui.theme.DuckWhite
import com.duckblast.game.ui.theme.SkyBottom
import com.duckblast.game.ui.theme.SkyTop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.sin

@Composable
fun SplashScreen(
    onProfilePresent: () -> Unit,
    onNoProfile: () -> Unit,
    profileRepository: ProfileRepository = koinInject(),
    soundManager: SoundManager = koinInject()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
    ) {
        FlyingDuckLoop(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DUCK BLAST",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = DuckBrown,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A Retro Shooting Tribute",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF003300),
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(Unit) {
        soundManager.play(SoundId.SPLASH_FANFARE)
        delay(2000)
        val hasProfile = withContext(Dispatchers.IO) {
            val id = profileRepository.getSelectedProfileId()
            id != 0L && profileRepository.getProfileById(id) != null
        }
        if (hasProfile) onProfilePresent() else onNoProfile()
    }
}

@Composable
private fun FlyingDuckLoop(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "splash-duck")
    val travel by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "travel"
    )
    val flap by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flap"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = -w * 0.2f + travel * (w * 1.4f)
        val cy = h * 0.35f + sin(travel * 6.28f * 1.5f).toFloat() * h * 0.04f

        drawDuck(cx, cy, w * 0.10f, flap)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDuck(
    cx: Float,
    cy: Float,
    bodyRadius: Float,
    flap: Float
) {
    val bodyHeight = bodyRadius * 0.9f
    // body
    drawOval(
        color = DuckBrown,
        topLeft = Offset(cx - bodyRadius, cy - bodyHeight / 2f),
        size = Size(bodyRadius * 2f, bodyHeight)
    )
    // white belly
    drawOval(
        color = DuckWhite,
        topLeft = Offset(cx - bodyRadius * 0.55f, cy - bodyHeight * 0.20f),
        size = Size(bodyRadius * 1.10f, bodyHeight * 0.55f)
    )
    // head
    drawCircle(color = DuckGreen, radius = bodyRadius * 0.45f, center = Offset(cx + bodyRadius * 0.8f, cy - bodyHeight * 0.25f))
    // beak
    drawRect(
        color = DuckBeak,
        topLeft = Offset(cx + bodyRadius * 1.10f, cy - bodyHeight * 0.20f),
        size = Size(bodyRadius * 0.45f, bodyHeight * 0.20f)
    )
    // eye
    drawCircle(color = Color.Black, radius = bodyRadius * 0.06f, center = Offset(cx + bodyRadius * 0.95f, cy - bodyHeight * 0.32f))

    // wing — rotates between three positions via flap value
    val wingOffsetY = bodyHeight * (-0.30f + flap * 0.60f)
    drawRect(
        color = Color(0xFF552200),
        topLeft = Offset(cx - bodyRadius * 0.30f, cy + wingOffsetY),
        size = Size(bodyRadius * 0.95f, bodyHeight * 0.30f)
    )
}
