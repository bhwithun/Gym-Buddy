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
        val short = dayName.trim().take(3)
        val exact = dayNames.indexOfFirst { it.equals(short, ignoreCase = true) }
        return if (exact >= 0) exact + 1 else 0
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

object RoutineExport {
    fun gson(): Gson = GsonBuilder()
        .registerTypeAdapter(ExportRoutineDay::class.java, ExportRoutineDayDeserializer())
        .registerTypeAdapter(ExportExercise::class.java, ExportExerciseDeserializer())
        .create()

    fun fromEntities(days: List<RoutineDayEntity>): List<ExportRoutineDay> =
        days.map { day ->
            ExportRoutineDay(
                dayOfWeek = DayUtils.getDayName(day.dayOfWeek),
                exercises = day.exercises.map { exercise ->
                    ExportExercise(
                        title = exercise.title,
                        weight = exercise.weight,
                        reps = exercise.reps,
                        sets = exercise.sets,
                        notes = exercise.notes,
                        easyGoodOrHard = exercise.rating
                    )
                }
            )
        }

    fun toEntities(exportDays: List<ExportRoutineDay>): List<RoutineDayEntity>? {
        if (exportDays.size != 7) return null
        val entities = exportDays.map { exportDay ->
            val dayOfWeekInt = DayUtils.getDayOfWeek(exportDay.dayOfWeek)
            if (dayOfWeekInt !in 1..7) return null
            val exercises = exportDay.exercises.map { exportExercise ->
                Exercise(
                    title = exportExercise.title,
                    weight = exportExercise.weight,
                    reps = exportExercise.reps,
                    sets = exportExercise.sets,
                    notes = exportExercise.notes,
                    rating = exportExercise.easyGoodOrHard.ifBlank { "good" }
                )
            }
            RoutineDayEntity(dayOfWeekInt, exercises.isEmpty(), exercises)
        }
        if (entities.map { it.dayOfWeek }.toSet().size != 7) return null
        return entities.sortedBy { it.dayOfWeek }
    }
}

data class ExportRoutineDay(
    val dayOfWeek: String,
    val exercises: List<ExportExercise>
)

class ExportRoutineDayDeserializer : JsonDeserializer<ExportRoutineDay> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ExportRoutineDay {
        val jsonObject = json.asJsonObject

        val dayElement = jsonObject.get("dayOfWeek")
        val dayOfWeekStr = when {
            dayElement == null || dayElement.isJsonNull -> "Mon"
            dayElement.isJsonPrimitive && dayElement.asJsonPrimitive.isNumber -> {
                DayUtils.getDayName(dayElement.asInt)
            }
            else -> dayElement.asString ?: "Mon"
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

class ExportExerciseDeserializer : JsonDeserializer<ExportExercise> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ExportExercise {
        val obj = json.asJsonObject
        val title = obj.get("title")?.asString ?: ""
        val notes = obj.get("notes")?.asString ?: ""
        val rating = obj.get("easyGoodOrHard")?.asString
            ?: obj.get("rating")?.asString
            ?: "good"
        return ExportExercise(
            title = title,
            weight = intField(obj, "weight", 0),
            reps = intField(obj, "reps", 10),
            sets = intField(obj, "sets", 3),
            notes = notes,
            easyGoodOrHard = rating
        )
    }

    private fun intField(obj: com.google.gson.JsonObject, name: String, default: Int): Int {
        val element = obj.get(name) ?: return default
        if (!element.isJsonPrimitive) return default
        val primitive = element.asJsonPrimitive
        if (primitive.isNumber) return primitive.asNumber.toInt()
        if (primitive.isString) return primitive.asString.toDoubleOrNull()?.toInt() ?: default
        return default
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
        val timerDurationSeconds = jsonObject.get("timerDurationSeconds")?.asInt ?: 0

        return Exercise(title, weight, reps, sets, notes, completedSets, isTimerActive, remainingSeconds, timerEndTime, rating, timerDurationSeconds)
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
