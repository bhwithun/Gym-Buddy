package com.gymbuddy

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gymbuddy.databinding.FragmentWorkoutBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
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

        // Set the date
        val dateFormat = SimpleDateFormat("EEE MMM d, yyyy", Locale.getDefault())
        binding.dateText.text = dateFormat.format(Date())

        loadWorkout()

        binding.resetButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Reset Today")
                .setMessage("Are you sure you want to reset today's workout progress? This action cannot be undone.")
                .setPositiveButton("Reset") { _, _ ->
                    lifecycleScope.launch {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(requireContext()).workoutLogDao().deleteByDate(dateStr)
                        }
                        Toast.makeText(requireContext(), "Today's workout reset", Toast.LENGTH_SHORT).show()
                        loadWorkout()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadWorkout() {
        lifecycleScope.launch {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val day = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(today)
            }
            if (day?.isRest == true) {
                Toast.makeText(requireContext(), "Rest Day!", Toast.LENGTH_SHORT).show()
                binding.viewPager.adapter = null
            } else if (day != null) {
                val baseExercises: List<Exercise> = day.exercises
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val existingLog = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(dateStr)
                }
                exercises.clear()
                if (existingLog != null) {
                    // Load from log
                    val loggedExercises: List<Exercise> = gson.fromJson(existingLog.loggedJson, object : TypeToken<List<Exercise>>() {}.type)
                    exercises.addAll(loggedExercises)
                } else {
                    // Create new log
                    exercises.addAll(baseExercises)
                    val plannedJson = gson.toJson(exercises.map { it.copy(completedSets = 0) })
                    val loggedJson = gson.toJson(exercises)
                    val log = WorkoutLogEntity(dateStr, plannedJson, loggedJson)
                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(requireContext()).workoutLogDao().insert(log)
                    }
                }

                val adapter = ExercisePagerAdapter(this@WorkoutFragment, exercises, { position ->
                    saveWorkoutLog()
                }, { updatedExercise ->
                    // Update the exercise in the list
                    val pos = exercises.indexOfFirst { it.title == updatedExercise.title && it.weight == updatedExercise.weight }
                    if (pos != -1) {
                        exercises[pos] = updatedExercise
                    }
                    // Also update the routine
                    lifecycleScope.launch {
                        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                        val routineDay = withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(today)
                        }
                        if (routineDay != null) {
                            val updatedExercises = routineDay.exercises.toMutableList()
                            val routinePos = updatedExercises.indexOfFirst { it.title == updatedExercise.title && it.weight == updatedExercise.weight }
                            if (routinePos != -1) {
                                updatedExercises[routinePos] = updatedExercise.copy(completedSets = 0) // reset completedSets for routine template
                                val updatedRoutineDay = routineDay.copy(exercises = updatedExercises)
                                withContext(Dispatchers.IO) {
                                    AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedRoutineDay)
                                }
                            }
                        }
                    }
                })
                binding.viewPager.adapter = adapter
                binding.viewPager.setPageTransformer(PageFlipPageTransformer())
            } else {
                Toast.makeText(requireContext(), "No routine for today", Toast.LENGTH_SHORT).show()
                binding.viewPager.adapter = null
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        saveWorkoutLog()
        _binding = null
    }

    private fun saveWorkoutLog() {
        if (exercises.isNotEmpty()) {
            val context = requireContext()
            CoroutineScope(Dispatchers.IO).launch {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val plannedJson = gson.toJson(exercises.map { it.copy(completedSets = 0) }) // planned without completion
                val loggedJson = gson.toJson(exercises)
                val log = WorkoutLogEntity(dateStr, plannedJson, loggedJson)
                AppDatabase.getDatabase(context).workoutLogDao().insert(log)
            }
        }
    }

    private class ExercisePagerAdapter(
        fragment: Fragment,
        private val exercises: List<Exercise>,
        private val onSetCompleted: (Int) -> Unit,
        private val onUpdate: (Exercise) -> Unit
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = exercises.size

        override fun createFragment(position: Int): Fragment {
            val fragment = ExerciseWorkoutFragment.newInstance(exercises[position], position)
            fragment.setOnSetCompletedListener(onSetCompleted)
            fragment.setOnUpdateListener(onUpdate)
            return fragment
        }
    }

    private class PageFlipPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val rotation = -30f * position
            page.rotationY = rotation
            page.alpha = 1f - Math.abs(position) * 0.5f
        }
    }
}
