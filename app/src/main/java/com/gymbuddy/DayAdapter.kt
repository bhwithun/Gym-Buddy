package com.gymbuddy

import android.content.ClipData
import android.content.ClipDescription
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.ItemRoutineDayBinding

class DayAdapter(private val days: List<RoutineDayEntity>, private val onDayClick: (RoutineDayEntity) -> Unit, private val onExercisesSwapped: (Int, Int) -> Unit) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    class DayViewHolder(val binding: ItemRoutineDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemRoutineDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        val context = holder.itemView.context

        // Highlight today's day
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val isToday = day.dayOfWeek == today
        holder.binding.dayName.text = dayNames[day.dayOfWeek - 1]
        holder.binding.dayName.setTextColor(
            if (isToday) context.getColor(R.color.secondary)
            else context.getColor(R.color.onSurface)
        )

        // Clear existing dynamic views
        holder.binding.exerciseBox.removeAllViews()

        if (day.isRest) {
            val restText = android.widget.TextView(context).apply {
                text = "Rest Day"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.ITALIC)
                setTextColor(context.getColor(R.color.onSurfaceVariant))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            holder.binding.exerciseBox.addView(restText)
        } else {
            // Add all exercises dynamically
            for (exercise in day.exercises) {
                val exerciseText = android.widget.TextView(context).apply {
                    text = exercise.title
                    textSize = 14f
                    setTextColor(context.getColor(R.color.onSurface))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (4 * context.resources.displayMetrics.density).toInt() // 4dp margin
                    }
                }
                holder.binding.exerciseBox.addView(exerciseText)
            }
        }

        // Set up drag and drop
        holder.binding.exerciseBox.setOnLongClickListener {
            val clipData = ClipData.newPlainText("dayOfWeek", day.dayOfWeek.toString())
            val dragShadow = View.DragShadowBuilder(it)
            it.startDragAndDrop(clipData, dragShadow, null, 0)
            true
        }

        holder.itemView.setOnDragListener { v, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    // Highlight potential drop target
                    v.alpha = 0.7f
                    v.elevation = 16f
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                    // More prominent highlight when hovering
                    v.alpha = 0.9f
                    v.elevation = 24f
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED -> {
                    // Return to drag-started state
                    v.alpha = 0.7f
                    v.elevation = 16f
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    // Reset visual state
                    v.alpha = 1.0f
                    v.elevation = 8f
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    // Reset visual state immediately
                    v.alpha = 1.0f
                    v.elevation = 8f

                    val clipData = event.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val draggedDayOfWeek = clipData.getItemAt(0).text.toString().toInt()
                        onExercisesSwapped(draggedDayOfWeek, day.dayOfWeek)
                    }
                    true
                }
                else -> true
            }
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount() = days.size
}
