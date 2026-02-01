package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.FragmentDayDetailPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDetailPageFragment : Fragment() {

    private var _binding: FragmentDayDetailPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var day: RoutineDayEntity
    private lateinit var exercises: MutableList<Exercise>
    private lateinit var adapter: ExerciseAdapter

    companion object {
        private const val ARG_DAY = "day"

        fun newInstance(day: RoutineDayEntity): DayDetailPageFragment {
            val fragment = DayDetailPageFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAY, day)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = arguments?.getSerializable(ARG_DAY) as RoutineDayEntity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayDetailPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        binding.dayTitle.text = dayNames[day.dayOfWeek - 1]

        exercises = day.exercises.toMutableList()
        binding.restDayText.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                exercises.add(toPosition, exercises.removeAt(fromPosition))
                adapter.notifyItemMoved(fromPosition, toPosition)
                saveToDb()
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe actions
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)

        adapter = ExerciseAdapter(exercises, { _, _ -> }, { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        })
        binding.exercisesRecyclerView.adapter = adapter
    }

    private fun saveToDb() {
        lifecycleScope.launch {
            val updatedDay = RoutineDayEntity(day.dayOfWeek, exercises.isEmpty(), exercises)
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)

                // Also update today's workout if it exists
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val existingLog = AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(dateStr)
                if (existingLog != null) {
                    val gson = com.google.gson.Gson()
                    val loggedExercises: MutableList<Exercise> = gson.fromJson(existingLog.loggedJson, object : com.google.gson.reflect.TypeToken<MutableList<Exercise>>() {}.type)
                    // Reorder loggedExercises to match the new routine order
                    val reorderedExercises = exercises.map { routineExercise ->
                        loggedExercises.find { it.title == routineExercise.title } ?: routineExercise
                    }.toMutableList()
                    val newLoggedJson = gson.toJson(reorderedExercises)
                    val updatedLog = existingLog.copy(loggedJson = newLoggedJson)
                    AppDatabase.getDatabase(requireContext()).workoutLogDao().insert(updatedLog)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}