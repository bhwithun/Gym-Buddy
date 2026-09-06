package com.gymbuddy

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object WorkoutClock {
    private const val PREFS = "workout_clock"
    private const val KEY_DATE = "date"
    private const val KEY_START = "start_ms"
    private const val KEY_END = "end_ms"
    private const val KEY_SHOWN = "summary_shown"
    private const val SUMMARY_REQUEST = 7101

    const val EXTRA_START_MS = "summary_start_ms"
    const val EXTRA_END_MS = "summary_end_ms"
    const val EXTRA_EXERCISE_COUNT = "summary_exercise_count"
    const val EXTRA_SET_COUNT = "summary_set_count"

    enum class Event { SET_COMPLETE, SET_UNDONE, LOAD }

    data class Summary(
        val startMs: Long,
        val endMs: Long,
        val durationMs: Long,
        val exerciseCount: Int,
        val setCount: Int
    )

    fun sync(context: Context, exercises: List<Exercise>, event: Event): Boolean {
        if (exercises.isEmpty()) return false
        val prefs = prefs(context)
        rollDate(prefs)
        val anyProgress = exercises.any { it.completedSets > 0 }
        val allDone = exercises.all { it.sets <= 0 || it.completedSets >= it.sets }
        val now = System.currentTimeMillis()

        when (event) {
            Event.SET_COMPLETE -> {
                if (prefs.getLong(KEY_START, 0L) == 0L) {
                    prefs.edit().putLong(KEY_START, now).commit()
                }
                if (allDone) {
                    if (prefs.getLong(KEY_END, 0L) == 0L) {
                        prefs.edit().putLong(KEY_END, now).commit()
                    }
                } else {
                    prefs.edit().putLong(KEY_END, 0L).putBoolean(KEY_SHOWN, false).commit()
                }
            }
            Event.SET_UNDONE -> {
                if (!anyProgress) {
                    clearTimes(prefs)
                } else {
                    prefs.edit().putLong(KEY_END, 0L).putBoolean(KEY_SHOWN, false).commit()
                }
            }
            Event.LOAD -> {
                if (!anyProgress) {
                    clearTimes(prefs)
                }
                // Do not clear a recorded finish on load — the log can lag behind
                // the last set and would otherwise wipe gym time for makeup sessions.
            }
        }

        val start = prefs.getLong(KEY_START, 0L)
        val end = prefs.getLong(KEY_END, 0L)
        if (start == 0L || end == 0L) return false
        if (event != Event.LOAD && !allDone) return false
        return !prefs.getBoolean(KEY_SHOWN, false)
    }

    fun summary(context: Context, exercises: List<Exercise>): Summary? {
        val prefs = prefs(context)
        rollDate(prefs)
        val start = prefs.getLong(KEY_START, 0L)
        val end = prefs.getLong(KEY_END, 0L)
        if (start == 0L || end == 0L) return null
        return Summary(
            startMs = start,
            endMs = end,
            durationMs = max(0L, end - start),
            exerciseCount = exercises.size,
            setCount = exercises.sumOf { it.completedSets.coerceAtLeast(0) }
        )
    }

    fun markPresented(context: Context) {
        prefs(context).edit().putBoolean(KEY_SHOWN, true).commit()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().commit()
    }

    fun presentIfNeeded(context: Context, exercises: List<Exercise>, event: Event) {
        if (!sync(context, exercises, event)) return
        val summary = summary(context, exercises) ?: run {
            val prefs = prefs(context)
            val start = prefs.getLong(KEY_START, 0L)
            val end = prefs.getLong(KEY_END, 0L)
            if (start == 0L || end == 0L) return
            Summary(start, end, max(0L, end - start), exercises.size, exercises.sumOf { it.completedSets })
        }
        launchSummary(context, summary)
    }

    fun launchSummary(context: Context, summary: Summary) {
        val intent = Intent(context, WorkoutSummaryActivity::class.java).apply {
            putExtra(EXTRA_START_MS, summary.startMs)
            putExtra(EXTRA_END_MS, summary.endMs)
            putExtra(EXTRA_EXERCISE_COUNT, summary.exerciseCount)
            putExtra(EXTRA_SET_COUNT, summary.setCount)
        }
        try {
            if (context is Activity) {
                context.startActivity(intent)
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                summaryPendingIntent(context, intent).send()
            }
        } catch (_: Exception) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                summaryPendingIntent(context, intent).send()
            } catch (_: Exception) {
            }
        }
    }

    private fun summaryPendingIntent(context: Context, intent: Intent): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, SUMMARY_REQUEST, intent, flags)
    }

    fun updateTimes(context: Context, startMs: Long, endMs: Long) {
        val start = max(0L, startMs)
        val end = max(start, endMs)
        prefs(context).edit()
            .putLong(KEY_START, start)
            .putLong(KEY_END, end)
            .commit()
    }

    fun durationParts(ms: Long): Triple<Int, Int, Int> {
        val totalSec = max(0L, ms / 1000L)
        val h = (totalSec / 3600L).toInt()
        val m = ((totalSec % 3600L) / 60L).toInt()
        val s = (totalSec % 60L).toInt()
        return Triple(h, m, s)
    }

    fun durationFromParts(hours: Int, minutes: Int, seconds: Int): Long {
        val h = hours.coerceIn(0, 23)
        val m = minutes.coerceIn(0, 59)
        val s = seconds.coerceIn(0, 59)
        return ((h * 3600L) + (m * 60L) + s) * 1000L
    }

    fun formatDuration(ms: Long): String {
        val totalSec = max(0L, ms / 1000L)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    fun formatDurationWords(ms: Long): String {
        val totalSec = max(0L, ms / 1000L)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val parts = mutableListOf<String>()
        if (h > 0) parts.add(if (h == 1L) "1 hour" else "$h hours")
        if (m > 0) parts.add(if (m == 1L) "1 minute" else "$m minutes")
        if (h == 0L && (s > 0L || m == 0L)) parts.add(if (s == 1L) "1 second" else "$s seconds")
        return parts.joinToString(" ")
    }

    fun formatClock(ms: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun rollDate(prefs: android.content.SharedPreferences) {
        val today = WorkoutRepository.todayDateString()
        if (prefs.getString(KEY_DATE, null) != today) {
            prefs.edit().clear().putString(KEY_DATE, today).commit()
        }
    }

    private fun clearTimes(prefs: android.content.SharedPreferences) {
        prefs.edit()
            .putLong(KEY_START, 0L)
            .putLong(KEY_END, 0L)
            .putBoolean(KEY_SHOWN, false)
            .commit()
    }
}