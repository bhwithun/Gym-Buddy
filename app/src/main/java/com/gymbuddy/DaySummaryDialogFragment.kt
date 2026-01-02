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

        fun newInstance(date: String): DaySummaryDialogFragment {
            val fragment = DaySummaryDialogFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
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
        val adapter = DaySummaryPagerAdapter(this, startDate)
        binding.viewPager.adapter = adapter

        // Set initial position
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startCalendar = Calendar.getInstance()
        startCalendar.time = dateFormat.parse(startDate) ?: Date()
        val position = (startCalendar.get(Calendar.YEAR) - calendar.get(Calendar.YEAR)) * 365 + startCalendar.get(Calendar.DAY_OF_YEAR) - calendar.get(Calendar.DAY_OF_YEAR)
        binding.viewPager.setCurrentItem(position + 1000, false) // Offset to allow swiping back
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
        private val startDate: String
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = Int.MAX_VALUE // Infinite for swiping

        override fun createFragment(position: Int): androidx.fragment.app.Fragment {
            val offset = position - 1000 // Adjust for offset
            val calendar = Calendar.getInstance()
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate) ?: Date()
            calendar.add(Calendar.DAY_OF_MONTH, offset)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            return DaySummaryPageFragment.newInstance(dateStr)
        }
    }
}
