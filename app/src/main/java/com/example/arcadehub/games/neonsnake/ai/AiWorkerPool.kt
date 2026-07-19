package com.example.arcadehub.games.neonsnake.ai

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object AiWorkerPool {
    @Volatile private var executor: java.util.concurrent.ExecutorService? = null
    @Volatile private var poolSize: Int = 0
    private val threadCounter = AtomicInteger(0)

    private val threadFactory = ThreadFactory { runnable ->
        Thread(runnable, "snake-ai-worker-${threadCounter.getAndIncrement()}").apply { isDaemon = true }
    }

    @Synchronized
    private fun ensureCapacity(threads: Int): java.util.concurrent.ExecutorService {
        val existing = executor
        if (existing != null && poolSize >= threads) return existing
        existing?.shutdown()
        val created = Executors.newFixedThreadPool(threads, threadFactory)
        executor = created
        poolSize = threads
        return created
    }

    fun runAll(threadCount: Int, body: (threadId: Int) -> Unit) {
        if (threadCount <= 1) {
            body(0)
            return
        }

        val exec = ensureCapacity(threadCount)
        val futures = ArrayList<Future<*>>(threadCount)
        for (id in 0 until threadCount) {
            futures.add(exec.submit { body(id) })
        }
        for (f in futures) f.get()
    }
}
