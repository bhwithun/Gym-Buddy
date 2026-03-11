package com.gymbuddy

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gymbuddy.databinding.FragmentExerciseWorkoutBinding

class ExerciseWorkoutFragment : Fragment(), SetsEditorDialogFragment.SetsEditorListener, RepsEditorDialogFragment.RepsEditorListener, WeightEditorDialogFragment.WeightEditorListener {

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
        updateStarUI()

        // Resume timer if it was running
        if (exercise.isTimerActive && exercise.timerEndTime > System.currentTimeMillis() && !isTimerRunning) {
            resumeTimer()
        }

        binding.titleText.setOnClickListener {
            val dialog = ExerciseEditorDialogFragment.newInstance(exercise) { updatedExercise ->
                exercise.title = updatedExercise.title
                exercise.notes = updatedExercise.notes
                updateUI()
                onUpdate(exercise)
            }
            dialog.show(parentFragmentManager, "exercise_editor")
        }

        binding.weightValue.setOnClickListener {
            val dialog = WeightEditorDialogFragment.newInstance(exercise.weight)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "weight_editor")
        }

        binding.repsValue.setOnClickListener {
            val dialog = RepsEditorDialogFragment.newInstance(exercise.reps)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "reps_editor")
        }

        binding.setsValue.setOnClickListener {
            val dialog = SetsEditorDialogFragment.newInstance(exercise.sets)
            dialog.setTargetFragment(this, 0)
            dialog.show(parentFragmentManager, "sets_editor")
        }

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

        // Set up help icon click listener for Google search
        binding.helpIcon.setOnClickListener {
            searchExerciseOnline(exercise.title)
        }

        // Set up star rating click listeners
        binding.starEasy.setOnClickListener {
            exercise.rating = "easy"
            updateStarUI()
            showRatingBubble("easy")
            onUpdate(exercise)
        }

        binding.starGood.setOnClickListener {
            exercise.rating = "good"
            updateStarUI()
            showRatingBubble("good")
            onUpdate(exercise)
        }

        binding.starHard.setOnClickListener {
            exercise.rating = "hard"
            updateStarUI()
            showRatingBubble("hard")
            onUpdate(exercise)
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
                    showSetCompletedBubble()

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

    override fun onResume() {
        super.onResume()
        // Ensure star UI is updated when swiping between exercises
        updateStarUI()
    }

    private fun startTimer(initialSeconds: Int = 59) {
        stopTimer() // Stop any existing timer
        exercise.isTimerActive = true
        exercise.timerEndTime = System.currentTimeMillis() + initialSeconds * 1000L
        isTimerRunning = true
        updateUI()
        onUpdate(exercise)

        timerRunnable = Runnable {
            try {
                val currentTime = System.currentTimeMillis()
                exercise.remainingSeconds = maxOf(0, ((exercise.timerEndTime - currentTime) / 1000).toInt())
                if (exercise.remainingSeconds > 0) {
                    if (_binding != null) updateUI()
                    onUpdate(exercise)
                    handler.postDelayed(timerRunnable!!, 1000)
                } else {
                    if (isAdded) { // Check if fragment is still attached
                        startFlashing()
                    }
                }
            } catch (e: Exception) {
                // Timer failed, stop it safely
                if (isAdded) {
                    stopTimer()
                }
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
    }

    private fun resumeTimer() {
        if (exercise.isTimerActive && exercise.timerEndTime > System.currentTimeMillis()) {
            isTimerRunning = true
            updateUI()

            timerRunnable = Runnable {
                try {
                    val currentTime = System.currentTimeMillis()
                    exercise.remainingSeconds = maxOf(0, ((exercise.timerEndTime - currentTime) / 1000).toInt())
                    if (exercise.remainingSeconds > 0) {
                        if (_binding != null) updateUI()
                        onUpdate(exercise)
                        handler.postDelayed(timerRunnable!!, 1000)
                    } else {
                        if (isAdded) { // Check if fragment is still attached
                            startFlashing()
                        }
                    }
                } catch (e: Exception) {
                    // Timer failed, stop it safely
                    if (isAdded) {
                        stopTimer()
                    }
                }
            }
            handler.postDelayed(timerRunnable!!, 1000)
        }
    }

    private fun startFlashing() {
        var flashCount = 0
        flashRunnable = Runnable {
            try {
                if (_binding != null) {
                    binding.cooldownValue.visibility = if (binding.cooldownValue.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                }
                flashCount++
                if (flashCount < 6) { // 3 flashes (6 toggles)
                    handler.postDelayed(flashRunnable!!, 200)
                } else {
                    exercise.isTimerActive = false
                    exercise.remainingSeconds = 0
                    isTimerRunning = false
                    if (_binding != null) updateUI()
                    onUpdate(exercise)
                }
            } catch (e: Exception) {
                // Flashing failed, stop it
                stopTimer()
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
        updateUI()
        onUpdate(exercise)
    }

    private fun showFullScreenNotes() {
        // Set the notes text (remove "Notes: " prefix if present)
        val notesText = exercise.notes.removePrefix("Notes: ").trim()
        binding.fullScreenNotesText.text = notesText

        // Hide all other UI elements
        binding.titleContainer.visibility = View.GONE
        binding.weightLabel.visibility = View.GONE
        binding.weightValue.visibility = View.GONE
        binding.repsLabel.visibility = View.GONE
        binding.repsValue.visibility = View.GONE
        binding.setsLabel.visibility = View.GONE
        binding.setsValue.visibility = View.GONE
        binding.starRatingContainer.visibility = View.GONE
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
        binding.titleContainer.visibility = View.VISIBLE
        binding.weightLabel.visibility = View.VISIBLE
        binding.weightValue.visibility = View.VISIBLE
        binding.repsLabel.visibility = View.VISIBLE
        binding.repsValue.visibility = View.VISIBLE
        binding.setsLabel.visibility = View.VISIBLE
        binding.setsValue.visibility = View.VISIBLE
        binding.starRatingContainer.visibility = View.VISIBLE
        binding.notesText.visibility = View.VISIBLE
        binding.progressPieChart.visibility = View.VISIBLE

        // Cooldown visibility is managed by updateUI
        updateStarUI()
        updateUI()
    }

    private fun updateStarUI() {
        if (_binding == null) return

        // Reset all stars to outline (☆) with golden color
        binding.starEasy.text = "\u2606"
        binding.starEasy.setTextColor(0xFFFFD700.toInt()) // Golden
        binding.starGood.text = "\u2606"
        binding.starGood.setTextColor(0xFFFFD700.toInt()) // Golden
        binding.starHard.text = "\u2606"
        binding.starHard.setTextColor(0xFFFFD700.toInt()) // Golden

        // Fill stars based on rating (★) with yellow color
        when (exercise.rating) {
            "easy" -> {
                binding.starEasy.text = "\u2605"
                binding.starEasy.setTextColor(0xFFFFFF00.toInt()) // Yellow
            }
            "good" -> {
                binding.starEasy.text = "\u2605"
                binding.starEasy.setTextColor(0xFFFFFF00.toInt()) // Yellow
                binding.starGood.text = "\u2605"
                binding.starGood.setTextColor(0xFFFFFF00.toInt()) // Yellow
            }
            "hard" -> {
                binding.starEasy.text = "\u2605"
                binding.starEasy.setTextColor(0xFFFFFF00.toInt()) // Yellow
                binding.starGood.text = "\u2605"
                binding.starGood.setTextColor(0xFFFFFF00.toInt()) // Yellow
                binding.starHard.text = "\u2605"
                binding.starHard.setTextColor(0xFFFFFF00.toInt()) // Yellow
            }
        }
    }

    private fun showRatingBubble(rating: String) {
        val emoji = when (rating) {
            "easy" -> "🥱"
            "good" -> "💪"
            "hard" -> "🥵"
            else -> "💪"
        }

        // Create floating TextView
        val bubbleView = TextView(requireContext()).apply {
            text = emoji
            textSize = 48f
            alpha = 1f
        }

        // Add to the root view
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(bubbleView)

        // Position at bottom center initially
        val displayMetrics = resources.displayMetrics
        val startX = (displayMetrics.widthPixels / 2f) - 50f
        val startY = displayMetrics.heightPixels - 200f
        val endY = 100f

        bubbleView.x = startX
        bubbleView.y = startY

        // Create combined animation: upward movement with side-to-side oscillation
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000 // 2 seconds
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                // Upward movement
                bubbleView.y = startY - (startY - endY) * progress

                // Side-to-side oscillation (sine wave)
                val oscillation = kotlin.math.sin(progress * 4 * Math.PI).toFloat() * 30f
                bubbleView.x = startX + oscillation

                // Fade out as it goes up
                bubbleView.alpha = 1f - progress
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootView.removeView(bubbleView)
                }
            })
        }

        animator.start()
    }

    private fun showSetCompletedBubble() {
        // Create floating TextView with clapping hands emoji
        val bubbleView = TextView(requireContext()).apply {
            text = "👏"
            textSize = 48f
            alpha = 1f
        }

        // Add to the root view
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(bubbleView)

        // Position at bottom center initially
        val displayMetrics = resources.displayMetrics
        val startX = (displayMetrics.widthPixels / 2f) - 50f
        val startY = displayMetrics.heightPixels - 200f
        val endY = 100f

        bubbleView.x = startX
        bubbleView.y = startY

        // Create combined animation: upward movement with side-to-side oscillation
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000 // 2 seconds
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                // Upward movement
                bubbleView.y = startY - (startY - endY) * progress

                // Side-to-side oscillation (sine wave)
                val oscillation = kotlin.math.sin(progress * 4 * Math.PI).toFloat() * 30f
                bubbleView.x = startX + oscillation

                // Fade out as it goes up
                bubbleView.alpha = 1f - progress
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootView.removeView(bubbleView)
                }
            })
        }

        animator.start()
    }

    private fun updateUI() {
        if (_binding == null) return

        binding.titleText.text = exercise.title

        binding.weightLabel.text = "Weight"
        binding.weightValue.text = if (exercise.weight == 0) "-" else exercise.weight.toString()

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

    override fun onWeightUpdated(newWeight: Int) {
        exercise.weight = newWeight
        updateUI()
        onUpdate(exercise)
    }

    override fun onRepsUpdated(newReps: Int) {
        exercise.reps = newReps
        updateUI()
        onUpdate(exercise)
    }

    override fun onSetsUpdated(newSets: Int) {
        exercise.sets = newSets
        if (exercise.completedSets >= exercise.sets) {
            stopTimer()
        }
        updateUI()
        onUpdate(exercise)
    }

    private fun searchExerciseOnline(exerciseName: String) {
        try {
            // Create Google search URL for the exercise
            val searchQuery = Uri.encode("$exerciseName exercise")
            val searchUrl = "https://www.google.com/search?q=$searchQuery"

            // Create intent to open browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try to open Google in browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                startActivity(intent)
            } catch (e2: Exception) {
                // If all else fails, show a toast
                android.widget.Toast.makeText(
                    requireContext(),
                    "Unable to open browser",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
