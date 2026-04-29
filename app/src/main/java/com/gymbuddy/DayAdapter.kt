package com.gymbuddy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.ItemRoutineDayBinding

class DayAdapter(private val days: List<RoutineDayEntity>, private val onDayClick: (RoutineDayEntity) -> Unit) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    class DayViewHolder(val binding: ItemRoutineDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemRoutineDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val context = holder.itemView.context

        // Day name in white
        holder.binding.dayName.text = dayNames[day.dayOfWeek - 1]
        holder.binding.dayName.setTextColor(context.getColor(R.color.onSurface))

        // Clear existing dynamic views from grid
        holder.binding.exerciseGrid.removeAllViews()

        if (day.isRest) {
            // Show rest day text
            holder.binding.restText.visibility = android.view.View.VISIBLE
        } else {
            // Hide rest day text
            holder.binding.restText.visibility = android.view.View.GONE

            // Add exercise cards to grid - all same color (surfaceVariant)
            for (exercise in day.exercises) {

                val exerciseCard = androidx.cardview.widget.CardView(context).apply {
                    layoutParams = android.widget.GridLayout.LayoutParams().apply {
                        width = 0
                        height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                        setMargins(
                            (6 * context.resources.displayMetrics.density).toInt(), // 6dp margin
                            (6 * context.resources.displayMetrics.density).toInt(),
                            (6 * context.resources.displayMetrics.density).toInt(),
                            (6 * context.resources.displayMetrics.density).toInt()
                        )
                    }
                    radius = (12 * context.resources.displayMetrics.density) // 12dp corner radius
                    cardElevation = (6 * context.resources.displayMetrics.density) // 6dp elevation
                    setCardBackgroundColor(context.getColor(R.color.surfaceVariant))
                }

                // Create content for the card
                val cardContent = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(
                        (16 * context.resources.displayMetrics.density).toInt(), // 16dp padding
                        (16 * context.resources.displayMetrics.density).toInt(),
                        (16 * context.resources.displayMetrics.density).toInt(),
                        (16 * context.resources.displayMetrics.density).toInt()
                    )
                }

                // Exercise title in bright white
                val titleText = android.widget.TextView(context).apply {
                    text = exercise.title
                    textSize = 16f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(context.getColor(R.color.onSurface))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                cardContent.addView(titleText)

                // Exercise details (W:R:S format) with colored text
                val wrsText = "W:${exercise.weight} R:${exercise.reps} S:${exercise.sets}"
                val spannable = android.text.SpannableString(wrsText)

                val wStart = 2
                val wEnd = wStart + exercise.weight.toString().length
                val rStart = wEnd + 3
                val rEnd = rStart + exercise.reps.toString().length
                val sStart = rEnd + 3
                val sEnd = sStart + exercise.sets.toString().length

                spannable.setSpan(android.text.style.ForegroundColorSpan(context.getColor(R.color.secondary)), wStart, wEnd, 0)
                spannable.setSpan(android.text.style.ForegroundColorSpan(context.getColor(R.color.weight_color)), rStart, rEnd, 0)
                spannable.setSpan(android.text.style.ForegroundColorSpan(context.getColor(R.color.sets_color)), sStart, sEnd, 0)

                val detailsText = android.widget.TextView(context).apply {
                    text = spannable
                    textSize = 14f
                    setTextColor(context.getColor(R.color.onSurfaceVariant))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = (6 * context.resources.displayMetrics.density).toInt() // 6dp top margin
                    }
                }
                cardContent.addView(detailsText)

                exerciseCard.addView(cardContent)
                holder.binding.exerciseGrid.addView(exerciseCard)
            }
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount() = days.size
}
