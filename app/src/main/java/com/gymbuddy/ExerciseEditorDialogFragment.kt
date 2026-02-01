package com.gymbuddy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogExerciseEditorBinding

class ExerciseEditorDialogFragment : DialogFragment() {

    private var _binding: DialogExerciseEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var exercise: Exercise
    private var index: Int = 0
    private lateinit var listener: ExerciseEditorListener

    interface ExerciseEditorListener {
        fun onExerciseUpdated(wrapper: ExerciseWrapper)
        fun onDayUpdated(day: RoutineDayEntity)
    }

    companion object {
        private const val ARG_INDEX = "index"
        private const val ARG_EXERCISE = "exercise"

        fun newInstance(index: Int, exercise: Exercise): ExerciseEditorDialogFragment {
            val fragment = ExerciseEditorDialogFragment()
            val args = Bundle()
            args.putInt(ARG_INDEX, index)
            args.putSerializable(ARG_EXERCISE, exercise)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            index = it.getInt(ARG_INDEX)
            exercise = it.getSerializable(ARG_EXERCISE) as Exercise
        }
        listener = targetFragment as? ExerciseEditorListener ?: parentFragment as? ExerciseEditorListener ?: requireActivity() as ExerciseEditorListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExerciseEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate fields
        binding.titleEdit.setText(exercise.title)
        binding.weightEdit.setText(exercise.weight.toString())
        binding.repsValue.text = exercise.reps.toString()
        binding.setsValue.text = exercise.sets.toString()
        binding.notesEdit.setText(exercise.notes)

        // Reps buttons
        binding.repsMinus.setOnClickListener {
            val current = binding.repsValue.text.toString().toInt()
            if (current > 1) {
                binding.repsValue.text = (current - 1).toString()
            }
        }
        binding.repsPlus.setOnClickListener {
            val current = binding.repsValue.text.toString().toInt()
            binding.repsValue.text = (current + 1).toString()
        }

        // Sets buttons
        binding.setsMinus.setOnClickListener {
            val current = binding.setsValue.text.toString().toInt()
            if (current > 1) {
                binding.setsValue.text = (current - 1).toString()
            }
        }
        binding.setsPlus.setOnClickListener {
            val current = binding.setsValue.text.toString().toInt()
            binding.setsValue.text = (current + 1).toString()
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            val wrapper = ExerciseWrapper(index, exercise, "canceled")
            listener.onExerciseUpdated(wrapper)
            dismiss()
        }

        // Save
        binding.saveButton.setOnClickListener {
            val title = binding.titleEdit.text.toString().trim()
            val weightStr = binding.weightEdit.text.toString().trim()
            if (title.isBlank() || weightStr.isBlank()) {
                // Show error, but for now, assume valid
                return@setOnClickListener
            }
            val weight = weightStr.toIntOrNull() ?: 1
            val reps = binding.repsValue.text.toString().toInt()
            val sets = binding.setsValue.text.toString().toInt()
            val notes = binding.notesEdit.text.toString()
            val updatedExercise = Exercise(title, weight, reps, sets, notes)
            val wrapper = ExerciseWrapper(index, updatedExercise, "updated")
            listener.onExerciseUpdated(wrapper)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
