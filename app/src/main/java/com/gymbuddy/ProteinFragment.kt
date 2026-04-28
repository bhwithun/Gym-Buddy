package com.gymbuddy

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.FragmentProteinBinding
import com.gymbuddy.databinding.ItemFoodBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProteinFragment : Fragment() {

    private var _binding: FragmentProteinBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: ProteinSettings
    private lateinit var todayLog: DailyProteinLog
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProteinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadData()
    }

    private fun setupUI() {
        binding.resetButton.setOnClickListener {
            resetAllSliders()
        }

        binding.settingsButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProteinSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.foodRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = FoodAdapter(FoodData.foodList, { foodId, portions ->
            updateFoodPortions(foodId, portions)
        }, {
            updateTotalsInRealTime()
        })
        binding.foodRecyclerView.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val today = dateFormat.format(Date())

            // Load settings
            settings = db.proteinSettingsDao().getSettings() ?: ProteinSettings()

            // Load or create today's log
            todayLog = db.dailyProteinLogDao().getLogForDate(today) ?: DailyProteinLog(today)

            updateUI()
        }
    }

    private fun updateFoodPortions(foodId: String, portions: Int) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            todayLog = todayLog.updatePortions(foodId, portions)
            db.dailyProteinLogDao().insertOrUpdate(todayLog)
            updateUI()
        }
    }

    private fun updateUI() {
        val totalProtein = todayLog.getTotalProtein(FoodData.foodList)
        val goal = settings.getDailyGoal()
        val percentage = if (goal > 0) (totalProtein / goal * 100).toInt() else 0

        // Update header
        binding.proteinAmountText.text = "${totalProtein}g / ${goal.toInt()}g"
        binding.percentageText.text = "$percentage%"
        binding.progressBar.progress = percentage.coerceIn(0, 100)

        // Update progress bar color
        val color = when {
            percentage >= 80 -> Color.GREEN
            percentage >= 50 -> Color.YELLOW
            else -> Color.RED
        }
        binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)

        // Update settings info
        val rate = settings.getDefaultRate()
        val goalText = if (settings.fitnessGoal == "build") "Building muscle" else "Maintaining muscle"
        binding.settingsInfoText.text = "${rate}g/lb • $goalText"

        // Update adapter
        (binding.foodRecyclerView.adapter as? FoodAdapter)?.apply {
            updateData(todayLog)
            setSettings(settings)
        }
    }

    private fun resetAllSliders() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val today = dateFormat.format(Date())
            val resetLog = DailyProteinLog(today) // Creates empty log with all zeros
            db.dailyProteinLogDao().insertOrUpdate(resetLog)
            todayLog = resetLog
            updateUI()
            showResetBubble()
        }
    }

    private fun showResetBubble() {
        // Create floating TextView with sponge emoji
        val bubbleView = TextView(requireContext()).apply {
            text = "🧽"
            textSize = 48f
            alpha = 1f
        }

        // Add to the root view
        val rootView = requireActivity().findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(bubbleView)

        // Position at bottom center initially
        val displayMetrics = resources.displayMetrics
        val startX = (displayMetrics.widthPixels / 2f) - 50f
        val startY = displayMetrics.heightPixels - 200f
        val endY = 100f

        bubbleView.x = startX
        bubbleView.y = startY

        // Create combined animation: upward movement with side-to-side oscillation
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2000 // 2 seconds
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                // Upward movement
                bubbleView.y = startY - (startY - endY) * progress

                // Side-to-side oscillation (sine wave)
                val oscillation = kotlin.math.sin(progress * 4 * Math.PI).toFloat() * 30f
                bubbleView.x = startX + oscillation

                // Fade out as it goes up
                bubbleView.alpha = 1f - progress
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    rootView.removeView(bubbleView)
                }
            })
        }

        animator.start()
    }

    private fun updateTotalsInRealTime() {
        // Calculate total protein from all current slider positions
        var newTotalProtein = 0
        val adapter = binding.foodRecyclerView.adapter as? FoodAdapter
        for (food in FoodData.foodList) {
            val portions = adapter?.getCurrentSliderValue(food.id) ?: 0
            newTotalProtein += portions * food.proteinPerPortion
        }

        val goal = settings.getDailyGoal()
        val percentage = if (goal > 0) (newTotalProtein / goal * 100).toInt() else 0

        // Update header in real-time
        binding.proteinAmountText.text = "${newTotalProtein}g / ${goal.toInt()}g"
        binding.percentageText.text = "$percentage%"
        binding.progressBar.progress = percentage.coerceIn(0, 100)

        // Update progress bar color
        val color = when {
            percentage >= 80 -> Color.GREEN
            percentage >= 50 -> Color.YELLOW
            else -> Color.RED
        }
        binding.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }

    override fun onResume() {
        super.onResume()
        // Reload data in case settings changed
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FoodAdapter(
    private val foodList: List<FoodItem>,
    private val onPortionsChanged: (String, Int) -> Unit,
    private val onRealTimeUpdate: () -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private var currentLog: DailyProteinLog = DailyProteinLog("")
    private lateinit var settings: ProteinSettings
    private val currentSliderValues = mutableMapOf<String, Int>()

    fun updateData(log: DailyProteinLog) {
        currentLog = log
        // Initialize current slider values from the log
        currentSliderValues.clear()
        for (food in foodList) {
            currentSliderValues[food.id] = log.getPortionsForFood(food.id)
        }
        notifyDataSetChanged()
    }

    fun setSettings(settings: ProteinSettings) {
        this.settings = settings
    }

    fun getCurrentSliderValue(foodId: String): Int {
        return currentSliderValues[foodId] ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(foodList[position])
    }

    override fun getItemCount() = foodList.size

    inner class FoodViewHolder(private val binding: ItemFoodBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(food: FoodItem) {
            binding.foodNameText.text = "${food.name} (${food.servingDescription})"

            val portions = currentSliderValues[food.id] ?: 0
            val totalProtein = portions * food.proteinPerPortion

            // Update protein text to show calculation: "3 x 12g = 36g" or "0 x 12g = 0g"
            binding.proteinText.text = "$portions × ${food.proteinPerPortion}g = ${totalProtein}g"

            binding.portionsSlider.progress = portions

            binding.portionsSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        // Update current slider value
                        currentSliderValues[food.id] = progress

                        val newTotal = progress * food.proteinPerPortion
                        binding.proteinText.text = "$progress × ${food.proteinPerPortion}g = ${newTotal}g"
                        // Update totals in real-time
                        onRealTimeUpdate()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { newPortions ->
                        onPortionsChanged(food.id, newPortions)
                    }
                }
            })
        }


    }
}