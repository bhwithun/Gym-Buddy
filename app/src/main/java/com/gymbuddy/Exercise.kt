package com.gymbuddy

import java.io.Serializable

data class Exercise(
    val title: String,
    var weight: Int,
    var reps: Int,
    var sets: Int,
    val notes: String = "",
    var completedSets: Int = 0,
    var isTimerActive: Boolean = false,
    var remainingSeconds: Int = 0,
    var timerEndTime: Long = 0
) : Serializable
