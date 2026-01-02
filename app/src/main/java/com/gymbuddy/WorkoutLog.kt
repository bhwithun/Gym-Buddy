package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLogEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val plannedJson: String,
    val loggedJson: String
)

data class LoggedExercise(
    val exercise: Exercise,
    val completedSets: Int
)

data class WorkoutLog(
    val date: String,
    val plannedExercises: List<Exercise>,
    val loggedExercises: List<LoggedExercise>
)
