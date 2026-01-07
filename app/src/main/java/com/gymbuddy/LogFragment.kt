package com.gymbuddy

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.FragmentLogBinding
import com.google.gson.Gson
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendarView()

        binding.todayButton.setOnClickListener {
            // Go to today's date using local timezone
            val today = Calendar.getInstance()
            val todayDay = CalendarDay.from(today)
            binding.calendarView.setCurrentDate(todayDay, true)
        }
    }

    private fun setupCalendarView() {
        // Set up date selection listener
        binding.calendarView.setOnDateChangedListener { widget, date, selected ->
            if (selected) {
                // Use CalendarDay's built-in date Calendar object to match workout log format
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.date.time)

                // Show summary dialog
                val dialog = DaySummaryDialogFragment.newInstance(dateStr)
                dialog.show(parentFragmentManager, "day_summary")
            }
        }

        // Load workout data and apply decorators
        loadWorkoutDecorators()
    }

    private fun loadWorkoutDecorators() {
        lifecycleScope.launch {
            val decorators = mutableListOf<DayViewDecorator>()

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Set to first day of month
            calendar.set(currentYear, currentMonth, 1)
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()

            val fullDays = mutableListOf<CalendarDay>()
            val partialDays = mutableListOf<CalendarDay>()
            val skippedDays = mutableListOf<CalendarDay>()
            val restDays = mutableListOf<CalendarDay>()

            for (day in 1..daysInMonth) {
                calendar.set(currentYear, currentMonth, day)
                val date = calendar.time
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val calendarDay = CalendarDay.from(calendar)

                val status = if (calendar.after(today)) {
                    "future"
                } else {
                    val routineDay = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(dayOfWeek)
                    }

                    if (routineDay == null || routineDay.isRest) {
                        "rest"
                    } else {
                        val log = withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(dateStr)
                        }
                        if (log != null) {
                            val exercises = Gson().fromJson(log.loggedJson, Array<Exercise>::class.java)
                            val totalSets = exercises.sumOf { it.sets }
                            val completedSets = exercises.sumOf { it.completedSets }
                            when {
                                completedSets >= totalSets -> "full"
                                completedSets > 0 -> "partial"
                                else -> "skipped"
                            }
                        } else {
                            "skipped"
                        }
                    }
                }

                when (status) {
                    "full" -> fullDays.add(calendarDay)
                    "partial" -> partialDays.add(calendarDay)
                    "skipped" -> skippedDays.add(calendarDay)
                    "rest" -> restDays.add(calendarDay)
                }
            }

            // Add decorators
            if (fullDays.isNotEmpty()) {
                decorators.add(ColorDecorator(fullDays, Color.GREEN))
            }
            if (partialDays.isNotEmpty()) {
                decorators.add(ColorDecorator(partialDays, Color.YELLOW))
            }
            if (skippedDays.isNotEmpty()) {
                decorators.add(ColorDecorator(skippedDays, Color.RED))
            }
            if (restDays.isNotEmpty()) {
                decorators.add(ColorDecorator(restDays, Color.GRAY))
            }

            // Apply decorators to calendar
            for (decorator in decorators) {
                binding.calendarView.addDecorator(decorator)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Decorator class for coloring dates
    private class ColorDecorator(
        private val dates: Collection<CalendarDay>,
        private val color: Int
    ) : DayViewDecorator {

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            // Set background drawable to color the entire tile
            view.setBackgroundDrawable(ColorDrawable(color))
            // Make sure text is white for visibility on colored backgrounds
            if (color != Color.TRANSPARENT) {
                view.addSpan(android.text.style.ForegroundColorSpan(Color.WHITE))
            }
        }
    }
}
