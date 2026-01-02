package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymbuddy.databinding.FragmentDaySummaryPageBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DaySummaryPageFragment : Fragment() {

    private var _binding: FragmentDaySummaryPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var date: String

    companion object {
        private const val ARG_DATE = "date"

        fun newInstance(date: String): DaySummaryPageFragment {
            val fragment = DaySummaryPageFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        date = arguments?.getString(ARG_DATE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDaySummaryPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dateText.text = date
        binding.exercisesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadDaySummary()
    }

    private fun loadDaySummary() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val routineDay = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(dayOfWeek)
            }

            if (routineDay == null || routineDay.isRest) {
                binding.statusText.text = "Rest Day"
                binding.exercisesRecyclerView.adapter = ExerciseSummaryAdapter(emptyList(), emptyList())
            } else {
                val log = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext()).workoutLogDao().getByDate(date)
                }

                val plannedExercises = routineDay.exercises
                val performedExercises = if (log != null) {
                    Gson().fromJson(log.loggedJson, Array<Exercise>::class.java).toList()
                } else {
                    emptyList()
                }

                binding.statusText.text = if (performedExercises.isNotEmpty()) "Performed" else "Planned"
                binding.exercisesRecyclerView.adapter = ExerciseSummaryAdapter(plannedExercises, performedExercises)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseSummaryAdapter(
        private val planned: List<Exercise>,
        private val performed: List<Exercise>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<ExerciseSummaryAdapter.ExerciseViewHolder>() {

        class ExerciseViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val plannedText: android.widget.TextView = itemView.findViewById(R.id.plannedText)
            val performedText: android.widget.TextView = itemView.findViewById(R.id.performedText)
            val progressPieChart: ProgressPieChart = itemView.findViewById(R.id.progressPieChart)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise_summary, parent, false)
            return ExerciseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            val plannedExercise = planned.getOrNull(position)
            val performedExercise = performed.find { it.title == plannedExercise?.title }

            holder.plannedText.text = plannedExercise?.let { "${it.title}: ${it.weight} x ${it.sets} sets" } ?: ""
            holder.performedText.text = performedExercise?.let { "Completed: ${it.completedSets}/${it.sets} sets" } ?: "Not performed"

            val progress = performedExercise?.let { it.completedSets.toFloat() / it.sets } ?: 0f
            holder.progressPieChart.setProgress(progress)
        }

        override fun getItemCount() = planned.size
    }
}
