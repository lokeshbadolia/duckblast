package com.duckblast.game.game.engine

import com.duckblast.game.audio.SoundId
import com.duckblast.game.audio.SoundManager
import com.duckblast.game.game.entities.Crosshair
import com.duckblast.game.game.entities.DogMode
import com.duckblast.game.game.entities.Duck
import com.duckblast.game.game.entities.DuckDog
import com.duckblast.game.game.entities.Gun
import com.duckblast.game.game.entities.Plate
import com.duckblast.game.game.entities.PlateShard
import com.duckblast.game.game.entities.ScorePopup
import com.duckblast.game.game.entities.Target
import com.duckblast.game.game.entities.TargetState
import com.duckblast.game.game.entities.WorldBounds
import com.duckblast.game.game.level.GameMode
import com.duckblast.game.game.level.LevelConfig
import com.duckblast.game.game.level.LevelManager
import com.duckblast.game.game.scoring.BonusCalculator
import com.duckblast.game.game.scoring.ScoreSystem
import com.duckblast.game.haptics.VibrationManager
import com.duckblast.game.haptics.VibrationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The core game model. Thread-confined to the game-loop thread for updates;
 * touch events arrive from the UI thread via a lock-free queue. State is held
 * in plain fields and mutated by [update]; reads by the renderer happen on
 * the same thread, while [state] is exposed via [AtomicReference] for cross-
 * thread observers (ViewModel, multiplayer sync).
 */
class GameEngine(
    private val soundManager: SoundManager,
    private val vibrationManager: VibrationManager,
    private val levelManager: LevelManager,
    private val scoreSystem: ScoreSystem
) {

    /* ---------------- dimensions ---------------- */

    var screenWidthPx: Float = 1080f
        private set
    var screenHeightPx: Float = 1920f
        private set
    var density: Float = 3f
        private set
    var world: WorldBounds = WorldBounds(0f, 0f, screenWidthPx, screenHeightPx)
        private set

    /* ---------------- state machine ---------------- */

    private val _state = AtomicReference<GameState>(GameState.Idle)
    val state: GameState get() = _state.get()
    private var pausedSnapshot: GameState? = null
    private var stateDuration: Float = 0f
    private var stateElapsed: Float = 0f

    /* ---------------- entities ---------------- */

    val crosshair = Crosshair()
    val gun = Gun()
    val dog = DuckDog()
    val targets: List<Target> get() = _targets
    val scorePopups: List<ScorePopup> get() = _scorePopups
    val plateShards: List<PlateShard> get() = _plateShards
    private val _targets = mutableListOf<Target>()
    private val _scorePopups = mutableListOf<ScorePopup>()
    private val _plateShards = mutableListOf<PlateShard>()

    /* ---------------- round / level tracking ---------------- */

    var roundIndex: Int = 0
        private set
    var shotsRemaining: Int = 3
        private set
    var hitsThisRound: Int = 0
        private set
    var hitsThisLevel: Int = 0
        private set
    var hiScore: Long = 0L

    val score: Long get() = scoreSystem.score
    val ducksHit: Int get() = scoreSystem.ducksHit
    val shotsFired: Int get() = scoreSystem.shotsFired
    val accuracy: Float get() = scoreSystem.accuracy
    val currentLevel: Int get() = levelManager.currentLevel
    val currentConfig: LevelConfig get() = levelManager.current()
    var startTimeMs: Long = 0L
        private set
    val elapsedSeconds: Int
        get() = if (startTimeMs == 0L) 0 else ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()

    /* ---------------- touch ---------------- */

    private val touchQueue = ConcurrentLinkedQueue<TouchEvent>()

    /* ---------------- events ---------------- */

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64, replay = 0)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /* ---------------- rng ---------------- */

    private var random: Random = Random.Default

    /* ---------------- lifecycle ---------------- */

    fun setSize(widthPx: Float, heightPx: Float, density: Float) {
        this.screenWidthPx = widthPx
        this.screenHeightPx = heightPx
        this.density = density.coerceAtLeast(1f)
        val hud = heightPx * HUD_FRACTION
        val top = heightPx * TOP_BAR_FRACTION
        world = WorldBounds(0f, top, widthPx, heightPx - hud)
        crosshair.radius = 32f * this.density
        crosshair.moveTo(widthPx / 2f, heightPx * 0.55f, world, CROSSHAIR_PADDING_DP * this.density)
        gun.x = widthPx / 2f
        gun.y = world.bottom + 20f * this.density
        dog.x = widthPx / 2f
        dog.y = world.bottom - 40f * this.density
    }

    fun start(seed: Long = 0L) {
        random = if (seed != 0L) Random(seed) else Random.Default
        levelManager.reset()
        scoreSystem.reset()
        _targets.clear()
        _scorePopups.clear()
        _plateShards.clear()
        roundIndex = 0
        hitsThisLevel = 0
        startTimeMs = System.currentTimeMillis()
        pausedSnapshot = null
        beginDogIntro(firstOfLevel = true)
    }

    fun reset() = start(0L)

    fun pause() {
        val s = state
        if (s !is GameState.Paused && s !is GameState.Idle && s !is GameState.GameOver) {
            pausedSnapshot = s
            _state.set(GameState.Paused)
        }
    }

    fun resume() {
        val snap = pausedSnapshot ?: return
        _state.set(snap)
        pausedSnapshot = null
    }

    fun onTouchMove(x: Float, y: Float) { touchQueue.add(TouchEvent.Move(x, y)) }
    fun onTouchUp(x: Float, y: Float) { touchQueue.add(TouchEvent.Up(x, y)) }

    /* ---------------- tick ---------------- */

    fun update(dt: Float) {
        if (state is GameState.Paused) {
            drainTouches(processShots = false)
            return
        }
        drainTouches(processShots = true)
        stateElapsed += dt

        when (val s = state) {
            GameState.Idle -> Unit
            is GameState.DogIntro -> tickDogIntro()
            is GameState.DuckLaunch -> tickDuckLaunch()
            GameState.Playing -> tickPlaying()
            is GameState.RoundResult -> tickRoundResult(s)
            is GameState.LevelClear -> tickLevelClear(s)
            GameState.GameOver -> Unit
            GameState.Paused -> Unit
        }

        gun.update(dt, crosshair)
        dog.update(dt)

        val itT = _targets.iterator()
        while (itT.hasNext()) {
            val t = itT.next()
            t.update(dt, world)
            if (t.resolved) itT.remove()
        }
        val itP = _scorePopups.iterator()
        while (itP.hasNext()) {
            val p = itP.next(); p.update(dt); if (p.expired) itP.remove()
        }
        val itS = _plateShards.iterator()
        while (itS.hasNext()) {
            val s = itS.next(); s.update(dt); if (s.expired) itS.remove()
        }
    }

    /* ---------------- state ticks ---------------- */

    private fun tickDogIntro() {
        val progress = (stateElapsed / stateDuration).coerceIn(0f, 1f)
        _state.set(GameState.DogIntro(progress))
        when {
            stateElapsed < stateDuration * 0.27f ->
                if (dog.mode != DogMode.INTRO_RUN) dog.setMode(DogMode.INTRO_RUN, stateDuration * 0.27f)
            stateElapsed < stateDuration * 0.55f ->
                if (dog.mode != DogMode.INTRO_SNIFF) dog.setMode(DogMode.INTRO_SNIFF, stateDuration * 0.28f)
            stateElapsed < stateDuration * 0.70f ->
                if (dog.mode != DogMode.INTRO_JUMP) {
                    dog.setMode(DogMode.INTRO_JUMP, stateDuration * 0.15f)
                    vibrationManager.vibrate(VibrationType.DOG_APPEAR)
                }
            else ->
                if (dog.mode != DogMode.HIDDEN) dog.setMode(DogMode.HIDDEN, 0f)
        }
        if (stateElapsed >= stateDuration) {
            spawnRound()
            transitionTo(GameState.DuckLaunch(0f), 0.4f)
        }
    }

    private fun tickDuckLaunch() {
        val progress = (stateElapsed / stateDuration).coerceIn(0f, 1f)
        _state.set(GameState.DuckLaunch(progress))
        if (stateElapsed >= stateDuration) {
            transitionTo(GameState.Playing, INDEFINITE)
        }
    }

    private fun tickPlaying() {
        if (_targets.isEmpty()) finalizeRound()
    }

    private fun tickRoundResult(current: GameState.RoundResult) {
        val progress = (stateElapsed / stateDuration).coerceIn(0f, 1f)
        if (progress != current.progress) _state.set(current.copy(progress = progress))
        if (stateElapsed >= stateDuration) advanceRound()
    }

    private fun tickLevelClear(current: GameState.LevelClear) {
        val progress = (stateElapsed / stateDuration).coerceIn(0f, 1f)
        if (progress != current.progress) _state.set(current.copy(progress = progress))
        if (stateElapsed >= stateDuration) {
            levelManager.advance()
            hitsThisLevel = 0
            roundIndex = 0
            beginDogIntro(firstOfLevel = true)
        }
    }

    /* ---------------- spawns ---------------- */

    private fun spawnRound() {
        val config = currentConfig
        shotsRemaining = config.shotsPerRound
        hitsThisRound = 0
        _targets.clear()
        when (config.mode) {
            GameMode.DUCK -> repeat(config.ducksPerRound) { spawnDuck(config) }
            GameMode.PLATE -> repeat(config.ducksPerRound) { spawnPlate(config) }
        }
    }

    private fun spawnDuck(config: LevelConfig) {
        val fromLeft = random.nextBoolean()
        val startX = if (fromLeft) world.left + 32f * density else world.right - 32f * density
        val startY = world.bottom - 60f * density - random.nextFloat() * world.height * 0.15f
        val speedPx = config.duckSpeed * density
        val baseVx = if (fromLeft) speedPx else -speedPx
        val verticalDistance = world.height * 0.70f
        val baseVy = -verticalDistance / config.timePerDuck
        val zigAmp = speedPx * 0.45f
        val zigFreq = 0.9f + random.nextFloat() * 0.6f
        _targets.add(
            Duck(
                x = startX,
                y = startY,
                baseVx = baseVx,
                baseVy = baseVy,
                zigzagAmplitude = zigAmp,
                zigzagFrequency = zigFreq,
                radius = 28f * density
            )
        )
        soundManager.play(SoundId.DUCK_QUACK)
    }

    private fun spawnPlate(config: LevelConfig) {
        val fromLeft = random.nextBoolean()
        val startX = if (fromLeft) world.left + 60f * density else world.right - 60f * density
        val startY = world.bottom - 30f * density
        val centerX = world.left + world.width / 2f
        val airTime = (config.timePerDuck * 0.6f).coerceAtLeast(1.0f)
        val vx = (centerX - startX) / airTime
        val targetApex = world.top + world.height * 0.18f
        val rise = startY - targetApex
        val gravity = 1400f * density
        val vy = -sqrt(2f * gravity * rise.coerceAtLeast(1f))
        _targets.add(
            Plate(
                x = startX,
                y = startY,
                vx = vx,
                vy = vy,
                radius = 22f * density,
                gravity = gravity
            )
        )
        soundManager.play(SoundId.PLATE_LAUNCH)
    }

    /* ---------------- round resolution ---------------- */

    private fun finalizeRound() {
        val reaction = if (hitsThisRound > 0) DogReaction.CARRY else DogReaction.LAUGH
        val dogDuration = if (reaction == DogReaction.CARRY) 2.0f else 2.5f
        dog.setMode(
            if (reaction == DogReaction.CARRY) DogMode.SUCCESS_CARRY else DogMode.LAUGH,
            dogDuration
        )
        val config = currentConfig
        if (hitsThisRound == config.ducksPerRound) {
            val bonus = BonusCalculator.perfectRoundBonus(config)
            scoreSystem.addBonus(bonus)
            _events.tryEmit(GameEvent.PerfectRound)
            soundManager.play(SoundId.PERFECT_ROUND)
        }
        soundManager.play(if (reaction == DogReaction.CARRY) SoundId.DOG_BARK else SoundId.DOG_LAUGH)
        _events.tryEmit(GameEvent.RoundEnded(hitsThisRound, config.ducksPerRound, reaction))
        transitionTo(
            GameState.RoundResult(hitsThisRound, config.ducksPerRound, reaction, 0f),
            dogDuration
        )
    }

    private fun advanceRound() {
        val config = currentConfig
        roundIndex += 1
        if (roundIndex >= config.roundsPerLevel) {
            if (hitsThisLevel >= config.ducksToPass) {
                val bonus = BonusCalculator.levelClearBonus(config, hitsThisLevel)
                scoreSystem.addBonus(bonus)
                _events.tryEmit(GameEvent.LevelCleared(config.level, bonus))
                soundManager.play(SoundId.LEVEL_CLEAR)
                vibrationManager.vibrate(VibrationType.LEVEL_CLEAR)
                transitionTo(GameState.LevelClear(config.level, 0f), 2.0f)
            } else {
                _events.tryEmit(GameEvent.GameEnded)
                soundManager.play(SoundId.GAME_OVER)
                vibrationManager.vibrate(VibrationType.GAME_OVER)
                _state.set(GameState.GameOver)
                stateElapsed = 0f
                stateDuration = INDEFINITE
            }
        } else {
            beginDogIntro(firstOfLevel = false)
        }
    }

    private fun beginDogIntro(firstOfLevel: Boolean) {
        val duration = if (firstOfLevel) 1.5f else 0.9f
        transitionTo(GameState.DogIntro(0f), duration)
        dog.setMode(DogMode.INTRO_RUN, duration * 0.27f)
        soundManager.play(SoundId.ROUND_START)
    }

    private fun transitionTo(newState: GameState, duration: Float) {
        _state.set(newState)
        stateDuration = duration
        stateElapsed = 0f
    }

    /* ---------------- input ---------------- */

    private fun drainTouches(processShots: Boolean) {
        while (true) {
            val ev = touchQueue.poll() ?: break
            when (ev) {
                is TouchEvent.Move -> moveCrosshair(ev.x, ev.y)
                is TouchEvent.Up -> {
                    moveCrosshair(ev.x, ev.y)
                    if (processShots) handleShot(ev.x, ev.y)
                }
            }
        }
    }

    private fun moveCrosshair(x: Float, y: Float) {
        crosshair.moveTo(x, y, world, CROSSHAIR_PADDING_DP * density)
    }

    private fun handleShot(x: Float, y: Float) {
        when (state) {
            GameState.Playing -> doPlayingShot(x, y)
            is GameState.RoundResult -> if (dog.isLaughing()) doEasterEggShot(x, y)
            else -> Unit
        }
    }

    private fun doPlayingShot(x: Float, y: Float) {
        if (shotsRemaining <= 0) return
        shotsRemaining -= 1
        scoreSystem.addShot()
        gun.fire()
        soundManager.play(SoundId.GUNSHOT)
        vibrationManager.vibrate(VibrationType.GUNSHOT)
        _events.tryEmit(GameEvent.Shot(x, y))

        val forgive = HIT_FORGIVENESS_DP * density
        val hit = _targets.firstOrNull { it.killable && CollisionDetector.hit(x, y, it, forgive) }
        if (hit != null) {
            hit.onHit()
            val config = currentConfig
            scoreSystem.addHit(config.pointsPerDuck)
            hitsThisRound += 1
            hitsThisLevel += 1
            _scorePopups.add(
                ScorePopup(
                    x = hit.x,
                    y = hit.y,
                    text = "+${config.pointsPerDuck}",
                    color = SCORE_COLOR
                )
            )
            vibrationManager.vibrate(VibrationType.HIT_TARGET)
            if (hit is Plate) {
                spawnShardsForPlate(hit)
                soundManager.play(SoundId.PLATE_BREAK)
            } else {
                soundManager.play(SoundId.DUCK_FALL)
            }
            _events.tryEmit(GameEvent.TargetHit(hit.x, hit.y, config.pointsPerDuck))
        } else {
            _events.tryEmit(GameEvent.TargetMissed(x, y))
        }

        if (shotsRemaining <= 0) {
            _targets.forEach {
                if (it.state == TargetState.ACTIVE) {
                    it.beginEscape()
                    _events.tryEmit(GameEvent.TargetEscaped(it.x, it.y))
                    soundManager.play(SoundId.DUCK_ESCAPED)
                }
            }
        }
    }

    private fun doEasterEggShot(x: Float, y: Float) {
        val dogRadius = 60f * density
        val dx = x - dog.x
        val dy = y - dog.y
        if (dx * dx + dy * dy > dogRadius * dogRadius) return
        scoreSystem.addBonus(BonusCalculator.EASTER_EGG_DOG_SHOT)
        dog.setMode(DogMode.FLINCH, 0.8f)
        _scorePopups.add(
            ScorePopup(
                x = dog.x,
                y = dog.y - 20f * density,
                text = "BAD HUNTER! +${BonusCalculator.EASTER_EGG_DOG_SHOT}",
                color = SCORE_COLOR,
                lifetime = 1.4f
            )
        )
        soundManager.play(SoundId.DOG_BARK)
        vibrationManager.vibrate(VibrationType.HIT_TARGET)
        _events.tryEmit(GameEvent.DogShotEasterEgg)
    }

    private fun spawnShardsForPlate(plate: Plate) {
        val baseVx = plate.vx
        val baseVy = plate.vy
        val push = 240f * density
        _plateShards.add(
            PlateShard(
                x = plate.x, y = plate.y,
                vx = baseVx - push, vy = baseVy - push * 0.4f,
                rotationSpeed = -540f,
                gravity = 1500f * density,
                orientation = 0
            )
        )
        _plateShards.add(
            PlateShard(
                x = plate.x, y = plate.y,
                vx = baseVx + push, vy = baseVy - push * 0.4f,
                rotationSpeed = 540f,
                gravity = 1500f * density,
                orientation = 1
            )
        )
    }

    /* ---------------- companion ---------------- */

    companion object {
        const val HUD_FRACTION: Float = 0.15f
        const val TOP_BAR_FRACTION: Float = 0.06f
        const val HIT_FORGIVENESS_DP: Float = 20f
        const val CROSSHAIR_PADDING_DP: Float = 32f
        const val SCORE_COLOR: Int = 0xFFFCFC00.toInt()
        const val INDEFINITE: Float = Float.POSITIVE_INFINITY
    }
}

internal sealed class TouchEvent {
    data class Move(val x: Float, val y: Float) : TouchEvent()
    data class Up(val x: Float, val y: Float) : TouchEvent()
}
