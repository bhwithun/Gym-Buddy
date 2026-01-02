package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

@Entity(tableName = "routine_days")
@TypeConverters(Converters::class)
data class RoutineDayEntity(
    @PrimaryKey val dayOfWeek: Int,
    val isRest: Boolean,
    val exercises: List<Exercise>
) : Serializable

data class RoutineDay(
    val dayOfWeek: Int,
    val isRest: Boolean,
    val exercises: List<Exercise>
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromExerciseList(exercises: List<Exercise>): String {
        return gson.toJson(exercises)
    }

    @TypeConverter
    fun toExerciseList(json: String): List<Exercise> {
        val type = object : TypeToken<List<Exercise>>() {}.type
        return gson.fromJson(json, type)
    }
}
