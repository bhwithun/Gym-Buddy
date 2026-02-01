package com.gymbuddy

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
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

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var flashRunnable: Runnable? = null
    private var isTimerRunning = false

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

        // Set up tap listener for notes magnification toggle
        binding.notesText.setOnClickListener {
            if (binding.fullScreenNotesText.visibility == View.VISIBLE) {
                hideFullScreenNotes()
            } else {
                showFullScreenNotes()
            }
        }

        // Allow tapping full-screen notes to return to normal view
        binding.fullScreenNotesText.setOnClickListener {
            hideFullScreenNotes()
        }



        binding.progressPieChart.setOnClickListener {
            if (exercise.completedSets >= exercise.sets) {
                // Reset progress
                exercise.completedSets = 0
                updateUI()
                onUpdate(exercise)
                stopTimer()
            } else {
                // Complete set logic
                if (exercise.completedSets < exercise.sets) {
                    // Haptic feedback
                    val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        vibrator.vibrate(50)
                    }

                    exercise.completedSets++
                    updateUI()
                    onUpdate(exercise)
                    onSetCompleted(position)

                    // Handle timer: stop if final set completed, otherwise start new timer
                    if (exercise.completedSets >= exercise.sets) {
                        stopTimer()  // Stop any running timer when final set is completed
                    } else {
                        startTimer()  // Start timer for next set
                    }
                }
            }
        }
    }

    private fun startTimer(initialSeconds: Int = 59) {
        stopTimer() // Stop any existing timer
        exercise.isTimerActive = true
        exercise.remainingSeconds = initialSeconds
        isTimerRunning = true
        updateUI()
        onUpdate(exercise)

        timerRunnable = Runnable {
            exercise.remainingSeconds--
            if (exercise.remainingSeconds > 0) {
                updateUI()
                onUpdate(exercise)
                handler.postDelayed(timerRunnable!!, 1000)
            } else {
                startFlashing()
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
    }

    private fun startFlashing() {
        var flashCount = 0
        flashRunnable = Runnable {
            binding.cooldownValue.visibility = if (binding.cooldownValue.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
            flashCount++
            if (flashCount < 6) { // 3 flashes (6 toggles)
                handler.postDelayed(flashRunnable!!, 200)
            } else {
                exercise.isTimerActive = false
                exercise.remainingSeconds = 0
                isTimerRunning = false
                updateUI()
                onUpdate(exercise)
            }
        }
        handler.post(flashRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        flashRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        flashRunnable = null
        isTimerRunning = false
        exercise.isTimerActive = false
        exercise.remainingSeconds = 0
        updateUI()
        onUpdate(exercise)
    }

    private fun showFullScreenNotes() {
        // Set the notes text (remove "Notes: " prefix if present)
        val notesText = exercise.notes.removePrefix("Notes: ").trim()
        binding.fullScreenNotesText.text = notesText

        // Hide all other UI elements
        binding.titleText.visibility = View.GONE
        binding.weightLabel.visibility = View.GONE
        binding.weightValue.visibility = View.GONE
        binding.repsLabel.visibility = View.GONE
        binding.repsValue.visibility = View.GONE
        binding.setsLabel.visibility = View.GONE
        binding.setsValue.visibility = View.GONE
        binding.cooldownLabel.visibility = View.GONE
        binding.cooldownValue.visibility = View.GONE
        binding.notesText.visibility = View.GONE
        binding.progressPieChart.visibility = View.GONE

        // Show full screen notes
        binding.fullScreenNotesText.visibility = View.VISIBLE
    }

    private fun hideFullScreenNotes() {
        // Hide full screen notes
        binding.fullScreenNotesText.visibility = View.GONE

        // Show all other UI elements
        binding.titleText.visibility = View.VISIBLE
        binding.weightLabel.visibility = View.VISIBLE
        binding.weightValue.visibility = View.VISIBLE
        binding.repsLabel.visibility = View.VISIBLE
        binding.repsValue.visibility = View.VISIBLE
        binding.setsLabel.visibility = View.VISIBLE
        binding.setsValue.visibility = View.VISIBLE
        binding.notesText.visibility = View.VISIBLE
        binding.progressPieChart.visibility = View.VISIBLE

        // Cooldown visibility is managed by updateUI
        updateUI()
    }

    private fun updateUI() {
        binding.titleText.text = exercise.title

        binding.weightLabel.text = "Weight"
        binding.weightValue.text = exercise.weight.toString()

        binding.repsLabel.text = "Reps"
        binding.repsValue.text = exercise.reps.toString()

        binding.setsLabel.text = "Sets"
        binding.setsValue.text = exercise.sets.toString()

        if (exercise.isTimerActive && isTimerRunning && exercise.remainingSeconds > 0) {
            binding.cooldownLabel.visibility = View.VISIBLE
            binding.cooldownValue.visibility = View.VISIBLE
            binding.cooldownLabel.text = "Cooldown"
            binding.cooldownValue.text = ":${exercise.remainingSeconds.toString().padStart(2, '0')}"
        } else {
            binding.cooldownLabel.visibility = View.GONE
            binding.cooldownValue.visibility = View.GONE
        }

        binding.notesText.text = if (exercise.notes.isNotBlank()) "Notes: ${exercise.notes}" else ""

        binding.progressPieChart.setProgress(exercise.completedSets, exercise.sets)
        binding.progressPieChart.setSegmented(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}
