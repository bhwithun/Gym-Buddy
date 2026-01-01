package com.gymbuddy

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.gymbuddy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExerciseAdapter
    private val exercises = mutableListOf<Exercise>()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sample exercises
        exercises.add(Exercise("Goblet Squats", "45", 12, 4, "Feet shoulder-width apart"))
        exercises.add(Exercise("Bench Press", "2 x 45", 8, 5, "Arch back slightly"))
        exercises.add(Exercise("Deadlift", "135", 5, 3, "Mixed grip"))
        exercises.add(Exercise("Pull-ups", "BW", 10, 4, ""))

        adapter = ExerciseAdapter(exercises) { position ->
            val fragment = ExerciseEditorDialogFragment.newInstance(position, exercises[position])
            fragment.show(supportFragmentManager, "exercise_editor")
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        Log.d("MainActivity", gson.toJson(wrapper))
        when (wrapper.status) {
            "updated" -> {
                exercises[wrapper.index] = wrapper.exercise
                adapter.notifyItemChanged(wrapper.index)
                Toast.makeText(this, "Exercise ${wrapper.index} updated", Toast.LENGTH_SHORT).show()
            }
            "canceled" -> {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
