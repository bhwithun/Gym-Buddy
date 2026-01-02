package com.gymbuddy

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.FragmentLogBinding
import com.gymbuddy.databinding.ItemCalendarDayBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class CalendarDay(val day: Int, val status: String, val date: String?)

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        binding.monthYearText.text = monthName

        binding.logRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        loadCalendarDays()
    }

    private fun loadCalendarDays() {
        lifecycleScope.launch {
            val days = mutableListOf<CalendarDay>()
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            // Set to first day of month
            calendar.set(currentYear, currentMonth, 1)
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            // Add empty cells for days before the first day
            for (i in Calendar.SUNDAY until firstDayOfWeek) {
                days.add(CalendarDay(0, "empty", null))
            }

            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()

            for (day in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, day)
                val date = calendar.time
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                val status = if (calendar.after(today)) {
                    "future"
                } else {
                    val routineDay = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(dayOfWeek)
                    }

                    if (routineDay == null || routineDay.isRest) {
                        "rest"
                    } else {
                        val log = withContext(Dispatchers.IO) {
                            AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(dateStr)
                        }
                        if (log != null) {
                            val exercises = Gson().fromJson(log.loggedJson, Array<Exercise>::class.java)
                            val totalSets = exercises.sumOf { it.sets }
                            val completedSets = exercises.sumOf { it.completedSets }
                            when {
                                completedSets >= totalSets -> "full"
                                completedSets > 0 -> "partial"
                                else -> "skipped"
                            }
                        } else {
                            "skipped"
                        }
                    }
                }

                days.add(CalendarDay(day, status, dateStr))
            }

            binding.logRecyclerView.adapter = CalendarAdapter(days) { date ->
                if (date != null) {
                    // Show summary dialog
                    val dialog = DaySummaryDialogFragment.newInstance(date)
                    dialog.show(parentFragmentManager, "day_summary")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class CalendarAdapter(private val days: List<CalendarDay>, private val onDayClick: (String?) -> Unit) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

        class CalendarViewHolder(val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
            val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CalendarViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
            val day = days[position]
            if (day.day == 0) {
                holder.binding.dayNumberText.text = ""
                holder.binding.root.background = null
            } else {
                holder.binding.dayNumberText.text = day.day.toString()
                val color = when (day.status) {
                    "full" -> Color.GREEN
                    "partial" -> Color.YELLOW
                    "skipped" -> Color.RED
                    "rest" -> Color.GRAY
                    else -> Color.TRANSPARENT
                }
                val drawable = GradientDrawable()
                drawable.setColor(color)
                drawable.setStroke(2, Color.parseColor("#CCCCCC"))
                drawable.cornerRadius = 4f
                holder.binding.root.background = drawable
            }

            holder.itemView.setOnClickListener {
                onDayClick(day.date)
            }
        }

        override fun getItemCount() = days.size
    }
}
