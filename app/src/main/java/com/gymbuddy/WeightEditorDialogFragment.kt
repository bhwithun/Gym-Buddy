package com.gymbuddy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogWeightEditorBinding

class WeightEditorDialogFragment : DialogFragment() {

    private var _binding: DialogWeightEditorBinding? = null
    private val binding get() = _binding!!

    private var currentWeight: Int = 0
    private lateinit var listener: WeightEditorListener

    interface WeightEditorListener {
        fun onWeightUpdated(newWeight: Int)
    }

    companion object {
        private const val ARG_WEIGHT = "weight"

        fun newInstance(currentWeight: Int): WeightEditorDialogFragment {
            val fragment = WeightEditorDialogFragment()
            val args = Bundle()
            args.putInt(ARG_WEIGHT, currentWeight)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentWeight = it.getInt(ARG_WEIGHT)
        }
        listener = targetFragment as? WeightEditorListener ?: parentFragment as? WeightEditorListener ?: requireActivity() as WeightEditorListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogWeightEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate field
        binding.weightEdit.setText(currentWeight.toString())

        // Minus button
        binding.weightMinus.setOnClickListener {
            val current = binding.weightEdit.text.toString().toIntOrNull() ?: currentWeight
            if (current > 0) {
                binding.weightEdit.setText((current - 1).toString())
            }
        }

        // Plus button
        binding.weightPlus.setOnClickListener {
            val current = binding.weightEdit.text.toString().toIntOrNull() ?: currentWeight
            binding.weightEdit.setText((current + 1).toString())
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // OK
        binding.okButton.setOnClickListener {
            val newWeight = binding.weightEdit.text.toString().toIntOrNull()
            if (newWeight != null && newWeight >= 0) {
                listener.onWeightUpdated(newWeight)
                dismiss()
            } else {
                // Invalid, maybe show error
                binding.weightEdit.error = "Invalid number"
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