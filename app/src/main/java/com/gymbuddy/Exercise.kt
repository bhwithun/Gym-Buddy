package com.gymbuddy

import java.io.Serializable

data class Exercise(
    val title: String,
    val weight: String,
    var reps: Int,
    var sets: Int,
    val notes: String = "",
    var completedSets: Int = 0,
    var isTimerActive: Boolean = false,
    var remainingSeconds: Int = 0
) : Serializable
