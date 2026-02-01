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
            binding.recyclerView.adapter = DayAdapter(days) { day ->
                val fragment = DayDetailDialogFragment.newInstance(day)
                fragment.show(parentFragmentManager, "day_detail")
            }
        }

        binding.exportButton.setOnClickListener {
            lifecycleScope.launch {
                val days = withContext(Dispatchers.IO) { dao.getAll() }
                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(days)
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
                    val gson = Gson()
                    val type = object : TypeToken<List<RoutineDayEntity>>() {}.type
                    val importedDays: List<RoutineDayEntity> = gson.fromJson(json, type)
                    if (importedDays.size == 7) {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                dao.deleteAll()
                                dao.insertAll(*importedDays.toTypedArray())
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
