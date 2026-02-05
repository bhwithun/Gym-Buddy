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

// Export-friendly classes without transient data
data class ExportExercise(
    val title: String,
    val weight: Int,
    val reps: Int,
    val sets: Int,
    val notes: String
)

data class ExportRoutineDay(
    val dayOfWeek: Int,
    val isRest: Boolean,
    val exercises: List<ExportExercise>
)

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

        return Exercise(title, weight, reps, sets, notes, completedSets, isTimerActive, remainingSeconds)
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
