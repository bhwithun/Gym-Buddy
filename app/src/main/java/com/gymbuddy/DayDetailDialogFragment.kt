package com.gymbuddy

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.DialogDayDetailBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gymbuddy.Exercise
import com.gymbuddy.RoutineDayEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDetailDialogFragment : DialogFragment(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private var _binding: DialogDayDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var day: RoutineDayEntity
    private var exercises = mutableListOf<Exercise>()
    private val gson = Gson()

    companion object {
        private const val ARG_DAY = "day"

        fun newInstance(day: RoutineDayEntity): DayDetailDialogFragment {
            val fragment = DayDetailDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_DAY, day)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        day = arguments?.getSerializable(ARG_DAY) as RoutineDayEntity
        exercises.addAll(day.exercises)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDayDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        binding.dayTitle.text = dayNames[day.dayOfWeek - 1]
        binding.restDayText.visibility = View.GONE

        val adapter = ExerciseAdapter(exercises) { position, action ->
            if (action == "edit") {
                val fragment = ExerciseEditorDialogFragment.newInstance(position, exercises[position])
                fragment.setTargetFragment(this@DayDetailDialogFragment, 0)
                fragment.show(parentFragmentManager, "exercise_editor")
            }
        }
        binding.exercisesRecyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                exercises.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveToDb()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.exercisesRecyclerView)

        binding.addButton.setOnClickListener {
            val newExercise = Exercise("New Exercise", "0", 10, 3, "")
            exercises.add(newExercise)
            adapter.notifyItemInserted(exercises.size - 1)
            saveToDb()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }



    private fun saveToDb() {
        lifecycleScope.launch {
            val updatedDay = day.copy(exercises = exercises)
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)
            }
            (activity as? ExerciseEditorDialogFragment.ExerciseEditorListener)?.onDayUpdated(updatedDay)
        }
    }

    override fun onStart() {
        super.onStart()
        val displayMetrics = resources.displayMetrics
        val height = (displayMetrics.heightPixels * 0.8).toInt()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            height
        )
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        if (wrapper.status == "updated") {
            exercises[wrapper.index] = wrapper.exercise
            binding.exercisesRecyclerView.adapter?.notifyItemChanged(wrapper.index)
        }
    }

    override fun onDayUpdated(day: RoutineDayEntity) {
        // Not used
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
