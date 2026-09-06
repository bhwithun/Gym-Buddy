package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogDurationEditorBinding

class DurationEditorDialogFragment : DialogFragment() {

    private var _binding: DialogDurationEditorBinding? = null
    private val binding get() = _binding!!

    private var currentMs: Long = 0L
    private lateinit var listener: DurationEditorListener

    interface DurationEditorListener {
        fun onDurationUpdated(durationMs: Long)
    }

    companion object {
        private const val ARG_MS = "duration_ms"
        private const val MAX_HOURS = 23

        fun newInstance(durationMs: Long): DurationEditorDialogFragment {
            val fragment = DurationEditorDialogFragment()
            fragment.arguments = Bundle().apply { putLong(ARG_MS, durationMs) }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentMs = arguments?.getLong(ARG_MS) ?: 0L
        listener = requireActivity() as DurationEditorListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDurationEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val parts = WorkoutClock.durationParts(currentMs)
        binding.hoursEdit.setText(parts.first.toString())
        binding.minutesEdit.setText(parts.second.toString())
        binding.secondsEdit.setText(parts.third.toString())

        bindStepper(binding.hoursMinus, binding.hoursEdit, -1, 0, MAX_HOURS)
        bindStepper(binding.hoursPlus, binding.hoursEdit, 1, 0, MAX_HOURS)
        bindStepper(binding.minutesMinus, binding.minutesEdit, -1, 0, 59)
        bindStepper(binding.minutesPlus, binding.minutesEdit, 1, 0, 59)
        bindStepper(binding.secondsMinus, binding.secondsEdit, -1, 0, 59)
        bindStepper(binding.secondsPlus, binding.secondsEdit, 1, 0, 59)

        binding.cancelButton.setOnClickListener { dismiss() }
        binding.okButton.setOnClickListener {
            val hours = readField(binding.hoursEdit, 0, MAX_HOURS)
            val minutes = readField(binding.minutesEdit, 0, 59)
            val seconds = readField(binding.secondsEdit, 0, 59)
            if (hours == null) {
                binding.hoursEdit.error = getString(R.string.summary_edit_invalid)
                return@setOnClickListener
            }
            if (minutes == null) {
                binding.minutesEdit.error = getString(R.string.summary_edit_invalid)
                return@setOnClickListener
            }
            if (seconds == null) {
                binding.secondsEdit.error = getString(R.string.summary_edit_invalid)
                return@setOnClickListener
            }
            listener.onDurationUpdated(WorkoutClock.durationFromParts(hours, minutes, seconds))
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindStepper(button: View, field: EditText, delta: Int, min: Int, max: Int) {
        button.setOnClickListener {
            val current = field.text.toString().toIntOrNull() ?: 0
            field.setText((current + delta).coerceIn(min, max).toString())
            field.error = null
        }
    }

    private fun readField(field: EditText, min: Int, max: Int): Int? {
        val value = field.text.toString().toIntOrNull() ?: return null
        if (value !in min..max) return null
        return value
    }
}
