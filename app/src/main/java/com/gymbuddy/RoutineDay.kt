package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.io.Serializable
import java.lang.reflect.Type

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

// Day name mapping utility
object DayUtils {
    val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    fun getDayName(dayOfWeek: Int): String {
        return dayNames[dayOfWeek - 1]
    }

    fun getDayOfWeek(dayName: String): Int {
        return dayNames.indexOf(dayName) + 1
    }
}

// Export-friendly classes without transient data
data class ExportExercise(
    val title: String,
    val weight: Int,
    val reps: Int,
    val sets: Int,
    val notes: String,
    val easyGoodOrHard: String
)

data class ExportRoutineDay(
    val dayOfWeek: String,
    val exercises: List<ExportExercise>
)

class ExportRoutineDayDeserializer : JsonDeserializer<ExportRoutineDay> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ExportRoutineDay {
        val jsonObject = json.asJsonObject

        // Handle dayOfWeek - can be Int (old) or String (new)
        val dayOfWeekStr = when {
            jsonObject.get("dayOfWeek").isJsonPrimitive && jsonObject.get("dayOfWeek").asJsonPrimitive.isNumber -> {
                // Old format: dayOfWeek as Int
                val dayOfWeekInt = jsonObject.get("dayOfWeek").asInt
                DayUtils.getDayName(dayOfWeekInt)
            }
            else -> {
                // New format: dayOfWeek as String
                jsonObject.get("dayOfWeek")?.asString ?: "Mon" // default
            }
        }

        // Handle exercises
        val exercisesJson = jsonObject.get("exercises")?.asJsonArray
        val exercises = if (exercisesJson != null) {
            exercisesJson.map { exerciseJson ->
                context.deserialize<ExportExercise>(exerciseJson, ExportExercise::class.java)
            }
        } else {
            emptyList()
        }

        return ExportRoutineDay(dayOfWeekStr, exercises)
    }
}

class ExerciseDeserializer : JsonDeserializer<Exercise> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Exercise {
        val jsonObject = json.asJsonObject

        val title = jsonObject.get("title")?.asString ?: ""
        val weightElement = jsonObject.get("weight")
        val weight = if (weightElement != null && weightElement.isJsonPrimitive && weightElement.asJsonPrimitive.isString) {
            // Old format: weight as string
            val weightStr = weightElement.asString
            weightStr.toIntOrNull() ?: 1
        } else if (weightElement != null) {
            // New format: weight as int
            weightElement.asInt
        } else {
            1 // default
        }
        val reps = jsonObject.get("reps")?.asInt ?: 10
        val sets = jsonObject.get("sets")?.asInt ?: 3
        val notes = jsonObject.get("notes")?.asString ?: ""
        val completedSets = jsonObject.get("completedSets")?.asInt ?: 0
        val isTimerActive = jsonObject.get("isTimerActive")?.asBoolean ?: false
        val remainingSeconds = jsonObject.get("remainingSeconds")?.asInt ?: 0
        val timerEndTime = jsonObject.get("timerEndTime")?.asLong ?: 0L
        val rating = jsonObject.get("rating")?.asString ?: "good"

        return Exercise(title, weight, reps, sets, notes, completedSets, isTimerActive, remainingSeconds, timerEndTime, rating)
    }
}

class Converters {
    private val gson = GsonBuilder()
        .registerTypeAdapter(Exercise::class.java, ExerciseDeserializer())
        .create()

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
