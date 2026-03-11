package com.gymbuddy

import java.io.Serializable

data class ExerciseWrapper(
    val index: Int,
    val exercise: Exercise,
    val status: String // "updated", "canceled", "created"
) : Serializable
