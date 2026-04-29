package com.gymbuddy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.FragmentRoutineBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.gymbuddy.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineFragment : Fragment() {

    private var _binding: FragmentRoutineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = AppDatabase.getDatabase(requireContext()).routineDao()
        dao.getAllLive().observe(viewLifecycleOwner) { days ->
            val adapter = DayAdapter(days) { day ->
                val fragment = DayDetailDialogFragment.newInstance(day)
                fragment.show(parentFragmentManager, "day_detail")
            }
            binding.recyclerView.adapter = adapter
        }

        binding.exportButton.setOnClickListener {
            lifecycleScope.launch {
                val days = withContext(Dispatchers.IO) { dao.getAll() }
                val exportDays = days.map { day ->
                    ExportRoutineDay(
                        dayOfWeek = DayUtils.getDayName(day.dayOfWeek),
                        exercises = day.exercises.map { exercise ->
                            ExportExercise(
                                title = exercise.title,
                                weight = exercise.weight,
                                reps = exercise.reps,
                                sets = exercise.sets,
                                notes = exercise.notes,
                                easyGoodOrHard = exercise.rating
                            )
                        }
                    )
                }
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(exportDays)
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Routine", json)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Routine exported to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        binding.importButton.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val json = clip.getItemAt(0).text.toString()
                try {
                    val gson = GsonBuilder()
                        .registerTypeAdapter(ExportRoutineDay::class.java, ExportRoutineDayDeserializer())
                        .create()
                    val type = object : TypeToken<List<ExportRoutineDay>>() {}.type
                    val importedExportDays: List<ExportRoutineDay> = gson.fromJson(json, type)

                    if (importedExportDays.size == 7) {
                        // Convert ExportRoutineDay to RoutineDayEntity
                        val importedDays = importedExportDays.map { exportDay ->
                            val dayOfWeekInt = DayUtils.getDayOfWeek(exportDay.dayOfWeek)
                            val isRest = exportDay.exercises.isEmpty()
                            val exercises = exportDay.exercises.map { exportExercise ->
                                Exercise(
                                    title = exportExercise.title,
                                    weight = exportExercise.weight,
                                    reps = exportExercise.reps,
                                    sets = exportExercise.sets,
                                    notes = exportExercise.notes,
                                    rating = exportExercise.easyGoodOrHard
                                )
                            }
                            RoutineDayEntity(dayOfWeekInt, isRest, exercises)
                        }

                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                dao.deleteAll()
                                dao.insertAll(*importedDays.toTypedArray())

                                // Delete today's workout log so it loads fresh from the new routine
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                                AppDatabase.getDatabase(requireContext()).workoutLogDao().deleteByDate(dateStr)
                            }
                            Toast.makeText(requireContext(), "Routine imported successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Invalid routine: must contain exactly 7 days", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Invalid JSON format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "No data in clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
