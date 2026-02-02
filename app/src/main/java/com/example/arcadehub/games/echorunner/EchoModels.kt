package com.example.arcadehub.games.echorunner

enum class DimensionType { REAL, ECHO }

class Player {
    var x = 0f
    var y = 0f
    var drawX = 0f
    var size = EchoConfig.PLAYER_SIZE
    var isEcho = false
}

class Obstacle {
    var active = false
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f
    var type = DimensionType.REAL
    var passed = false
}

class Particle {
    var active = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var life = 0f
    var color = 0
}