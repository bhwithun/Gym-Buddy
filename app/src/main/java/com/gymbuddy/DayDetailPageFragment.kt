package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gymbuddy.databinding.FragmentDayDetailPageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DayDetailPageFragment : Fragment() {

    private var _binding: FragmentDayDetailPageBinding? = null
    private val binding get() = _binding!!

    private var dayOfWeek: Int = 1
    private lateinit var exercises: MutableList<Exercise>

    companion object {
        private const val ARG_DAY_OF_WEEK = "day_of_week"

        fun newInstance(dayOfWeek: Int): DayDetailPageFragment {
            val fragment = DayDetailPageFragment()
            val args = Bundle()
            args.putInt(ARG_DAY_OF_WEEK, dayOfWeek)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dayOfWeek = arguments?.getInt(ARG_DAY_OF_WEEK) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayDetailPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        binding.dayTitle.text = dayNames[dayOfWeek - 1]

        lifecycleScope.launch {
            val routineDay = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getByDayOfWeek(dayOfWeek)
            }
            exercises = routineDay?.exercises?.toMutableList() ?: mutableListOf()
            binding.restDayText.visibility = if (exercises.isEmpty()) View.VISIBLE else View.GONE

            val adapter = ExerciseAdapter(exercises) { position, action ->
                if (action == "edit") {
                    val fragment = ExerciseEditorDialogFragment.newInstance(position, exercises[position])
                    fragment.setTargetFragment(parentFragment, 0)
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
                            paint.color = android.graphics.Color.RED
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
        }

        binding.addButton.setOnClickListener {
            val newExercise = Exercise("New Exercise", "0", 10, 3, "")
            exercises.add(newExercise)
            binding.exercisesRecyclerView.adapter?.notifyItemInserted(exercises.size - 1)
            saveToDb()
        }
    }

    private fun saveToDb() {
        lifecycleScope.launch {
            val updatedDay = RoutineDayEntity(dayOfWeek, exercises.isEmpty(), exercises)
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().insertAll(updatedDay)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
