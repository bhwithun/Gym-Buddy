package com.gymbuddy

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.gymbuddy.databinding.ActivityWorkoutSummaryBinding
import kotlin.concurrent.thread

class WorkoutSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutSummaryBinding
    private var showingWords = false
    private var durationMs = 0L
    private var summaryData: WorkoutClock.Summary? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        })

        val startMs = intent.getLongExtra(WorkoutClock.EXTRA_START_MS, 0L)
        val endMs = intent.getLongExtra(WorkoutClock.EXTRA_END_MS, 0L)
        val summary = if (startMs > 0L && endMs >= startMs) {
            WorkoutClock.Summary(
                startMs = startMs,
                endMs = endMs,
                durationMs = endMs - startMs,
                exerciseCount = intent.getIntExtra(WorkoutClock.EXTRA_EXERCISE_COUNT, 0),
                setCount = intent.getIntExtra(WorkoutClock.EXTRA_SET_COUNT, 0)
            )
        } else {
            try {
                WorkoutClock.summary(this, WorkoutRepository.loadOrCreateToday(this).exercises)
            } catch (_: Exception) {
                null
            }
        }
        if (summary == null) {
            finish()
            return
        }

        summaryData = summary
        durationMs = summary.durationMs
        binding.timeRange.text = getString(
            R.string.summary_range,
            WorkoutClock.formatClock(summary.startMs),
            WorkoutClock.formatClock(summary.endMs)
        )
        binding.statsLine.text = getString(
            R.string.summary_stats,
            summary.exerciseCount,
            summary.setCount
        )
        renderDuration()

        binding.durationValue.setOnClickListener {
            showingWords = !showingWords
            renderDuration()
        }
        if (WorkerRemote.isConfigured(this)) {
            binding.pushButton.visibility = View.VISIBLE
            binding.pushButton.setOnClickListener { pushToWorker() }
        } else {
            binding.pushButton.visibility = View.GONE
        }
        binding.doneButton.setOnClickListener { close() }
    }

    private fun renderDuration() {
        if (showingWords) {
            binding.durationValue.textSize = 28f
            binding.durationValue.typeface = android.graphics.Typeface.DEFAULT_BOLD
            binding.durationValue.text = WorkoutClock.formatDurationWords(durationMs)
            binding.durationHint.text = getString(R.string.summary_tap_time_digital)
        } else {
            binding.durationValue.textSize = 56f
            binding.durationValue.typeface = ResourcesCompat.getFont(this, R.font.seven_segment)
            binding.durationValue.text = WorkoutClock.formatDuration(durationMs)
            binding.durationHint.text = getString(R.string.summary_tap_time)
        }
    }

    private fun pushToWorker() {
        val summary = summaryData ?: return
        binding.pushButton.isEnabled = false
        binding.pushButton.setText(R.string.summary_pushing)
        thread {
            val result = WorkoutPushClient.push(this, summary)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (result.isSuccess) {
                    binding.pushButton.setText(R.string.summary_pushed)
                } else {
                    binding.pushButton.isEnabled = true
                    binding.pushButton.setText(R.string.summary_push)
                    Toast.makeText(this, R.string.summary_push_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun close() {
        WorkoutClock.markPresented(this)
        finish()
    }
}