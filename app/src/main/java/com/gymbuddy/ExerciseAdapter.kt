package com.gymbuddy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.ItemExerciseBinding

class ExerciseAdapter(
    private val exercises: MutableList<Exercise>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClick(adapterPosition)
            }
        }

        fun bind(exercise: Exercise) {
            binding.titleText.text = exercise.title
            binding.detailsText.text = "${exercise.weight} | ${exercise.reps} x ${exercise.sets}"
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
