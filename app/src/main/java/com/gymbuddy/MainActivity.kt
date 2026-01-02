package com.gymbuddy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.gymbuddy.databinding.ActivityMainBinding
import com.gymbuddy.Exercise
import com.gymbuddy.ExerciseWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val routineDao = db.routineDao()
                if (routineDao.getAll().isEmpty()) {
                    val sampleDays = arrayOf(
                        RoutineDayEntity(1, true, "[]"), // Sun rest
                        RoutineDayEntity(2, false, gson.toJson(listOf(
                            Exercise("Push-ups", "BW", 10, 3, ""),
                            Exercise("Squats", "BW", 15, 3, ""),
                            Exercise("Planks", "BW", 30, 3, "seconds")
                        ))),
                        RoutineDayEntity(3, false, gson.toJson(listOf(
                            Exercise("Bench Press", "45", 8, 4, ""),
                            Exercise("Rows", "50", 10, 3, ""),
                            Exercise("Lunges", "BW", 12, 3, "per leg")
                        ))),
                        RoutineDayEntity(4, false, gson.toJson(listOf(
                            Exercise("Deadlift", "135", 5, 3, ""),
                            Exercise("Pull-ups", "BW", 8, 4, ""),
                            Exercise("Dips", "BW", 10, 3, "")
                        ))),
                        RoutineDayEntity(5, false, gson.toJson(listOf(
                            Exercise("Squats", "95", 8, 4, ""),
                            Exercise("Overhead Press", "45", 8, 3, ""),
                            Exercise("Bicep Curls", "25", 12, 3, "")
                        ))),
                        RoutineDayEntity(6, false, gson.toJson(listOf(
                            Exercise("Bench Press", "50", 6, 4, ""),
                            Exercise("Deadlift", "155", 4, 3, ""),
                            Exercise("Planks", "BW", 45, 3, "seconds")
                        ))),
                        RoutineDayEntity(7, true, "[]") // Sat rest
                    )
                    routineDao.insertAll(*sampleDays)
                }
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> replaceFragment(HomeFragment())
                R.id.navigation_routine -> replaceFragment(RoutineFragment())
                R.id.navigation_workout -> replaceFragment(WorkoutFragment())
                R.id.navigation_log -> replaceFragment(LogFragment())
            }
            true
        }

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        // For now, just show a toast since exercises are not in a global list
        when (wrapper.status) {
            "updated" -> Toast.makeText(this, "Exercise updated", Toast.LENGTH_SHORT).show()
            "canceled" -> Toast.makeText(this, "Edit canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDayUpdated(day: RoutineDayEntity) {
        Toast.makeText(this, "Day updated", Toast.LENGTH_SHORT).show()
    }
}
