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
    private val onItemClick: (Int, String) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClick(adapterPosition, "edit")
            }
            binding.root.setOnLongClickListener {
                onItemClick(adapterPosition, "delete")
                true
            }
        }

        fun bind(exercise: Exercise) {
            binding.titleText.text = exercise.title
            binding.weightText.text = exercise.weight
            val repsSetsText = "${exercise.reps} x ${exercise.sets}"
            val spannable = SpannableString(repsSetsText)
            val repsEnd = exercise.reps.toString().length
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#F1C40F")), 0, repsEnd, 0) // yellow for reps
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), repsEnd, repsSetsText.length, 0) // green for sets
            binding.repsSetsText.text = spannable
            binding.notesText.text = if (exercise.notes.isBlank()) "" else exercise.notes
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
