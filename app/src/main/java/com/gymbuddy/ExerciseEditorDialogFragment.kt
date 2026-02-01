package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.DialogExerciseEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseEditorDialogFragment : DialogFragment() {

    private var _binding: DialogExerciseEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var exercise: Exercise
    private var onSave: ((Exercise) -> Unit)? = null

    companion object {
        private const val ARG_EXERCISE = "exercise"

        fun newInstance(exercise: Exercise, onSave: (Exercise) -> Unit): ExerciseEditorDialogFragment {
            val fragment = ExerciseEditorDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXERCISE, exercise)
            fragment.arguments = args
            fragment.onSave = onSave
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exercise = arguments?.getSerializable(ARG_EXERCISE) as Exercise
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExerciseEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nameEditText.setText(exercise.title)
        binding.notesEditText.setText(exercise.notes)

        binding.saveButton.setOnClickListener {
            val updatedExercise = exercise.copy(
                title = binding.nameEditText.text.toString(),
                notes = binding.notesEditText.text.toString()
            )
            onSave?.invoke(updatedExercise)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}