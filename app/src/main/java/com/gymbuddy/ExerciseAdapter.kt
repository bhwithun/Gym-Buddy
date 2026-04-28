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
            val weight = exercise.weight.toString()
            val reps = exercise.reps.toString()
            val sets = exercise.sets.toString()

            val wrsText = "W:$weight R:$reps S:$sets"
            val spannable = SpannableString(wrsText)

            val wStart = 2
            val wEnd = wStart + weight.length
            val rStart = wEnd + 3
            val rEnd = rStart + reps.length
            val sStart = rEnd + 3
            val sEnd = sStart + sets.length

            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FFFF00")), wStart, wEnd, 0)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00FFFF")), rStart, rEnd, 0)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#00FF00")), sStart, sEnd, 0)

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
