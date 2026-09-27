package com.gymbuddy

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

object WorkoutSync {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val changes = AtomicInteger(0)

    fun generation(): Int = changes.get()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyWorkoutChanged() {
        changes.incrementAndGet()
        listeners.forEach { listener ->
            try {
                listener()
            } catch (_: Exception) {
            }
        }
    }
}