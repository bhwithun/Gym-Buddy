package com.gymbuddy

import java.io.Serializable

data class Exercise(
    val title: String,
    val weight: String,
    val reps: Int,
    val sets: Int,
    val notes: String = "",
    @Transient var completedSets: Int = 0
) : Serializable
