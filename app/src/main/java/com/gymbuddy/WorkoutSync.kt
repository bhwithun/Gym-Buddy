package com.gymbuddy

import java.util.concurrent.CopyOnWriteArrayList

object WorkoutSync {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyWorkoutChanged() {
        listeners.forEach { listener ->
            try {
                listener()
            } catch (_: Exception) {
            }
        }
    }
}