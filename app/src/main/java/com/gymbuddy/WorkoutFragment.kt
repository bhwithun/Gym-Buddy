package com.gymbuddy

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import kotlin.math.max

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!
    private val exercises = mutableListOf<Exercise>()
    private val smallPies = mutableListOf<ProgressPieChart>()
    private val gson = Gson()
    private var makeupDayOfWeek: Int? = null
    private var isMakeup = false

    companion object {
        private const val ARG_MAKEUP_DAY = "makeup_day"

        fun newInstance(makeupDayOfWeek: Int): WorkoutFragment {
            val fragment = WorkoutFragment()
            val args = Bundle()
            args.putInt(ARG_MAKEUP_DAY, makeupDayOfWeek)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val day = it.getInt(ARG_MAKEUP_DAY, -1)
            if (day != -1) {
                makeupDayOfWeek = day
                isMakeup = true
            }
        }
    }

    private fun getDayName(dayOfWeek: Int): String {
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        return days[dayOfWeek - 1]
    }

    // ── Circle appearance constants ──
    private val CIRCLE_SIZE_DP = 42f          // Full size of each pie when not crowded
    private val OVERLAP_DP = 12f              // How much each circle overlaps the previous one
                                          // 0 = no overlap, 36 = fully stacked, 18 = ~50% overlap

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
        if (isMakeup) {
            binding.dateText.text = "Makeup: ${getDayName(makeupDayOfWeek!!)}"
        } else {
            val dateFormat = SimpleDateFormat("EEE MMM d, yyyy", Locale.getDefault())
            binding.dateText.text = dateFormat.format(Date())
        }
        binding.makeupLink.setOnClickListener {
            lifecycleScope.launch {
                showMakeupDayDialog()
            }
        }
        loadWorkout()
    }

    private fun loadWorkout() {
        lifecycleScope.launch {
            val loadedDay = makeupDayOfWeek ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val day = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(loadedDay)
            }
            if (day?.isRest == true) {
                Toast.makeText(requireContext(), "Rest Day!", Toast.LENGTH_SHORT).show()
                binding.viewPager.adapter = null
            } else if (day != null) {
                val baseExercises: List<Exercise> = day.exercises
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                exercises.clear()

                if (isMakeup) {
                    // For makeup days, always start fresh with base exercises
                    exercises.addAll(baseExercises)
                    val plannedJson = gson.toJson(exercises.map { it.copy(completedSets = 0) })
                    val loggedJson = gson.toJson(exercises)
                    val log = WorkoutLogEntity(dateStr, plannedJson, loggedJson)
                    withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(requireContext()).workoutLogDao().insert(log)
                    }
                } else {
                    val existingLog = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(dateStr)
                    }
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
                }

                val adapter = ExercisePagerAdapter(this@WorkoutFragment, exercises, { position ->
                    saveWorkoutLog()
                    updateBackgroundColor()
                }, { updatedExercise ->
                    // Update the exercise in the list
                    val pos = exercises.indexOf(updatedExercise)
                    if (pos != -1) {
                        exercises[pos] = updatedExercise
                        // Update the corresponding small pie chart
                        smallPies[pos].setProgress(updatedExercise.completedSets, updatedExercise.sets)
                    }
                    updateBackgroundColor()
                    // Save workout log immediately to persist rating changes
                    saveWorkoutLog()
                    // Also update the routine
                    if (isAdded && context != null) {
                        lifecycleScope.launch {
                            val loadedDay = makeupDayOfWeek ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                            val routineDay = withContext(Dispatchers.IO) {
                                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(loadedDay)
                            }
                            if (routineDay != null) {
                                val updatedExercises = routineDay.exercises.toMutableList()
                                val routinePos = updatedExercises.indexOfFirst { it.title == updatedExercise.title }
                                if (routinePos != -1) {
                                    updatedExercises[routinePos] = updatedExercise.copy(completedSets = 0) // reset completedSets for routine template
                                    val updatedRoutineDay = routineDay.copy(exercises = updatedExercises)
                                    withContext(Dispatchers.IO) {
                                        AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedRoutineDay)
                                    }
                                }
                            }
                        }
                    }
                })

                binding.viewPager.adapter = adapter
                binding.viewPager.setPageTransformer(PageFlipPageTransformer())

                // Set up highlighting for current page
                binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        updateCircleHighlights(position)
                    }
                })

                // ── Create overlapping small progress circles ──
                binding.circlesContainer.removeAllViews()
                smallPies.clear()

                if (exercises.isNotEmpty()) {
                    val density = resources.displayMetrics.density
                    val circleSizePx = (CIRCLE_SIZE_DP * density).toInt()
                    val negativeMarginPx = (-OVERLAP_DP * density).toInt()

                    exercises.forEachIndexed { index, exercise ->
                        val pie = ProgressPieChart(requireContext()).apply {
                            val params = LinearLayout.LayoutParams(
                                circleSizePx,
                                circleSizePx
                            ).apply {
                                gravity = Gravity.CENTER_VERTICAL

                                // Negative margin creates the overlap (except first circle)
                                if (index > 0) {
                                    marginStart = negativeMarginPx
                                }
                            }
                            layoutParams = params

                            setProgress(exercise.completedSets, exercise.sets)
                            setSegmented(true)

                            // Later pies appear on top during overlap
                            elevation = index * 2f   // or translationZ = index * 4f for older APIs
                        }
                        smallPies.add(pie)
                        binding.circlesContainer.addView(pie)
                    }
                }

                // Auto-advance to first incomplete exercise
                val firstIncompleteIndex = exercises.indexOfFirst { it.completedSets < it.sets }
                if (firstIncompleteIndex != -1) {
                    binding.viewPager.setCurrentItem(firstIncompleteIndex, false)
                }

                // Check if all exercises are complete and update background
                updateBackgroundColor()
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
        if (exercises.isNotEmpty() && isAdded) { // Check if fragment is attached
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

    private fun updateBackgroundColor() {
        if (_binding == null) return
        val allComplete = exercises.all { it.completedSets >= it.sets }
        val backgroundColor = if (allComplete) android.graphics.Color.parseColor("#FF006400") else android.graphics.Color.parseColor("#FF121212") // dark green or default
        binding.root.setBackgroundColor(backgroundColor)
    }

    private fun updateCircleHighlights(currentPosition: Int) {
        for (i in smallPies.indices) {
            smallPies[i].setHighlighted(i == currentPosition)
        }
    }

    private class PageFlipPageTransformer : ViewPager2.PageTransformer {
        override fun transformPage(page: View, position: Float) {
            val rotation = -30f * position
            page.rotationY = rotation
            page.alpha = 1f - Math.abs(position) * 0.5f
        }
    }

    private suspend fun showMakeupDayDialog() {
        val allRoutineDays = withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(requireContext()).routineDao().getAll()
        }
        val nonRestDays = allRoutineDays.filter { !it.isRest }
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val availableDays = nonRestDays.map { dayNames[it.dayOfWeek - 1] }.toTypedArray()
        val availableDayOfWeeks = nonRestDays.map { it.dayOfWeek }.toIntArray()

        if (availableDays.isEmpty()) {
            Toast.makeText(requireContext(), "No workout days available for makeup", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Makeup Day")
            .setItems(availableDays) { _, which ->
                val selectedDayOfWeek = availableDayOfWeeks[which]
                (requireActivity() as MainActivity).replaceFragment(WorkoutFragment.newInstance(selectedDayOfWeek))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
