package com.gymbuddy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogRepsEditorBinding

class RepsEditorDialogFragment : DialogFragment() {

    private var _binding: DialogRepsEditorBinding? = null
    private val binding get() = _binding!!

    private var currentReps: Int = 0
    private lateinit var listener: RepsEditorListener

    interface RepsEditorListener {
        fun onRepsUpdated(newReps: Int)
    }

    companion object {
        private const val ARG_REPS = "reps"

        fun newInstance(currentReps: Int): RepsEditorDialogFragment {
            val fragment = RepsEditorDialogFragment()
            val args = Bundle()
            args.putInt(ARG_REPS, currentReps)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentReps = it.getInt(ARG_REPS)
        }
        listener = targetFragment as? RepsEditorListener ?: parentFragment as? RepsEditorListener ?: requireActivity() as RepsEditorListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRepsEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate field
        binding.repsEdit.setText(currentReps.toString())

        // Minus button
        binding.repsMinus.setOnClickListener {
            val current = binding.repsEdit.text.toString().toIntOrNull() ?: currentReps
            if (current > 1) {
                binding.repsEdit.setText((current - 1).toString())
            }
        }

        // Plus button
        binding.repsPlus.setOnClickListener {
            val current = binding.repsEdit.text.toString().toIntOrNull() ?: currentReps
            binding.repsEdit.setText((current + 1).toString())
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // OK
        binding.okButton.setOnClickListener {
            val newReps = binding.repsEdit.text.toString().toIntOrNull()
            if (newReps != null && newReps > 0) {
                listener.onRepsUpdated(newReps)
                dismiss()
            } else {
                // Invalid, maybe show error
                binding.repsEdit.error = "Invalid number"
            }
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
}