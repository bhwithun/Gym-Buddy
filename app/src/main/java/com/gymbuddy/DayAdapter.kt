package com.gymbuddy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.ItemRoutineDayBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DayAdapter(private val days: List<RoutineDayEntity>, private val onDayClick: (RoutineDayEntity) -> Unit) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    private val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val gson = Gson()

    class DayViewHolder(val binding: ItemRoutineDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemRoutineDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.binding.dayName.text = dayNames[day.dayOfWeek - 1]

        val exercises: List<Exercise> = gson.fromJson(day.exercisesJson, object : TypeToken<List<Exercise>>() {}.type)
        if (day.isRest || exercises.isEmpty()) {
            holder.binding.exerciseCount.text = ""
            holder.binding.restIndicator.visibility = android.view.View.VISIBLE
        } else {
            holder.binding.exerciseCount.text = "${exercises.size} exercises"
            holder.binding.restIndicator.visibility = android.view.View.GONE
        }

        holder.itemView.setOnClickListener { onDayClick(day) }
    }

    override fun getItemCount() = days.size
}
