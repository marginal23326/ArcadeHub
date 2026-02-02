package com.example.arcadehub.games.blockstack

import android.graphics.Canvas
import android.graphics.Color
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.core.Scene
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SceneManager
import com.example.arcadehub.managers.SoundManager
import com.example.arcadehub.games.hub.HubScene
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs

class BlockStackScene : Scene {

    private val renderer = BlockRenderer()
    val cameraSystem = CameraSystem()
    var gameMode: GameMode = GameMode.LINEAR
    lateinit var physics: PhysicsStrategy // Changed to public/lateinit for access if needed

    var isGameStarted = false
    var isGameOver = false
    var isZoomedOut = false
    var shopSelectionIndex = 4

    var score = 0
    var highScore = 0
    var runCoins = 0
    var gameOverTimestamp = 0L

    var activeSloMoTurns = 0; var isMagnetActive = false; var isWidenerActive = false; var secondChanceUsed = false
    var perfectCount = 0; var greatCount = 0; var niceCount = 0; var perfectStreak = 0
    var feedbackText = ""; var feedbackSubText = ""; var feedbackColor = Color.WHITE
    var feedbackAlpha = 0; var feedbackY = 0f; var shakeTimer = 0f

    private val inputQueue = ConcurrentLinkedQueue<InputAction>()

    override fun enter() {
        setMode(GameMode.LINEAR)
        loadData()
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
    }

    private fun setMode(mode: GameMode) {
        gameMode = mode
        physics = when (mode) {
            GameMode.LINEAR -> LinearPhysics()
            GameMode.ORBIT -> RadialPhysics()
        }
    }

    private fun loadData() { highScore = SaveManager.getInt("BS_HIGHSCORE_${gameMode.name}") }
    private fun saveData() { SaveManager.saveHighScore("BS_HIGHSCORE_${gameMode.name}", highScore) }
    override fun update(dt: Float) {
        while (inputQueue.isNotEmpty()) {
            handleInputInternal(inputQueue.poll() ?: continue)
        }

        val dtScale = dt * 60f
        if (shakeTimer > 0) shakeTimer -= dtScale

        if (!isGameOver && isGameStarted) {
            physics.update(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, dtScale)
        }

        if (isGameStarted) {
            if (gameMode == GameMode.LINEAR) {
                val p = physics as LinearPhysics
                if (isGameOver) {
                    if (isZoomedOut) {
                        val topY = p.stackList.lastOrNull()?.y ?: 0f
                        val bottomY = p.stackList.firstOrNull()?.y ?: 0f
                        val stackCenterY = (topY + bottomY) / 2f + (BlockConfig.BLOCK_HEIGHT / 2f)
                        cameraSystem.targetCameraOffsetY = (Constants.LOGIC_HEIGHT / 2f) - stackCenterY
                        cameraSystem.targetScale = physics.getCameraScaleTarget(Constants.LOGIC_HEIGHT, true, true)
                    } else {
                        cameraSystem.targetCameraOffsetY = 0f
                        cameraSystem.targetScale = 1f
                    }
                } else {
                    cameraSystem.targetCameraOffsetY = physics.getCameraTargetY()
                    cameraSystem.targetScale = 1f
                }
                cameraSystem.targetCameraOffsetX = 0f

            } else {
                // --- ORBIT MODE ---
                val offX = physics.getCameraTargetX()
                val offY = physics.getCameraTargetY()
                val scaleTarget = physics.getCameraScaleTarget(Constants.LOGIC_HEIGHT, isGameOver, isZoomedOut)

                if (isGameOver) {
                    cameraSystem.targetCameraOffsetX = 0f
                    cameraSystem.targetCameraOffsetY = if (isZoomedOut) 0f else 350f
                    cameraSystem.targetScale = scaleTarget
                } else {
                    cameraSystem.targetCameraOffsetX = offX
                    cameraSystem.targetCameraOffsetY = offY
                    cameraSystem.targetScale = scaleTarget
                }
            }
        }

        cameraSystem.update(dtScale)

        if (feedbackAlpha > 0) {
            feedbackAlpha = (feedbackAlpha - 5 * dtScale).toInt().coerceAtLeast(0)
            feedbackY -= 2f * dtScale
        }
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, this, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if(isDown) inputQueue.add(action)
    }

    private fun handleInputInternal(action: InputAction) {
        if (!isGameStarted) handleStartInput(action)
        else if (isGameOver) handleGameOverInput(action)
        else handleGameInput(action)
    }

    private fun handleStartInput(action: InputAction) {
        when (action) {
            InputAction.LEFT -> {
                if (shopSelectionIndex == -1) toggleMode()
                else if (shopSelectionIndex in 1..3) { shopSelectionIndex--; SoundManager.playSelect() }
            }
            InputAction.RIGHT -> {
                if (shopSelectionIndex == -1) toggleMode()
                else if (shopSelectionIndex < 3) { shopSelectionIndex++; SoundManager.playSelect() }
            }
            InputAction.UP -> {
                if (shopSelectionIndex == 4) shopSelectionIndex = 1
                else if (shopSelectionIndex == -1) shopSelectionIndex = 4
                else shopSelectionIndex = -1
                SoundManager.playSelect()
            }
            InputAction.DOWN -> {
                if (shopSelectionIndex == -1) shopSelectionIndex = 1
                else if (shopSelectionIndex <= 3) shopSelectionIndex = 4
                SoundManager.playSelect()
            }
            InputAction.SELECT -> {
                if (shopSelectionIndex == 4) startGame()
                else if (shopSelectionIndex in 0..3) {
                    if (BlockEconomy.buyItem(AbilityType.entries[shopSelectionIndex])) SoundManager.playPlaceVariation()
                    else SoundManager.playSound(SoundManager.sfxError)
                } else if (shopSelectionIndex == -1) toggleMode()
            }
            InputAction.BACK -> SceneManager.switchScene(HubScene())
        }
    }

    private fun toggleMode() {
        val newMode = if (gameMode == GameMode.LINEAR) GameMode.ORBIT else GameMode.LINEAR
        setMode(newMode)
        loadData()
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        SoundManager.playSelect()
    }

    private fun startGame() {
        isGameStarted = true; isGameOver = false; score = 0; runCoins = 0
        perfectCount = 0; greatCount = 0; niceCount = 0; perfectStreak = 0
        activeSloMoTurns = 0; isMagnetActive = false; isWidenerActive = false; secondChanceUsed = false
        cameraSystem.reset()
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        SoundManager.playPerfect(1)
    }

    private fun handleGameInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> processPlacement()
            InputAction.LEFT -> triggerSloMo()
            InputAction.RIGHT -> triggerWidener()
            InputAction.UP -> triggerMagnet()
            else -> {}
        }
    }

    private fun handleGameOverInput(action: InputAction) {
        if (System.currentTimeMillis() - gameOverTimestamp < BlockConfig.GAME_OVER_COOLDOWN_MS) return
        when (action) {
            InputAction.SELECT -> startGame()
            InputAction.DOWN -> {
                if (score > 0) isZoomedOut = !isZoomedOut
            }
            InputAction.BACK -> {
                saveData()
                isGameStarted = false; isGameOver = false; shopSelectionIndex = 4
                isZoomedOut = false
            }
            else -> {}
        }
    }

    private fun processPlacement() {
        val result = physics.calculatePlacement(score, isMagnetActive, isWidenerActive)
        if (result.type == PlacementType.MISS) {
            val hasRevive = BlockEconomy.useItem(AbilityType.SECOND_CHANCE)
            if (hasRevive && !secondChanceUsed) {
                secondChanceUsed = true
                physics.applyRevive(Constants.LOGIC_WIDTH)
                triggerFeedback("SAVED!", Color.YELLOW, "")
                SoundManager.playPerfect(1)
                isWidenerActive = false
                return
            }
            if (score > highScore) highScore = score
            saveData()
            SoundManager.playGameOver()
            isGameOver = true
            gameOverTimestamp = System.currentTimeMillis()
            shakeTimer = BlockConfig.SHAKE_DURATION.toFloat()
            return
        }
        score++
        var coinGain = 0
        if (result.type == PlacementType.PERFECT) {
            perfectStreak++; perfectCount++
            coinGain = BlockConfig.COIN_REWARD_PERFECT_BASE + perfectStreak
            SoundManager.playPerfect(perfectStreak)
            triggerFeedback("PERFECT", BlockConfig.COLOR_PERFECT, "+$coinGain")
        } else {
            perfectStreak = 0; SoundManager.playPlaceVariation()
            if (result.accuracy >= BlockConfig.THRESHOLD_GREAT) {
                coinGain = BlockConfig.COIN_REWARD_GREAT; greatCount++
                triggerFeedback("GREAT", BlockConfig.COLOR_GREAT, "+$coinGain")
            } else if (result.accuracy >= BlockConfig.THRESHOLD_NICE) {
                coinGain = BlockConfig.COIN_REWARD_NICE; niceCount++
                triggerFeedback("NICE", BlockConfig.COLOR_NICE, "+$coinGain")
            }
        }
        runCoins += coinGain
        BlockEconomy.addCoins(coinGain)
        isMagnetActive = false; if (activeSloMoTurns > 0) activeSloMoTurns--; isWidenerActive = false
        physics.spawnNewBlock(Constants.LOGIC_WIDTH, score, activeSloMoTurns)
    }

    private fun triggerSloMo() { if (activeSloMoTurns == 0 && BlockEconomy.useItem(AbilityType.SLO_MO)) { activeSloMoTurns = BlockConfig.SLOMO_DURATION_TURNS; physics.updateSpeed(score, activeSloMoTurns); triggerFeedback("SLO-MO", Color.CYAN, ""); SoundManager.playPerfect(1) } }
    private fun triggerMagnet() { if (!isMagnetActive && BlockEconomy.useItem(AbilityType.MAGNET)) { isMagnetActive = true; triggerFeedback("MAGNET", Color.MAGENTA, ""); SoundManager.playPerfect(1) } }
    private fun triggerWidener() { if (!isWidenerActive && BlockEconomy.getInventoryCount(AbilityType.WIDENER) > 0 && physics.triggerWidener(Constants.LOGIC_WIDTH)) { BlockEconomy.useItem(AbilityType.WIDENER); isWidenerActive = true; triggerFeedback("WIDEN", Color.GREEN, ""); SoundManager.playPerfect(1) } else if (!isWidenerActive) SoundManager.playSound(SoundManager.sfxError) }
    private fun triggerFeedback(text: String, color: Int, subText: String) { feedbackText = text; feedbackSubText = subText; feedbackColor = color; feedbackAlpha = 255; feedbackY = 0f }
    override fun exit() {}
}