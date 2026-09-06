package com.gymbuddy

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()

    companion object {
        const val EXTRA_OPEN_WORKOUT = "com.gymbuddy.OPEN_WORKOUT"
        const val EXTRA_EXERCISE_INDEX = "com.gymbuddy.EXERCISE_INDEX"
    }

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
                        RoutineDayEntity(1, true, emptyList()), // Sun rest
                        RoutineDayEntity(2, false, listOf(
                            Exercise("Push-ups", 0, 10, 3, ""),
                            Exercise("Squats", 0, 15, 3, ""),
                            Exercise("Planks", 0, 30, 3, "seconds")
                        )),
                        RoutineDayEntity(3, false, listOf(
                            Exercise("Bench Press", 45, 8, 4, ""),
                            Exercise("Rows", 50, 10, 3, ""),
                            Exercise("Lunges", 0, 12, 3, "per leg")
                        )),
                        RoutineDayEntity(4, false, listOf(
                            Exercise("Deadlift", 135, 5, 3, ""),
                            Exercise("Pull-ups", 0, 8, 4, ""),
                            Exercise("Dips", 0, 10, 3, "")
                        )),
                        RoutineDayEntity(5, false, listOf(
                            Exercise("Squats", 95, 8, 4, ""),
                            Exercise("Overhead Press", 45, 8, 3, ""),
                            Exercise("Bicep Curls", 25, 12, 3, "")
                        )),
                        RoutineDayEntity(6, false, listOf(
                            Exercise("Bench Press", 50, 6, 4, ""),
                            Exercise("Rows", 55, 8, 3, ""),
                            Exercise("Planks", 0, 45, 3, "seconds")
                        )),
                        RoutineDayEntity(7, true, emptyList()) // Sat rest
                    )
                    routineDao.insertAll(*sampleDays)
                }
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Haptic feedback
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(50)
            }

            when (item.itemId) {
                R.id.navigation_routine -> replaceFragment(RoutineFragment())
                R.id.navigation_workout -> replaceFragment(workoutFragmentFromIntent())
                R.id.navigation_protein -> replaceFragment(ProteinFragment())
                R.id.navigation_about -> replaceFragment(AboutFragment())
            }
            true
        }

        if (intent.getBooleanExtra(EXTRA_OPEN_WORKOUT, false)) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_workout
        } else if (savedInstanceState == null) {
            replaceFragment(RoutineFragment())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_WORKOUT, false)) {
            if (binding.bottomNavigation.selectedItemId == R.id.navigation_workout) {
                replaceFragment(workoutFragmentFromIntent())
            } else {
                binding.bottomNavigation.selectedItemId = R.id.navigation_workout
            }
        }
    }

    private fun workoutFragmentFromIntent(): WorkoutFragment {
        val page = intent.getIntExtra(EXTRA_EXERCISE_INDEX, -1)
        intent.removeExtra(EXTRA_EXERCISE_INDEX)
        return if (page >= 0) WorkoutFragment.withInitialPage(page) else WorkoutFragment()
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
