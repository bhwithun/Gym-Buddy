package com.gymbuddy

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.ItemExerciseBinding

class ExerciseAdapter(
    internal val exercises: MutableList<Exercise>,
    private val onItemClick: (Int, String) -> Unit,
    private val onStartDrag: (ExerciseViewHolder) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    onStartDrag(this)
                }
                false
            }
        }

        fun bind(exercise: Exercise) {
            binding.titleText.text = exercise.title
            val wrsText = "W:${exercise.weight} R:${exercise.reps} S:${exercise.sets}"
            val spannable = SpannableString(wrsText)
            val wEnd = "W:${exercise.weight}".length
            val rEnd = wEnd + " R:${exercise.reps}".length
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#888888")), 0, 1, 0) // gray for W
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#F1C40F")), wEnd + 1, wEnd + 2, 0) // yellow for R
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), rEnd + 1, rEnd + 2, 0) // green for S
            binding.wrsText.text = spannable
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(exercises[position])
    }

    override fun getItemCount(): Int = exercises.size
}
