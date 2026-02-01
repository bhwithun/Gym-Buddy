package com.gymbuddy

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.DialogDayDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDetailDialogFragment : DialogFragment(), ExerciseEditorDialogFragment.ExerciseEditorListener {

    private var _binding: DialogDayDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var day: RoutineDayEntity
    private lateinit var exercises: MutableList<Exercise>

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

        exercises = day.exercises.toMutableList()
        binding.restDayText.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE

        val adapter = ExerciseAdapter(exercises) { position, action ->
            if (action == "edit") {
                val fragment = ExerciseEditorDialogFragment.newInstance(position, exercises[position])
                fragment.setTargetFragment(this, 0)
                fragment.show(parentFragmentManager, "exercise_editor")
            }
        }
        binding.exercisesRecyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = android.graphics.Paint()
                    val icon = resources.getDrawable(android.R.drawable.ic_menu_close_clear_cancel, null)

                    if (dX > 0) {
                        paint.color = Color.RED
                        val background = android.graphics.RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.left + dX, itemView.bottom.toFloat())
                        c.drawRect(background, paint)

                        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        icon.draw(c)
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
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
            val newExercise = Exercise("New Exercise", 0, 10, 3, "")
            exercises.add(newExercise)
            binding.exercisesRecyclerView.adapter?.notifyItemInserted(exercises.size - 1)
            saveToDb()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun saveToDb() {
        lifecycleScope.launch {
            val updatedDay = RoutineDayEntity(day.dayOfWeek, exercises.isEmpty(), exercises)
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onExerciseUpdated(wrapper: ExerciseWrapper) {
        when (wrapper.status) {
            "updated" -> {
                exercises[wrapper.index] = wrapper.exercise
                binding.exercisesRecyclerView.adapter?.notifyItemChanged(wrapper.index)
                saveToDb()
            }
            "canceled" -> {
                // Do nothing
            }
        }
    }

    override fun onDayUpdated(day: RoutineDayEntity) {
        // Not used
    }
}
