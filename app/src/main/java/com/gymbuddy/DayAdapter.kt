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
        holder.binding.dayName.text = dayNames[day.dayOfWeek - 1]

        // Hide all exercise views first
        holder.binding.exercise1.visibility = View.GONE
        holder.binding.exercise2.visibility = View.GONE
        holder.binding.exercise3.visibility = View.GONE
        holder.binding.exercise4.visibility = View.GONE
        holder.binding.exercise5.visibility = View.GONE
        holder.binding.restText.visibility = View.GONE

        if (day.isRest) {
            holder.binding.restText.visibility = View.VISIBLE
        } else {
            val textViews = listOf(
                holder.binding.exercise1,
                holder.binding.exercise2,
                holder.binding.exercise3,
                holder.binding.exercise4,
                holder.binding.exercise5
            )
            for (i in day.exercises.indices) {
                if (i < textViews.size) {
                    textViews[i].text = day.exercises[i].title
                    textViews[i].visibility = View.VISIBLE
                }
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
                android.view.DragEvent.ACTION_DROP -> {
                    val clipData = event.clipData
                    if (clipData != null && clipData.itemCount > 0) {
                        val draggedDayOfWeek = clipData.getItemAt(0).text.toString().toInt()
                        onExercisesSwapped(draggedDayOfWeek, day.dayOfWeek)
                    }
                }
            }
            true
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount() = days.size
}
