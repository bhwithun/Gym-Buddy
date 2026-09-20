package com.gymbuddy

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

        binding.backupButton.setOnClickListener { promptBackup(dao) }
        binding.restoreButton.setOnClickListener { promptRestore() }
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

    private fun promptBackup(dao: RoutineDao) {
        if (!WorkerRemote.isConfigured(requireContext())) {
            toast(R.string.routine_worker_missing)
            return
        }
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            hint = getString(R.string.routine_backup_name_hint)
            setText(WorkerRemote.lastRoutineName(requireContext()).ifBlank { "My routine" })
            setPadding(48, 32, 48, 16)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.routine_backup_title)
            .setMessage(R.string.routine_backup_message)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.routine_backup) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    toast(R.string.routine_name_required)
                } else {
                    backup(dao, name)
                }
            }
            .show()
    }

    private fun backup(dao: RoutineDao, name: String) {
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val days = RoutineExport.fromEntities(dao.getAll())
                RoutineCloudClient.save(requireContext(), name, days)
            }
            setBusy(false)
            result.fold(
                onSuccess = { record ->
                    WorkerRemote.saveLastRoutineName(requireContext(), record.name)
                    WorkerRemote.markStandardAccepted(
                        requireContext(),
                        record.name,
                        record.updatedAt
                    )
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.routine_backup_ok, record.name),
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

    private fun promptRestore() {
        if (!WorkerRemote.isConfigured(requireContext())) {
            toast(R.string.routine_worker_missing)
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { RoutineCloudClient.list(requireContext()) }
            setBusy(false)
            result.fold(
                onSuccess = { versions ->
                    if (versions.isEmpty()) {
                        toast(R.string.routine_restore_empty)
                        return@fold
                    }
                    val labels = versions.map { version ->
                        val whenUpdated = RoutineSync.formatUpdated(version.updatedAt)
                        if (whenUpdated.isBlank()) version.name else "${version.name}\n$whenUpdated"
                    }.toTypedArray()
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.routine_restore_title)
                        .setItems(labels) { _, which ->
                            restore(versions[which])
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
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

    private fun restore(version: RoutineCloudClient.Version) {
        setBusy(true)
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                RoutineSync.restore(requireContext(), version)
            }
            setBusy(false)
            result.fold(
                onSuccess = { record ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.routine_restore_ok, record.name),
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
