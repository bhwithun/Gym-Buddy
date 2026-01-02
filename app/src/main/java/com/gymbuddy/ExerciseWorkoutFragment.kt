package com.gymbuddy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentExerciseWorkoutBinding

class ExerciseWorkoutFragment : Fragment() {

    private var _binding: FragmentExerciseWorkoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var exercise: Exercise
    private var position: Int = 0
    private lateinit var onSetCompleted: (Int) -> Unit
    private lateinit var onUpdate: (Exercise) -> Unit

    companion object {
        private const val ARG_EXERCISE = "exercise"
        private const val ARG_POSITION = "position"

        fun newInstance(exercise: Exercise, position: Int): ExerciseWorkoutFragment {
            val fragment = ExerciseWorkoutFragment()
            val args = Bundle()
            args.putSerializable(ARG_EXERCISE, exercise)
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnSetCompletedListener(listener: (Int) -> Unit) {
        onSetCompleted = listener
    }

    fun setOnUpdateListener(listener: (Exercise) -> Unit) {
        onUpdate = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exercise = arguments?.getSerializable(ARG_EXERCISE) as Exercise
        position = arguments?.getInt(ARG_POSITION) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI()

        binding.completeSetButton.setOnClickListener {
            if (exercise.completedSets < exercise.sets) {
                exercise.completedSets++
                updateUI()
                onUpdate(exercise)
                onSetCompleted(position)
            }
        }
    }

    private fun updateUI() {
        binding.titleText.text = exercise.title
        binding.weightText.text = "Weight: ${exercise.weight}"
        binding.repsText.text = "Reps: ${exercise.reps}"
        binding.setsText.text = "Sets: ${exercise.sets}"
        binding.notesText.text = if (exercise.notes.isNotBlank()) "Notes: ${exercise.notes}" else ""

        if (exercise.completedSets < exercise.sets) {
            binding.completeSetButton.text = "Complete Set ${exercise.completedSets + 1} of ${exercise.sets}"
            binding.completeSetButton.isEnabled = true
        } else {
            binding.completeSetButton.text = "All Sets Completed"
            binding.completeSetButton.isEnabled = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
