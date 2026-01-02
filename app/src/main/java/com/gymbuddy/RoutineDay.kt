package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "routine_days")
data class RoutineDayEntity(
    @PrimaryKey val dayOfWeek: Int,
    val isRest: Boolean,
    val exercisesJson: String
) : Serializable

data class RoutineDay(
    val dayOfWeek: Int,
    val isRest: Boolean,
    val exercises: List<Exercise>
)
