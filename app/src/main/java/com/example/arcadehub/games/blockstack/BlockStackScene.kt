package com.example.arcadehub.games.blockstack

import android.graphics.Canvas
import android.graphics.Color
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.managers.SaveManager
import com.example.arcadehub.managers.SoundManager
import java.util.concurrent.ConcurrentLinkedQueue

class BlockStackScene : BaseGameScene() {

    private val renderer = BlockRenderer()
    val cameraSystem = CameraSystem()
    var gameMode: GameMode = GameMode.LINEAR
    lateinit var physics: PhysicsStrategy

    // Dynamic key based on current mode
    override val highScoreKey: String
        get() = "BS_HIGHSCORE_${gameMode.name}"

    // Visual & Economy State
    var isZoomedOut = false
    var shopSelectionIndex = 4
    var runCoins = 0
    var gameOverTimestamp = 0L

    // Gameplay Counters
    var activeSloMoTurns = 0
    var isMagnetActive = false
    var isWidenerActive = false
    var secondChanceUsed = false
    var perfectCount = 0
    var greatCount = 0
    var niceCount = 0
    var perfectStreak = 0

    // Feedback UI
    var feedbackText = ""
    var feedbackSubText = ""
    var feedbackColor = Color.WHITE
    var feedbackAlpha = 0
    var feedbackY = 0f
    var shakeTimer = 0f

    // Input Queue to sync input with the specific physics update tick
    private val inputQueue = ConcurrentLinkedQueue<InputAction>()

    override fun enter() {
        setMode(GameMode.LINEAR)
        super.enter() // Loads high score and calls resetGame()
    }

    private fun setMode(mode: GameMode) {
        gameMode = mode
        physics = when (mode) {
            GameMode.LINEAR -> LinearPhysics()
            GameMode.ORBIT -> RadialPhysics()
        }
    }

    /**
     * Called by BaseGameScene.enter() or when returning from Game Over to Menu.
     * Sets the state to "Shop/Menu" mode.
     */
    override fun resetGame() {
        isGameStarted = false
        isGameOver = false
        isPaused = false
        shopSelectionIndex = 4

        // Reset physics for the background visualization
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
    }

    /**
     * Transition from Shop to actual Gameplay.
     */
    private fun startActualGameplay() {
        isGameStarted = true
        isGameOver = false
        isPaused = false

        score = 0
        runCoins = 0
        perfectCount = 0
        greatCount = 0
        niceCount = 0
        perfectStreak = 0

        activeSloMoTurns = 0
        isMagnetActive = false
        isWidenerActive = false
        secondChanceUsed = false

        cameraSystem.reset()
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        SoundManager.playPerfect(1) // Start sound
    }

    override fun update(dt: Float) {
        if (isPaused) return

        // Process Queued Inputs
        while (inputQueue.isNotEmpty()) {
            handleGameInputInternal(inputQueue.poll() ?: continue)
        }

        val dtScale = dt * 60f
        if (shakeTimer > 0) shakeTimer -= dtScale

        // Update Physics
        if (!isGameOver && isGameStarted) {
            physics.update(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT, dtScale)
        }

        updateCamera(dtScale)

        // Update Feedback Text Animation
        if (feedbackAlpha > 0) {
            feedbackAlpha = (feedbackAlpha - 5 * dtScale).toInt().coerceAtLeast(0)
            feedbackY -= 2f * dtScale
        }
    }

    private fun updateCamera(dtScale: Float) {
        if (isGameStarted) {
            if (gameMode == GameMode.LINEAR) {
                val p = physics as LinearPhysics
                if (isGameOver) {
                    if (isZoomedOut) {
                        // Center camera on the whole stack
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
                    // Follow top of stack
                    cameraSystem.targetCameraOffsetY = physics.getCameraTargetY()
                    cameraSystem.targetScale = 1f
                }
                cameraSystem.targetCameraOffsetX = 0f

            } else {
                // Radial Camera
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
    }

    override fun draw(canvas: Canvas) {
        renderer.draw(canvas, this, physics, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
    }

    // --- INPUT HANDLING ---

    /**
     * Handled by BaseGameScene logic when isGameStarted == false
     */
    override fun handleStartInput(action: InputAction) {
        when (action) {
            InputAction.LEFT -> {
                if (shopSelectionIndex == -1) toggleMode()
                else if (shopSelectionIndex in 1..3) {
                    shopSelectionIndex--
                    SoundManager.playSelect()
                }
            }
            InputAction.RIGHT -> {
                if (shopSelectionIndex == -1) toggleMode()
                else if (shopSelectionIndex < 3) {
                    shopSelectionIndex++
                    SoundManager.playSelect()
                }
            }
            InputAction.UP -> {
                // Cycle between Mode (-1), Shop Row (0-3), and Start Button (4)
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
                if (shopSelectionIndex == 4) {
                    startActualGameplay()
                } else if (shopSelectionIndex in 0..3) {
                    // Buying Items
                    val item = AbilityType.entries[shopSelectionIndex]
                    if (BlockEconomy.buyItem(item)) SoundManager.playPlaceVariation()
                    else SoundManager.playSound(SoundManager.sfxError)
                } else if (shopSelectionIndex == -1) {
                    toggleMode()
                }
            }
            InputAction.BACK -> quitToHub()
        }
    }

    /**
     * Handled by BaseGameScene logic when isGameStarted == true
     */
    override fun handleGameInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return
        inputQueue.add(action)
    }

    /**
     * Processed in update loop to stay in sync with physics
     */
    private fun handleGameInputInternal(action: InputAction) {
        when (action) {
            InputAction.SELECT -> processPlacement()
            InputAction.LEFT -> triggerSloMo()
            InputAction.RIGHT -> triggerWidener()
            InputAction.UP -> triggerMagnet()
            else -> {}
        }
    }

    /**
     * Handled by BaseGameScene logic when isGameOver == true
     */
    override fun handleGameOverInput(action: InputAction) {
        if (System.currentTimeMillis() - gameOverTimestamp < BlockConfig.GAME_OVER_COOLDOWN_MS) return

        when (action) {
            InputAction.SELECT -> startActualGameplay() // Retry
            InputAction.DOWN -> {
                if (score > 0) isZoomedOut = !isZoomedOut
            }
            InputAction.BACK -> {
                checkNewHighScore()
                resetGame() // Return to Shop/Menu
            }
            else -> {}
        }
    }

    // --- GAMEPLAY LOGIC ---

    private fun toggleMode() {
        val newMode = if (gameMode == GameMode.LINEAR) GameMode.ORBIT else GameMode.LINEAR
        setMode(newMode)
        // Refresh high score for the UI
        highScore = SaveManager.getInt(highScoreKey, 0)
        physics.reset(Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        SoundManager.playSelect()
    }

    private fun processPlacement() {
        val result = physics.calculatePlacement(score, isMagnetActive, isWidenerActive)

        // 1. MISS / GAME OVER
        if (result.type == PlacementType.MISS) {
            // Check for Revive
            val hasRevive = BlockEconomy.useItem(AbilityType.SECOND_CHANCE)
            if (hasRevive && !secondChanceUsed) {
                secondChanceUsed = true
                physics.applyRevive(Constants.LOGIC_WIDTH)
                triggerFeedback("SAVED!", Color.YELLOW, "")
                SoundManager.playPerfect(1)
                isWidenerActive = false
                return
            }

            checkNewHighScore()
            SoundManager.playGameOver()
            isGameOver = true
            gameOverTimestamp = System.currentTimeMillis()
            shakeTimer = BlockConfig.SHAKE_DURATION.toFloat()
            return
        }

        // 2. SUCCESSFUL PLACEMENT
        score++
        var coinGain = 0

        if (result.type == PlacementType.PERFECT) {
            perfectStreak++
            perfectCount++
            coinGain = BlockConfig.COIN_REWARD_PERFECT_BASE + perfectStreak
            SoundManager.playPerfect(perfectStreak)
            triggerFeedback("PERFECT", BlockConfig.COLOR_PERFECT, "+$coinGain")
        } else {
            perfectStreak = 0
            SoundManager.playPlaceVariation()

            if (result.accuracy >= BlockConfig.THRESHOLD_GREAT) {
                coinGain = BlockConfig.COIN_REWARD_GREAT
                greatCount++
                triggerFeedback("GREAT", BlockConfig.COLOR_GREAT, "+$coinGain")
            } else if (result.accuracy >= BlockConfig.THRESHOLD_NICE) {
                coinGain = BlockConfig.COIN_REWARD_NICE
                niceCount++
                triggerFeedback("NICE", BlockConfig.COLOR_NICE, "+$coinGain")
            }
        }

        // 3. APPLY REWARDS & RESET TURN
        runCoins += coinGain
        BlockEconomy.addCoins(coinGain)

        isMagnetActive = false
        if (activeSloMoTurns > 0) activeSloMoTurns--
        isWidenerActive = false

        physics.spawnNewBlock(Constants.LOGIC_WIDTH, score, activeSloMoTurns)
    }


    private fun triggerSloMo() {
        if (activeSloMoTurns == 0 && BlockEconomy.useItem(AbilityType.SLO_MO)) {
            activeSloMoTurns = BlockConfig.SLOMO_DURATION_TURNS
            physics.updateSpeed(score, activeSloMoTurns)
            triggerFeedback("SLO-MO", Color.CYAN, "")
            SoundManager.playPerfect(1)
        }
    }

    private fun triggerMagnet() {
        if (!isMagnetActive && BlockEconomy.useItem(AbilityType.MAGNET)) {
            isMagnetActive = true
            triggerFeedback("MAGNET", Color.MAGENTA, "")
            SoundManager.playPerfect(1)
        }
    }

    private fun triggerWidener() {
        if (!isWidenerActive && BlockEconomy.getInventoryCount(AbilityType.WIDENER) > 0) {
            if (physics.triggerWidener(Constants.LOGIC_WIDTH)) {
                BlockEconomy.useItem(AbilityType.WIDENER)
                isWidenerActive = true
                triggerFeedback("WIDEN", Color.GREEN, "")
                SoundManager.playPerfect(1)
            } else {
                SoundManager.playSound(SoundManager.sfxError)
            }
        } else if (!isWidenerActive) {
            SoundManager.playSound(SoundManager.sfxError)
        }
    }

    private fun triggerFeedback(text: String, color: Int, subText: String) {
        feedbackText = text
        feedbackSubText = subText
        feedbackColor = color
        feedbackAlpha = 255
        feedbackY = 0f
    }
}