package com.gymbuddy

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object StandardRoutineOffer {
    private const val CHECK_COOLDOWN_MS = 60_000L

    private var lastCheckAtMs = 0L
    private var checkInFlight = false
    private var promptShowing = false
    private var notNowThisProcess = false
    private var restoreInFlight = false
    private var pendingOffer: RoutineCloudClient.Version? = null
    private var dialog: AlertDialog? = null

    fun onActivityStarted(activity: MainActivity) {
        maybeCheck(activity)
    }

    fun onActivityStopped(activity: MainActivity) {
        if (dialog?.ownerActivity == activity && dialog?.isShowing == true) {
            dialog?.dismiss()
        }
        dialog = null
        promptShowing = false
    }

    fun maybeCheck(activity: MainActivity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (!WorkerRemote.isConfigured(activity)) return
        if (notNowThisProcess || promptShowing || checkInFlight || restoreInFlight) return
        if (activity.isWorkoutTabSelected()) return

        val pending = pendingOffer
        if (pending != null) {
            showOffer(activity, pending)
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastCheckAtMs < CHECK_COOLDOWN_MS) return

        checkInFlight = true
        activity.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                RoutineCloudClient.list(activity.applicationContext)
            }
            lastCheckAtMs = System.currentTimeMillis()
            checkInFlight = false
            result.fold(
                onSuccess = { versions ->
                    handleVersions(activity, versions)
                },
                onFailure = { }
            )
        }
    }

    private fun handleVersions(
        activity: MainActivity,
        versions: List<RoutineCloudClient.Version>
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (activity.isWorkoutTabSelected()) return
        when (
            val decision = RoutineSync.decideStandardOffer(
                versions,
                WorkerRemote.standardAppliedUpdatedAt(activity),
                WorkerRemote.standardNeverUpdatedAt(activity)
            )
        ) {
            StandardOfferDecision.Skip -> Unit
            is StandardOfferDecision.Baseline -> {
                WorkerRemote.markStandardAccepted(
                    activity,
                    decision.version.name,
                    decision.version.updatedAt
                )
            }
            is StandardOfferDecision.Offer -> showOffer(activity, decision.version)
        }
    }

    private fun showOffer(activity: MainActivity, version: RoutineCloudClient.Version) {
        if (promptShowing || activity.isFinishing || activity.isDestroyed) return
        pendingOffer = version
        promptShowing = true
        val whenUpdated = RoutineSync.formatUpdated(version.updatedAt)
        val message = if (whenUpdated.isBlank()) {
            activity.getString(R.string.routine_standard_newer_message)
        } else {
            activity.getString(R.string.routine_standard_newer_message_at, whenUpdated)
        }
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.routine_standard_newer_title)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.routine_restore) { _, _ ->
                restore(activity, version)
            }
            .setNegativeButton(R.string.routine_standard_not_now) { _, _ ->
                notNow()
            }
            .setNeutralButton(R.string.routine_standard_never) { _, _ ->
                never(activity, version)
            }
            .setOnCancelListener {
                notNow()
            }
            .setOnDismissListener {
                promptShowing = false
                dialog = null
            }
            .show()
    }

    private fun notNow() {
        notNowThisProcess = true
        pendingOffer = null
    }

    private fun never(activity: MainActivity, version: RoutineCloudClient.Version) {
        WorkerRemote.ignoreStandardUpdatedAt(activity, version.updatedAt)
        pendingOffer = null
    }

    private fun restore(activity: MainActivity, version: RoutineCloudClient.Version) {
        pendingOffer = null
        restoreInFlight = true
        activity.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                RoutineSync.restore(activity.applicationContext, version)
            }
            restoreInFlight = false
            if (activity.isFinishing || activity.isDestroyed) return@launch
            result.fold(
                onSuccess = { record ->
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.routine_restore_ok, record.name),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    pendingOffer = version
                    Toast.makeText(
                        activity,
                        error.message ?: activity.getString(R.string.routine_restore_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
}
