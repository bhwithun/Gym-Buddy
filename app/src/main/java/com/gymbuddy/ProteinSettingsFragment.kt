package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gymbuddy.databinding.FragmentProteinSettingsBinding
import kotlinx.coroutines.launch

class ProteinSettingsFragment : Fragment() {

    private var _binding: FragmentProteinSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: ProteinSettings

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProteinSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Unit toggle
        binding.unitToggle.setOnCheckedChangeListener { _, isChecked ->
            settings = settings.copy(useLbs = !isChecked) // isChecked means kg is selected
            updateUI()
        }

        // Fitness goal radio buttons
        binding.fitnessGoalGroup.setOnCheckedChangeListener { _, checkedId ->
            val goal = when (checkedId) {
                R.id.maintainRadio -> "maintain"
                R.id.buildRadio -> "build"
                else -> "build"
            }
            settings = settings.copy(fitnessGoal = goal)
            updateUI()
        }

        // Custom goal input
        binding.customGoalSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.customGoalInput.isEnabled = isChecked
            if (!isChecked) {
                settings = settings.copy(customGoal = null)
                binding.customGoalInput.setText("")
            }
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            settings = db.proteinSettingsDao().getSettings() ?: ProteinSettings()
            updateUI()
        }
    }

    private fun updateUI() {
        // Body weight
        val weightText = if (settings.bodyWeight % 1 == 0f) {
            settings.bodyWeight.toInt().toString() // Show as integer if whole number
        } else {
            settings.bodyWeight.toString() // Show decimal if needed
        }
        binding.bodyWeightInput.setText(weightText)
        binding.unitToggle.isChecked = !settings.useLbs // Checked means kg

        // Fitness goal
        when (settings.fitnessGoal) {
            "maintain" -> binding.maintainRadio.isChecked = true
            "build" -> binding.buildRadio.isChecked = true
        }

        // Recommended rate display
        val (minRate, maxRate) = settings.getRecommendedRate()
        val defaultRate = settings.getDefaultRate()
        val unit = if (settings.useLbs) "lb" else "kg"
        binding.recommendedRateText.text = "We recommend $minRate – $maxRate g per $unit for ${if (settings.fitnessGoal == "build") "muscle building" else "maintenance"}."

        // Calculated goal
        val calculatedGoal = settings.getDailyGoal()
        binding.calculatedGoalText.text = "Calculated goal: ${calculatedGoal.toInt()}g per day"

        // Custom goal
        val hasCustomGoal = settings.customGoal != null
        binding.customGoalSwitch.isChecked = hasCustomGoal
        binding.customGoalInput.isEnabled = hasCustomGoal
        if (hasCustomGoal) {
            binding.customGoalInput.setText(settings.customGoal?.toString() ?: "")
        }
    }

    private fun saveSettings() {
        try {
            val bodyWeight = binding.bodyWeightInput.text.toString().toFloatOrNull()
            if (bodyWeight == null || bodyWeight <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid body weight", Toast.LENGTH_SHORT).show()
                return
            }

            var customGoal: Float? = null
            if (binding.customGoalSwitch.isChecked) {
                customGoal = binding.customGoalInput.text.toString().toFloatOrNull()
                if (customGoal == null || customGoal <= 0) {
                    Toast.makeText(requireContext(), "Please enter a valid custom goal", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            settings = settings.copy(
                bodyWeight = bodyWeight,
                customGoal = customGoal
            )

            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(requireContext())
                db.proteinSettingsDao().insertOrUpdate(settings)
                Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving settings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}