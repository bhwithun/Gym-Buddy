package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.gymbuddy.databinding.DialogDaySummaryBinding
import java.text.SimpleDateFormat
import java.util.*

class DaySummaryDialogFragment : DialogFragment() {

    private var _binding: DialogDaySummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var startDate: String

    companion object {
        private const val ARG_DATE = "date"
        private const val EPOCH_OFFSET = 20000 // Offset to ensure positive positions (starts ~2025)

        fun newInstance(date: String): DaySummaryDialogFragment {
            val fragment = DaySummaryDialogFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }

        // Convert date string to ViewPager position
        fun dateToPosition(dateStr: String): Int {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(dateStr) ?: Date()
            val daysSinceEpoch = (date.time / (1000 * 60 * 60 * 24)).toInt()
            return daysSinceEpoch - EPOCH_OFFSET
        }

        // Convert ViewPager position to date string
        fun positionToDate(position: Int): String {
            val daysSinceEpoch = position + EPOCH_OFFSET
            val date = Date(daysSinceEpoch * 1000L * 60 * 60 * 24)
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(date)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startDate = arguments?.getString(ARG_DATE) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDaySummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        // Create adapter for ViewPager
        val adapter = DaySummaryPagerAdapter(this, startDate, binding.viewPager)
        binding.viewPager.adapter = adapter

        // Set initial position using direct date-to-position mapping
        val targetPosition = dateToPosition(startDate)
        binding.viewPager.setCurrentItem(targetPosition, false)
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

    private class DaySummaryPagerAdapter(
        fragment: DialogFragment,
        private val startDate: String,
        private val viewPager: androidx.viewpager2.widget.ViewPager2
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = Int.MAX_VALUE // Infinite for swiping

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            // Use direct position-to-date mapping
            val dateStr = positionToDate(position)
            val fragment = DaySummaryPageFragment.newInstance(dateStr)
            fragment.setOnNavigateToTodayListener {
                // Calculate position for today's actual date using date-to-position mapping
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                val todayPosition = dateToPosition(today)
                viewPager.setCurrentItem(todayPosition, true)
            }
            return fragment
        }
    }
}
