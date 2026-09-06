package com.gymbuddy

object ExerciseTimers {
    fun cooldownSeconds(rating: String): Int = when (rating) {
        "easy" -> 19
        "good" -> 39
        "hard" -> 59
        else -> 59
    }
}