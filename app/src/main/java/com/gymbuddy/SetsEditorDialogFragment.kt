package com.gymbuddy

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.gymbuddy.databinding.DialogSetsEditorBinding

class SetsEditorDialogFragment : DialogFragment() {

    private var _binding: DialogSetsEditorBinding? = null
    private val binding get() = _binding!!

    private var currentSets: Int = 0
    private lateinit var listener: SetsEditorListener

    interface SetsEditorListener {
        fun onSetsUpdated(newSets: Int)
    }

    companion object {
        private const val ARG_SETS = "sets"

        fun newInstance(currentSets: Int): SetsEditorDialogFragment {
            val fragment = SetsEditorDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SETS, currentSets)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentSets = it.getInt(ARG_SETS)
        }
        listener = targetFragment as? SetsEditorListener ?: parentFragment as? SetsEditorListener ?: requireActivity() as SetsEditorListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSetsEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate field
        binding.setsEdit.setText(currentSets.toString())

        // Minus button
        binding.setsMinus.setOnClickListener {
            val current = binding.setsEdit.text.toString().toIntOrNull() ?: currentSets
            if (current > 1) {
                binding.setsEdit.setText((current - 1).toString())
            }
        }

        // Plus button
        binding.setsPlus.setOnClickListener {
            val current = binding.setsEdit.text.toString().toIntOrNull() ?: currentSets
            binding.setsEdit.setText((current + 1).toString())
        }

        // Cancel
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        // OK
        binding.okButton.setOnClickListener {
            val newSets = binding.setsEdit.text.toString().toIntOrNull()
            if (newSets != null && newSets > 0) {
                listener.onSetsUpdated(newSets)
                dismiss()
            } else {
                // Invalid, maybe show error
                binding.setsEdit.error = "Invalid number"
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