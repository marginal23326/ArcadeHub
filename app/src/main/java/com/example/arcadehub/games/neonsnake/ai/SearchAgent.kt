package com.example.arcadehub.games.neonsnake.ai

class SearchAgent {
    val body = FastBody()
    var health: Int = 0

    fun copyOf(): SearchAgent {
        val a = SearchAgent()
        a.body.copyFrom(body)
        a.health = health
        return a
    }

    fun copyFrom(other: SearchAgent) {
        body.copyFrom(other.body)
        health = other.health
    }
}
