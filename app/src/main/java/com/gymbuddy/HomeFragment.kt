package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.FragmentHomeBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val calendar = Calendar.getInstance()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // Current streak
            val currentStreak = calculateCurrentStreak(db)
            binding.currentStreakText.text = "Current Streak: $currentStreak days"

            // Longest streak
            val longestStreak = calculateLongestStreak(db)
            binding.longestStreakText.text = "Longest Streak: $longestStreak days"

            // Monthly workouts
            val monthlyWorkouts = countWorkoutsThisMonth(db)
            binding.monthlyWorkoutsText.text = "Workouts this month: $monthlyWorkouts"

            // Yearly workouts
            val yearlyWorkouts = countWorkoutsThisYear(db)
            binding.yearlyWorkoutsText.text = "Workouts this year: $yearlyWorkouts"

            // Today's exercises
            val todayExercises = getTodayExercises(db)
            binding.todayExercisesText.text = todayExercises.joinToString("\n") { it.title }
        }
    }

    private suspend fun calculateCurrentStreak(db: AppDatabase): Int {
        var streak = 0
        val calendar = Calendar.getInstance()
        while (true) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val routineDay = withContext(Dispatchers.IO) {
                db.routineDao().getByDayOfWeek(dayOfWeek)
            }
            val isValidDay = routineDay != null && !routineDay.isRest
            val hasLog = withContext(Dispatchers.IO) {
                db.workoutLogDao().getByDate(dateStr) != null
            }
            if (isValidDay && hasLog) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            } else if (isValidDay) {
                // Skipped workout day, break streak
                break
            } else {
                // Rest day, continue streak
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
        }
        return streak
    }

    private suspend fun calculateLongestStreak(db: AppDatabase): Int {
        // For simplicity, return current streak as longest for now
        // In a real app, track historical streaks
        return calculateCurrentStreak(db)
    }

    private suspend fun countWorkoutsThisMonth(db: AppDatabase): Int {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val logs = withContext(Dispatchers.IO) {
            db.workoutLogDao().getAll()
        }
        return logs.count { log ->
            val logCalendar = Calendar.getInstance()
            logCalendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(log.date) ?: Date()
            logCalendar.get(Calendar.YEAR) == year && logCalendar.get(Calendar.MONTH) == month
        }
    }

    private suspend fun countWorkoutsThisYear(db: AppDatabase): Int {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val logs = withContext(Dispatchers.IO) {
            db.workoutLogDao().getAll()
        }
        return logs.count { log ->
            val logCalendar = Calendar.getInstance()
            logCalendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(log.date) ?: Date()
            logCalendar.get(Calendar.YEAR) == year
        }
    }

    private suspend fun getTodayExercises(db: AppDatabase): List<Exercise> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val routineDay = withContext(Dispatchers.IO) {
            db.routineDao().getByDayOfWeek(today)
        }
        return routineDay?.exercises ?: emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
