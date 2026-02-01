package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gymbuddy.databinding.DialogDayDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DayDetailDialogFragment : DialogFragment() {

    private var _binding: DialogDayDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var days: List<RoutineDayEntity>
    private var currentDayIndex: Int = 0

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
        val day = arguments?.getSerializable(ARG_DAY) as RoutineDayEntity

        runBlocking {
            days = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).routineDao().getAll()
            }
            currentDayIndex = days.indexOfFirst { it.dayOfWeek == day.dayOfWeek }
        }
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

        binding.viewPager.adapter = DayPagerAdapter(this)
        binding.viewPager.setCurrentItem(currentDayIndex, false)

        // Add rotation animation for page transitions
        binding.viewPager.setPageTransformer { page, position ->
            page.rotationY = position * -30f
            page.alpha = 1 - Math.abs(position)
        }

        binding.closeButton.setOnClickListener {
            dismiss()
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

    private inner class DayPagerAdapter(fragment: DialogFragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = days.size

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            return DayDetailPageFragment.newInstance(days[position])
        }
    }
}
