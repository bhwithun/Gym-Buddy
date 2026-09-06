package com.gymbuddy

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TodayWorkout(
    val exercises: List<Exercise>,
    val isRest: Boolean,
    val hasRoutine: Boolean
)

object WorkoutRepository {
    private val gson = Gson()
    private val exerciseListType = object : TypeToken<List<Exercise>>() {}.type

    fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun loadOrCreateToday(context: Context): TodayWorkout {
        val db = AppDatabase.getDatabase(context)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val routine = db.routineDao().getByDayOfWeekSync(dayOfWeek)
            ?: return TodayWorkout(emptyList(), isRest = false, hasRoutine = false)

        if (routine.isRest) {
            return TodayWorkout(emptyList(), isRest = true, hasRoutine = true)
        }

        val dateStr = todayDateString()
        val existing = db.workoutLogDao().getByDate(dateStr)
        if (existing != null) {
            val logged: List<Exercise> = gson.fromJson(existing.loggedJson, exerciseListType) ?: emptyList()
            return TodayWorkout(logged, isRest = false, hasRoutine = true)
        }

        val exercises = routine.exercises
        saveTodayExercises(context, exercises)
        return TodayWorkout(exercises, isRest = false, hasRoutine = true)
    }

    fun saveTodayExercises(context: Context, exercises: List<Exercise>) {
        val dateStr = todayDateString()
        val plannedJson = gson.toJson(exercises.map { it.copy(completedSets = 0) })
        val loggedJson = gson.toJson(exercises)
        AppDatabase.getDatabase(context).workoutLogDao().insert(
            WorkoutLogEntity(dateStr, plannedJson, loggedJson)
        )
    }
}