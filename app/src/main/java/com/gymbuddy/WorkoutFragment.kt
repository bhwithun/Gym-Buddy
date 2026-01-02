package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.FragmentWorkoutBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class WorkoutFragment : Fragment(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

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
                val exercises: List<Exercise> = Gson().fromJson(day.exercisesJson, object : TypeToken<List<Exercise>>() {}.type)
                val mutableExercises = exercises.toMutableList()
                binding.recyclerView.adapter = ExerciseAdapter(mutableExercises) { position, action ->
                    if (action == "edit") {
                        val fragment = ExerciseEditorDialogFragment.newInstance(position, mutableExercises[position])
                        fragment.show(parentFragmentManager, "exercise_editor")
                    }
                }
            } else {
                Toast.makeText(requireContext(), "No routine for today", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        // Update the local list
        val adapter = binding.recyclerView.adapter as? ExerciseAdapter
        if (adapter != null && wrapper.status == "updated") {
            adapter.exercises[wrapper.index] = wrapper.exercise
            adapter.notifyItemChanged(wrapper.index)
        }
    }

    override fun onDayUpdated(day: RoutineDayEntity) {
        // Not used in workout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
