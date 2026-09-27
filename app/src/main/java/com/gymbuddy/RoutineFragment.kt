package com.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gymbuddy.databinding.FragmentRoutineBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RoutineFragment : Fragment() {

    private var _binding: FragmentRoutineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dao = AppDatabase.getDatabase(requireContext()).routineDao()
        dao.getAllLive().observe(viewLifecycleOwner) { days ->
            val adapter = DayAdapter(days) { day ->
                val fragment = DayDetailDialogFragment.newInstance(day)
                fragment.show(parentFragmentManager, "day_detail")
            }
            binding.recyclerView.adapter = adapter
        }

        refreshEditorLink()

        binding.backupButton.setOnClickListener { confirmBackup(dao) }
        binding.restoreButton.setOnClickListener { confirmRestore() }
        binding.editOnPcLink.setOnClickListener {
            if (!WorkerRemote.openRoutineEditor(requireContext())) {
                toast(R.string.routine_worker_missing)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshEditorLink()
    }

    private fun refreshEditorLink() {
        binding.editOnPcLink.visibility =
            if (WorkerRemote.isConfigured(requireContext())) View.VISIBLE else View.GONE
    }

    private fun confirmBackup(dao: RoutineDao) {
        if (!WorkerRemote.isConfigured(requireContext())) {
            toast(R.string.routine_worker_missing)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.routine_backup_title)
            .setMessage(R.string.routine_backup_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.routine_backup) { _, _ ->
                backup(dao)
            }
            .show()
    }

    private fun backup(dao: RoutineDao) {
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val days = RoutineExport.fromEntities(dao.getAll())
                RoutineCloudClient.save(requireContext(), days)
            }
            setBusy(false)
            result.fold(
                onSuccess = { record ->
                    WorkerRemote.markStandardAccepted(
                        requireContext(),
                        record.name,
                        record.updatedAt
                    )
                    Toast.makeText(
                        requireContext(),
                        R.string.routine_backup_ok,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.routine_backup_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun confirmRestore() {
        if (!WorkerRemote.isConfigured(requireContext())) {
            toast(R.string.routine_worker_missing)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.routine_restore_title)
            .setMessage(R.string.routine_restore_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.routine_restore) { _, _ ->
                restore()
            }
            .show()
    }

    private fun restore() {
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                RoutineSync.restore(requireContext())
            }
            setBusy(false)
            result.fold(
                onSuccess = {
                    Toast.makeText(
                        requireContext(),
                        R.string.routine_restore_ok,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: getString(R.string.routine_restore_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.backupButton.isEnabled = !busy
        binding.restoreButton.isEnabled = !busy
    }

    private fun toast(message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
