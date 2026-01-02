package com.gymbuddy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gymbuddy.databinding.FragmentWorkoutBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private val exercises = mutableListOf<Exercise>()
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val day = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(today)
            }
            if (day?.isRest == true) {
                Toast.makeText(requireContext(), "Rest Day!", Toast.LENGTH_SHORT).show()
            } else if (day != null) {
                val baseExercises: List<Exercise> = day.exercises
                exercises.clear()
                exercises.addAll(baseExercises)

                // Load saved progress
                loadProgress()

                val adapter = ExercisePagerAdapter(this@WorkoutFragment, exercises) { position ->
                    saveProgress()
                }
                binding.viewPager.adapter = adapter
            } else {
                Toast.makeText(requireContext(), "No routine for today", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProgress() {
        val prefs = requireContext().getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val progressJson = prefs.getString("progress_$dateKey", null)
        if (progressJson != null) {
            val savedExercises: List<Exercise> = gson.fromJson(progressJson, object : TypeToken<List<Exercise>>() {}.type)
            // Update completedSets
            for (i in exercises.indices) {
                if (i < savedExercises.size) {
                    exercises[i].completedSets = savedExercises[i].completedSets
                }
            }
        }
    }

    private fun saveProgress() {
        val prefs = requireContext().getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val progressJson = gson.toJson(exercises)
        prefs.edit().putString("progress_$dateKey", progressJson).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        saveWorkoutLog()
        _binding = null
    }

    private fun saveWorkoutLog() {
        if (exercises.isNotEmpty()) {
            lifecycleScope.launch {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val plannedJson = gson.toJson(exercises.map { it.copy(completedSets = 0) }) // planned without completion
                val loggedJson = gson.toJson(exercises)
                val log = WorkoutLogEntity(dateStr, plannedJson, loggedJson)
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).workoutLogDao().insert(log)
                }
            }
        }
    }

    private class ExercisePagerAdapter(
        fragment: Fragment,
        private val exercises: List<Exercise>,
        private val onSetCompleted: (Int) -> Unit
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = exercises.size

        override fun createFragment(position: Int): Fragment {
            val fragment = ExerciseWorkoutFragment.newInstance(exercises[position], position)
            fragment.setOnSetCompletedListener(onSetCompleted)
            return fragment
        }
    }
}
