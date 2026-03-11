package com.gymbuddy

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ekn.gruzer.gaugelibrary.Range
import com.gymbuddy.databinding.FragmentHomeBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

            // Monthly workouts
            val monthlyWorkouts = countWorkoutsThisMonth(db)
            binding.monthlyWorkoutsText.text = "Workouts this month: $monthlyWorkouts"

            // Yearly workouts
            val yearlyWorkouts = countWorkoutsThisYear(db)
            binding.yearlyWorkoutsText.text = "This year: $yearlyWorkouts"

            // Total workouts since date
            val (totalWorkouts, sinceDate) = countTotalWorkoutsSince(db)
            val sinceFormatted = if (sinceDate != null) {
                SimpleDateFormat("M/d/yy", Locale.getDefault()).format(sinceDate)
            } else {
                "start"
            }
            binding.totalWorkoutsText.text = "Since $sinceFormatted: $totalWorkouts"

            // Dedication
            val dedication = calculateDedication(db, sinceDate)
            setupDedicationGauge()
            binding.dedicationGauge.setValue(dedication.toDouble())
            binding.dedicationText.text = "Dedication: ${dedication}%"
        }
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
            logCalendar.get(Calendar.YEAR) == year && logCalendar.get(Calendar.MONTH) == month && hasProgress(log)
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
            logCalendar.get(Calendar.YEAR) == year && hasProgress(log)
        }
    }

    private suspend fun countTotalWorkoutsSince(db: AppDatabase): Pair<Int, Date?> {
        val logs = withContext(Dispatchers.IO) {
            db.workoutLogDao().getAll()
        }
        val filteredLogs = logs.filter { hasProgress(it) }
        val count = filteredLogs.size
        val sinceDate = filteredLogs.minOfOrNull { log ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(log.date) ?: Date()
        }
        return Pair(count, sinceDate)
    }

    private suspend fun calculateDedication(db: AppDatabase, sinceDate: Date?): Int {
        if (sinceDate == null) return 0
        val logs = withContext(Dispatchers.IO) {
            db.workoutLogDao().getAll()
        }
        val workoutDays = logs.count { hasProgress(it) }
        val today = Calendar.getInstance().time
        val diff = today.time - sinceDate.time
        val totalDays = (diff / (1000 * 60 * 60 * 24)).toInt() + 1 // inclusive
        if (totalDays <= 0) return 0
        return (workoutDays.toDouble() / totalDays * 100).toInt()
    }

    private fun hasProgress(log: WorkoutLogEntity): Boolean {
        return try {
            val gson = Gson()
            val exercises: List<Exercise> = gson.fromJson(log.loggedJson, object : TypeToken<List<Exercise>>() {}.type)
            exercises.any { it.completedSets > 0 }
        } catch (e: Exception) {
            false // If parsing fails, assume no progress
        }
    }

    private fun setupDedicationGauge() {
        val range1 = Range()
        range1.color = Color.RED
        range1.from = 0.0
        range1.to = 33.0

        val range2 = Range()
        range2.color = Color.YELLOW
        range2.from = 33.0
        range2.to = 66.0

        val range3 = Range()
        range3.color = Color.GREEN
        range3.from = 66.0
        range3.to = 100.0

        binding.dedicationGauge.addRange(range1)
        binding.dedicationGauge.addRange(range2)
        binding.dedicationGauge.addRange(range3)

        binding.dedicationGauge.minValue = 0.0
        binding.dedicationGauge.maxValue = 100.0
        binding.dedicationGauge.setNeedleColor(Color.WHITE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
