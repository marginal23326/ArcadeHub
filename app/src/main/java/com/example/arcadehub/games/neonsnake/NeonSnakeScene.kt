package com.example.arcadehub.games.neonsnake

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.arcadehub.core.BaseGameScene
import com.example.arcadehub.core.Constants
import com.example.arcadehub.core.GraphicsUtils
import com.example.arcadehub.core.InputAction
import com.example.arcadehub.managers.SoundManager
import kotlin.math.max
import kotlin.math.min

class NeonSnakeScene : BaseGameScene() {

    private val physics = SnakePhysics()
    private val renderer = SnakeRenderer()

    private enum class State { MENU, PLAYING, REPLAY }
    private var currentState = State.MENU

    // Menu Selection
    // 0 = Level Selection
    // 1 = Game Mode Selection
    private var menuRowIndex = 0
    private var selectedLevel = 3
    private var isRobotVsRobotMode = false

    // Replay vars
    private var replayData: List<GameSnapshot> = emptyList()
    private var replayIndex = 0
    private var replayTimer = 0f
    private val REPLAY_SPEED = 0.85f

    override val highScoreKey: String = "SNAKE_HIGHSCORE"
    private var finalScore = 0

    // UI Paints
    private val menuTitlePaint = GraphicsUtils.createPaint(Color.CYAN, textSize = 70f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuValPaint = GraphicsUtils.createPaint(Color.YELLOW, textSize = 90f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val menuSubPaint = GraphicsUtils.createPaint(Color.LTGRAY, textSize = 35f, align = Paint.Align.CENTER)
    private val menuLabelPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 40f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)
    private val statsPaint = GraphicsUtils.createPaint(Color.WHITE, textSize = 35f, align = Paint.Align.CENTER, typeface = Typeface.DEFAULT_BOLD)

    override fun resetGame() {
        currentState = State.MENU
        isGameStarted = false
        isGameOver = false
        isPaused = false
        score = 0
        menuRowIndex = 0
        // Don't reset selectedLevel or isRobotVsRobotMode, keep user preference
    }

    override fun update(dt: Float) {
        when (currentState) {
            State.PLAYING -> {
                if (isPaused) return
                physics.update(dt)
                score = physics.player.score

                if (physics.isGameOver && !isGameOver) {
                    startReplayMode()
                }
            }
            State.REPLAY -> {
                replayTimer += dt
                if (replayTimer >= REPLAY_SPEED) {
                    replayTimer = 0f
                    replayIndex = (replayIndex + 1) % maxOf(1, replayData.size)
                }
            }
            else -> {}
        }
    }

    private fun startReplayMode() {
        this.isGameOver = true
        SoundManager.playGameOver()

        // Only save high score if human is playing
        if (!isRobotVsRobotMode) {
            checkNewHighScore()
        }

        this.finalScore = physics.player.score

        if (physics.gameOverReason == "YOU WON") {
            SnakeStats.recordWin(selectedLevel)
        } else if (physics.gameOverReason == "ROBOT WON") {
            SnakeStats.recordLoss(selectedLevel)
        }

        replayData = physics.getReplayHistory()
        replayIndex = 0
        currentState = State.REPLAY
    }

    override fun draw(canvas: Canvas) {
        when (currentState) {
            State.MENU -> drawLevelMenu(canvas)
            State.PLAYING -> {
                renderer.draw(
                    canvas,
                    physics,
                    Constants.LOGIC_WIDTH,
                    Constants.LOGIC_HEIGHT,
                    highScore,
                    isRobotVsRobotMode
                )
            }
            State.REPLAY -> {
                if (replayData.isNotEmpty()) {
                    renderer.drawReplayFrame(
                        canvas,
                        replayData[replayIndex],
                        Constants.LOGIC_WIDTH,
                        Constants.LOGIC_HEIGHT,
                        physics,
                        finalScore
                    )
                }
            }
        }

        if (isPaused) {
            GraphicsUtils.drawPauseMenu(canvas, Constants.LOGIC_WIDTH, Constants.LOGIC_HEIGHT)
        }
    }

    private fun drawLevelMenu(canvas: Canvas) {
        canvas.drawColor(SnakeConfig.COLOR_BG)
        val cx = Constants.LOGIC_WIDTH / 2f
        val cy = Constants.LOGIC_HEIGHT / 2f

        canvas.drawText("SNAKE SETUP", cx, cy - 180f, menuTitlePaint)

        // --- ROW 0: Level Selection ---
        val levelColor = if (menuRowIndex == 0) Color.YELLOW else Color.DKGRAY
        menuValPaint.color = levelColor
        menuLabelPaint.color = if (menuRowIndex == 0) Color.CYAN else Color.GRAY

        canvas.drawText("DIFFICULTY", cx, cy - 80f, menuLabelPaint)

        val showLeftLvl = if (selectedLevel > 1 && menuRowIndex == 0) "<" else " "
        val showRightLvl = if (selectedLevel < 7 && menuRowIndex == 0) ">" else " "
        canvas.drawText("$showLeftLvl   LEVEL $selectedLevel   $showRightLvl", cx, cy - 10f, menuValPaint)

        // --- ROW 1: Mode Selection ---
        val modeColor = if (menuRowIndex == 1) Color.YELLOW else Color.DKGRAY
        menuValPaint.color = modeColor
        menuLabelPaint.color = if (menuRowIndex == 1) Color.CYAN else Color.GRAY

        canvas.drawText("GAME MODE", cx, cy + 90f, menuLabelPaint)

        val modeText = if (isRobotVsRobotMode) "ROBOT vs ROBOT" else "HUMAN vs ROBOT"
        val showArrowsMode = if (menuRowIndex == 1) "<   $modeText   >" else "    $modeText    "

        // Use slightly smaller font for mode text so it fits
        menuValPaint.textSize = 70f
        canvas.drawText(showArrowsMode, cx, cy + 160f, menuValPaint)
        menuValPaint.textSize = 90f // Restore

        // --- Description (Dynamic) ---
        val description = if (menuRowIndex == 0) {
            when (selectedLevel) {
                1 -> "Novice: The robot is still learning."
                2 -> "Skilled: He's getting the hang of it!"
                3 -> "Expert: A very smart opponent."
                4 -> "Master: He can see into the future!"
                5 -> "Grandmaster: The ultimate challenge!"
                6 -> "Legend: Beyond human comprehension."
                7 -> "Impossible: Victory is a myth."
                else -> ""
            }
        } else {
            if (isRobotVsRobotMode) "Watch two AIs fight! Controls speed." else "You fight the AI!"
        }
        canvas.drawText(description, cx, cy + 260f, menuSubPaint)

        // Stats
        val wins = SnakeStats.getWins(selectedLevel)
        val losses = SnakeStats.getLosses(selectedLevel)
        val statColor = if (wins >= losses) Color.GREEN else Color.RED
        statsPaint.color = statColor
        canvas.drawText("PLAYER WINS: $wins   |   ROBOT WINS: $losses", cx, cy + 320f, statsPaint)

        // Controls
        GraphicsUtils.createPaint(Color.DKGRAY, textSize = 30f, align = Paint.Align.CENTER).also {
            canvas.drawText("UP/DOWN select  |  LEFT/RIGHT change  |  CENTER Play", cx, cy + 380f, it)
        }
    }

    override fun onInput(action: InputAction, isDown: Boolean) {
        if (!isDown) return

        if (currentState == State.MENU) {
            if (action == InputAction.BACK) quitToHub()
            else handleMenuInput(action)
            return
        }

        if (isPaused) {
            when (action) {
                InputAction.SELECT -> { isPaused = false; SoundManager.playSelect() }
                InputAction.BACK -> { resetGame(); SoundManager.playSelect() }
                else -> {}
            }
            return
        }

        if (currentState == State.REPLAY) {
            handleGameOverInput(action)
            return
        }

        if (currentState == State.PLAYING) {
            if (action == InputAction.BACK) {
                pauseGame()
            } else {
                if (isRobotVsRobotMode) {
                    handleRobotSpeedInput(action)
                } else {
                    handleGameInput(action)
                }
            }
        }
    }

    override fun handleGameOverInput(action: InputAction) {
        when (action) {
            InputAction.SELECT -> startGame() // Instant retry with same settings
            InputAction.BACK -> { resetGame(); SoundManager.playSelect() }
            else -> {}
        }
    }

    private fun handleMenuInput(action: InputAction) {
        when (action) {
            InputAction.UP -> {
                menuRowIndex = max(0, menuRowIndex - 1)
                SoundManager.playSelect()
            }
            InputAction.DOWN -> {
                menuRowIndex = min(1, menuRowIndex + 1)
                SoundManager.playSelect()
            }
            InputAction.LEFT -> {
                if (menuRowIndex == 0) {
                    if (selectedLevel > 1) { selectedLevel--; SoundManager.playSelect() }
                } else {
                    isRobotVsRobotMode = !isRobotVsRobotMode
                    SoundManager.playSelect()
                }
            }
            InputAction.RIGHT -> {
                if (menuRowIndex == 0) {
                    if (selectedLevel < 7) { selectedLevel++; SoundManager.playSelect() }
                } else {
                    isRobotVsRobotMode = !isRobotVsRobotMode
                    SoundManager.playSelect()
                }
            }
            InputAction.SELECT -> {
                startGame()
            }
            else -> {}
        }
    }

    private fun handleRobotSpeedInput(action: InputAction) {
        val STEP = 0.05f

        when (action) {
            InputAction.RIGHT -> {
                // Speed up (reduce delay)
                physics.speedDelay = max(0f, physics.speedDelay - STEP)
            }
            InputAction.LEFT -> {
                // Slow down (increase delay)
                physics.speedDelay = min(SnakeConfig.GAME_SPEED_SECONDS, physics.speedDelay + STEP)
            }
            else -> {}
        }
    }

    private fun startGame() {
        physics.setDifficultyLevel(selectedLevel)
        physics.isAutoPilot = isRobotVsRobotMode
        physics.reset()

        isGameOver = false
        currentState = State.PLAYING
        isGameStarted = true
        SoundManager.playPerfect(1)
    }

    override fun handleGameInput(action: InputAction, isDown: Boolean) { }

    private fun handleGameInput(action: InputAction) {
        val dir = when (action) {
            InputAction.UP -> GridDir.UP
            InputAction.DOWN -> GridDir.DOWN
            InputAction.LEFT -> GridDir.LEFT
            InputAction.RIGHT -> GridDir.RIGHT
            else -> null
        }
        dir?.let { physics.processInput(it) }
    }
}