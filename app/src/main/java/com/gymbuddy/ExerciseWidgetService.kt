package com.gymbuddy

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlin.math.min

class ExerciseWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ExerciseWidgetViewsFactory(applicationContext, intent)
    }
}

class ExerciseWidgetViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var exercises: List<Exercise> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        exercises = try {
            WorkoutRepository.loadOrCreateToday(context).exercises
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {
        exercises = emptyList()
    }

    override fun getCount(): Int = exercises.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_exercise_item)
        if (position !in exercises.indices) return views

        val exercise = exercises[position]
        views.setTextViewText(R.id.title_text, exercise.title)
        views.setTextViewText(
            R.id.weight_value,
            if (exercise.weight == 0) "-" else exercise.weight.toString()
        )
        views.setTextViewText(R.id.reps_value, exercise.reps.toString())
        views.setTextViewText(R.id.sets_value, exercise.sets.toString())
        views.setTextViewText(R.id.page_indicator, "${position + 1} / ${exercises.size}")

        val now = System.currentTimeMillis()
        val pulse = ExerciseWidgetPulse.isOn(position)
        val timerFraction = when {
            pulse -> 1f
            ExerciseWidgetPulse.isTracking(position) && ExerciseWidgetPulse.isActive() -> 0f
            exercise.isTimerActive && exercise.timerEndTime > now -> {
                val durationMs = (
                    (if (exercise.timerDurationSeconds > 0) exercise.timerDurationSeconds else exercise.cooldownSeconds())
                    ).coerceAtLeast(1) * 1000L
                ((exercise.timerEndTime - now).toFloat() / durationMs).coerceIn(0f, 1f)
            }
            else -> -1f
        }

        val density = context.resources.displayMetrics.density
        val sizePx = min(360, (160f * density).toInt()).coerceAtLeast(160)
        val bitmap = ProgressPieRenderer.createWidgetBitmap(
            sizePx = sizePx,
            completedSets = exercise.completedSets,
            totalSets = exercise.sets,
            timerFraction = timerFraction,
            pulse = pulse
        )
        views.setImageViewBitmap(R.id.pie_chart, bitmap)
        views.setContentDescription(
            R.id.pie_chart,
            context.getString(R.string.widget_pie_description, exercise.completedSets, exercise.sets)
        )

        val completeIntent = Intent().apply {
            putExtra(ExerciseWidgetProvider.EXTRA_CLICK_TYPE, ExerciseWidgetProvider.CLICK_COMPLETE_SET)
            putExtra(ExerciseWidgetProvider.EXTRA_POSITION, position)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        views.setOnClickFillInIntent(R.id.pie_chart, completeIntent)

        val openIntent = Intent().apply {
            putExtra(ExerciseWidgetProvider.EXTRA_CLICK_TYPE, ExerciseWidgetProvider.CLICK_OPEN_APP)
        }
        views.setOnClickFillInIntent(R.id.title_text, openIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_exercise_item)
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}