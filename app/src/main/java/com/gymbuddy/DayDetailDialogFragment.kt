package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gymbuddy.databinding.DialogDayDetailBinding

class DayDetailDialogFragment : DialogFragment() {

    private var _binding: DialogDayDetailBinding? = null
    private val binding get() = _binding!!

    private var startDayOfWeek: Int = 1

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
        startDayOfWeek = day.dayOfWeek
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

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Create adapter for ViewPager
        val adapter = DayDetailPagerAdapter(this, startDayOfWeek)
        binding.viewPager.adapter = adapter

        // Set initial position
        binding.viewPager.setCurrentItem(startDayOfWeek - 1, false)
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

    private class DayDetailPagerAdapter(
        fragment: DialogFragment,
        private val startDayOfWeek: Int
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 7

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            val dayOfWeek = position + 1
            return DayDetailPageFragment.newInstance(dayOfWeek)
        }
    }
}
