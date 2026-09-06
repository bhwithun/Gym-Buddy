package com.gymbuddy

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.RemoteViews
import java.util.concurrent.Executors

class ExerciseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        io.execute {
            bindAll(context)
            ExerciseWidgetTicker.startIfNeeded(context)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        io.execute { bindAll(context) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { ExerciseWidgetState.clear(context, it) }
    }

    override fun onDisabled(context: Context) {
        ExerciseWidgetTicker.stop()
        cancelTimerEndAlarm(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_CLICK -> {
                val pendingResult = goAsync()
                io.execute {
                    try {
                        handleClick(context, intent)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_TIMER_ENDED -> {
                io.execute { ExerciseWidgetTicker.start(context) }
            }
        }
    }

    companion object {
        const val ACTION_CLICK = "com.gymbuddy.widget.CLICK"
        const val ACTION_TIMER_ENDED = "com.gymbuddy.widget.TIMER_ENDED"
        const val EXTRA_CLICK_TYPE = "click_type"
        const val EXTRA_POSITION = "position"
        const val CLICK_COMPLETE_SET = 1
        const val CLICK_OPEN_APP = 2

        internal val io = Executors.newSingleThreadExecutor()

        fun refreshAll(context: Context) {
            io.execute { bindAll(context) }
        }

        fun refreshCollection(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExerciseWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.notifyAppWidgetViewDataChanged(ids, R.id.exercise_flipper)
        }

        internal fun bindAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ExerciseWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val workout = try {
                WorkoutRepository.loadOrCreateToday(context)
            } catch (_: Exception) {
                TodayWorkout(emptyList(), isRest = false, hasRoutine = false)
            }
            ids.forEach { id ->
                manager.updateAppWidget(id, buildRemoteViews(context, id, workout))
                if (workout.exercises.isNotEmpty()) {
                    manager.notifyAppWidgetViewDataChanged(id, R.id.exercise_flipper)
                }
            }
        }

        internal fun tick(context: Context): Boolean {
            val workout = try {
                WorkoutRepository.loadOrCreateToday(context)
            } catch (_: Exception) {
                return false
            }
            val now = System.currentTimeMillis()
            val exercises = workout.exercises.toMutableList()
            var mutated = false
            var keepTicking = ExerciseWidgetPulse.isActive()

            exercises.forEachIndexed { index, exercise ->
                if (!exercise.isTimerActive) return@forEachIndexed
                if (now < exercise.timerEndTime) {
                    keepTicking = true
                    return@forEachIndexed
                }
                if (!ExerciseWidgetPulse.isTracking(index)) {
                    ExerciseWidgetPulse.start(index)
                    keepTicking = true
                }
                if (ExerciseWidgetPulse.isActive()) {
                    keepTicking = true
                } else {
                    exercises[index] = exercise.copy(
                        isTimerActive = false,
                        remainingSeconds = 0,
                        timerEndTime = 0,
                        timerDurationSeconds = 0
                    )
                    mutated = true
                    ExerciseWidgetPulse.clear()
                }
            }

            if (!keepTicking && ExerciseWidgetPulse.exerciseIndex >= 0 && !ExerciseWidgetPulse.isActive()) {
                ExerciseWidgetPulse.clear()
            }

            if (mutated) {
                WorkoutRepository.saveTodayExercises(context, exercises)
                WorkoutSync.notifyWorkoutChanged()
            }

            refreshCollection(context)
            return keepTicking
        }

        private fun handleClick(context: Context, intent: Intent) {
            when (intent.getIntExtra(EXTRA_CLICK_TYPE, 0)) {
                CLICK_OPEN_APP -> {
                    try {
                        context.startActivity(openAppIntent(context))
                    } catch (_: Exception) {
                    }
                }
                CLICK_COMPLETE_SET -> {
                    val position = intent.getIntExtra(EXTRA_POSITION, -1)
                    completeSet(context, position)
                }
            }
        }

        private fun completeSet(context: Context, position: Int) {
            val workout = WorkoutRepository.loadOrCreateToday(context)
            if (position !in workout.exercises.indices) return

            val current = workout.exercises[position]
            val updated = applyPieTap(current)
            val exercises = workout.exercises.toMutableList()
            exercises[position] = updated
            WorkoutRepository.saveTodayExercises(context, exercises)

            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, ExerciseWidgetProvider::class.java)).forEach { id ->
                ExerciseWidgetState.setIndex(context, id, position)
            }

            if (updated.completedSets > current.completedSets && updated.completedSets < updated.sets) {
                vibrate(context)
            } else if (updated.completedSets > current.completedSets) {
                vibrate(context)
            }

            WorkoutSync.notifyWorkoutChanged()
            refreshCollection(context)

            if (updated.isTimerActive && updated.timerEndTime > System.currentTimeMillis()) {
                scheduleTimerEndAlarm(context, updated.timerEndTime)
                ExerciseWidgetTicker.start(context)
            } else {
                ExerciseWidgetPulse.clear()
                cancelTimerEndAlarm(context)
            }
        }

        fun applyPieTap(exercise: Exercise): Exercise {
            if (exercise.completedSets >= exercise.sets) {
                return exercise.copy(
                    completedSets = 0,
                    isTimerActive = false,
                    remainingSeconds = 0,
                    timerEndTime = 0,
                    timerDurationSeconds = 0
                )
            }
            val completed = exercise.completedSets + 1
            if (completed >= exercise.sets) {
                return exercise.copy(
                    completedSets = completed,
                    isTimerActive = false,
                    remainingSeconds = 0,
                    timerEndTime = 0,
                    timerDurationSeconds = 0
                )
            }
            val cooldown = exercise.cooldownSeconds()
            return exercise.copy(
                completedSets = completed,
                isTimerActive = true,
                remainingSeconds = cooldown,
                timerDurationSeconds = cooldown,
                timerEndTime = System.currentTimeMillis() + cooldown * 1000L
            )
        }

        fun buildRemoteViews(context: Context, appWidgetId: Int, workout: TodayWorkout): RemoteViews {
            if (!workout.hasRoutine || workout.isRest || workout.exercises.isEmpty()) {
                val views = RemoteViews(context.packageName, R.layout.widget_exercise_empty)
                val message = when {
                    workout.isRest -> context.getString(R.string.widget_empty_rest)
                    !workout.hasRoutine -> context.getString(R.string.widget_empty_none)
                    else -> context.getString(R.string.widget_empty_none)
                }
                views.setTextViewText(R.id.empty_message, message)
                views.setOnClickPendingIntent(
                    R.id.empty_root,
                    PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        openAppIntent(context),
                        pendingFlags(immutable = true)
                    )
                )
                return views
            }

            val views = RemoteViews(context.packageName, R.layout.widget_exercise)
            val serviceIntent = Intent(context, ExerciseWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("gymbuddy://widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.exercise_flipper, serviceIntent)
            views.setEmptyView(R.id.exercise_flipper, R.id.empty_view)

            val template = Intent(context, ExerciseWidgetProvider::class.java).apply {
                action = ACTION_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setPendingIntentTemplate(
                R.id.exercise_flipper,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    template,
                    pendingFlags(immutable = false)
                )
            )

            val saved = ExerciseWidgetState.getIndex(context, appWidgetId)
            val firstIncomplete = workout.exercises.indexOfFirst { it.completedSets < it.sets }
            val displayIndex = when {
                saved in workout.exercises.indices -> saved
                firstIncomplete >= 0 -> firstIncomplete
                else -> 0
            }
            views.setDisplayedChild(R.id.exercise_flipper, displayIndex)
            return views
        }

        fun openAppIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_WORKOUT, true)
            }
        }

        fun pendingFlags(immutable: Boolean): Int {
            val mutability = if (immutable) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_MUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT or mutability
        }

        private fun vibrate(context: Context) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }

        private fun scheduleTimerEndAlarm(context: Context, endTime: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = timerEndPendingIntent(context)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pending)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pending)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pending)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, pending)
                }
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, endTime, pending)
            }
        }

        private fun cancelTimerEndAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(timerEndPendingIntent(context))
        }

        private fun timerEndPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ExerciseWidgetProvider::class.java).apply {
                action = ACTION_TIMER_ENDED
            }
            return PendingIntent.getBroadcast(context, 0, intent, pendingFlags(immutable = true))
        }
    }
}

object ExerciseWidgetState {
    private const val PREFS = "exercise_widget_state"
    private fun key(appWidgetId: Int) = "index_$appWidgetId"

    fun getIndex(context: Context, appWidgetId: Int): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(key(appWidgetId), -1)
    }

    fun setIndex(context: Context, appWidgetId: Int, index: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(key(appWidgetId), index)
            .apply()
    }

    fun clear(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(appWidgetId))
            .apply()
    }
}

object ExerciseWidgetPulse {
    const val PULSE_DURATION_MS = 1200L
    const val PULSE_STEP_MS = 200L

    @Volatile
    var exerciseIndex: Int = -1
        private set

    @Volatile
    var startTime: Long = 0L
        private set

    fun start(index: Int) {
        exerciseIndex = index
        startTime = SystemClock.elapsedRealtime()
    }

    fun clear() {
        exerciseIndex = -1
        startTime = 0L
    }

    fun isTracking(index: Int): Boolean = exerciseIndex == index && startTime > 0L

    fun isActive(): Boolean {
        if (exerciseIndex < 0 || startTime <= 0L) return false
        return SystemClock.elapsedRealtime() - startTime < PULSE_DURATION_MS
    }

    fun isOn(index: Int): Boolean {
        if (!isTracking(index) || !isActive()) return false
        val elapsed = SystemClock.elapsedRealtime() - startTime
        return (elapsed / PULSE_STEP_MS) % 2L == 0L
    }
}

object ExerciseWidgetTicker {
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()
    @Volatile private var running = false
    @Volatile private var generation = 0
    private var runnable: Runnable? = null

    fun startIfNeeded(context: Context) {
        val workout = try {
            WorkoutRepository.loadOrCreateToday(context)
        } catch (_: Exception) {
            return
        }
        val now = System.currentTimeMillis()
        val needsTick = workout.exercises.any { it.isTimerActive && it.timerEndTime > 0 } ||
            ExerciseWidgetPulse.isActive()
        if (needsTick) {
            if (workout.exercises.any { it.isTimerActive && now >= it.timerEndTime && it.timerEndTime > 0 }) {
                val index = workout.exercises.indexOfFirst { it.isTimerActive && now >= it.timerEndTime }
                if (index >= 0 && !ExerciseWidgetPulse.isTracking(index)) {
                    ExerciseWidgetPulse.start(index)
                }
            }
            start(context)
        }
    }

    fun start(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            generation++
            val gen = generation
            runnable?.let { handler.removeCallbacks(it) }
            running = true
            val next = object : Runnable {
                override fun run() {
                    ExerciseWidgetProvider.io.execute {
                        val keepGoing = try {
                            ExerciseWidgetProvider.tick(app)
                        } catch (_: Exception) {
                            false
                        }
                        synchronized(lock) {
                            if (gen != generation) return@execute
                            if (keepGoing) {
                                handler.postDelayed(this, ExerciseWidgetPulse.PULSE_STEP_MS)
                            } else {
                                running = false
                            }
                        }
                    }
                }
            }
            runnable = next
            handler.post(next)
        }
    }

    fun stop() {
        synchronized(lock) {
            generation++
            runnable?.let { handler.removeCallbacks(it) }
            runnable = null
            running = false
        }
    }
}