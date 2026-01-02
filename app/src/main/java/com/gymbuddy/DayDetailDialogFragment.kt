package com.gymbuddy

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.DialogDayDetailBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymbuddy.Exercise
import com.gymbuddy.RoutineDayEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDetailDialogFragment : DialogFragment(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private var _binding: DialogDayDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var day: RoutineDayEntity
    private var exercises = mutableListOf<Exercise>()
    private val gson = Gson()

    companion object {
        private const val ARG_DAY = "day"

        fun newInstance(day: RoutineDayEntity): DayDetailDialogFragment {
            val fragment = DayDetailDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAY, day)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = arguments?.getSerializable(ARG_DAY) as RoutineDayEntity
        exercises.addAll(gson.fromJson(day.exercisesJson, object : TypeToken<List<Exercise>>() {}.type))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDayDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        binding.dayTitle.text = dayNames[day.dayOfWeek - 1]

        refreshExercisesViews()

        binding.addButton.setOnClickListener {
            val newExercise = Exercise("New Exercise", "0", 10, 3, "")
            exercises.add(newExercise)
            refreshExercisesViews()
        }

        binding.saveButton.setOnClickListener {
            lifecycleScope.launch {
                val isRest = exercises.isEmpty()
                val updatedJson = gson.toJson(exercises)
                val updatedDay = day.copy(isRest = isRest, exercisesJson = updatedJson)
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)
                }
                (activity as? ExerciseEditorDialogFragment.ExerciseEditorListener)?.onDayUpdated(updatedDay)
                dismiss()
            }
        }
    }

    private fun refreshExercisesViews() {
        binding.exercisesLayout.removeAllViews()
        for (i in exercises.indices) {
            addExerciseTextView(i)
        }
    }

    private fun addExerciseTextView(index: Int) {
        val exercise = exercises[index]
        val textView = TextView(requireContext()).apply {
            text = "${exercise.title}\n${exercise.weight} | ${exercise.reps} x ${exercise.sets}\n${exercise.notes}"
            setPadding(16, 16, 16, 16)
            setOnClickListener {
                val fragment = ExerciseEditorDialogFragment.newInstance(index, exercise)
                fragment.setTargetFragment(this@DayDetailDialogFragment, 0)
                fragment.show(parentFragmentManager, "exercise_editor")
            }
            setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Exercise")
                    .setMessage("Are you sure you want to delete this exercise?")
                    .setPositiveButton("Yes") { _, _ ->
                        exercises.removeAt(index)
                        refreshExercisesViews()
                        saveToDb()
                    }
                    .setNegativeButton("No", null)
                    .show()
                true
            }
        }
        binding.exercisesLayout.addView(textView)
    }

    private fun saveToDb() {
        lifecycleScope.launch {
            val updatedJson = gson.toJson(exercises)
            val updatedDay = day.copy(exercisesJson = updatedJson)
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)
            }
            (activity as? ExerciseEditorDialogFragment.ExerciseEditorListener)?.onDayUpdated(updatedDay)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        if (wrapper.status == "updated") {
            exercises[wrapper.index] = wrapper.exercise
            refreshExercisesViews()
        }
    }

    override fun onDayUpdated(day: RoutineDayEntity) {
        // Not used
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
