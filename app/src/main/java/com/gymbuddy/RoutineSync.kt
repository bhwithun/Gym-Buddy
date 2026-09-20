package com.gymbuddy

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class StandardOfferDecision {
    object Skip : StandardOfferDecision()
    data class Baseline(val version: RoutineCloudClient.Version) : StandardOfferDecision()
    data class Offer(val version: RoutineCloudClient.Version) : StandardOfferDecision()
}

object RoutineSync {
    const val STANDARD_NAME = "Standard"

    fun isStandardName(name: String): Boolean =
        name.equals(STANDARD_NAME, ignoreCase = true)

    fun findStandard(versions: List<RoutineCloudClient.Version>): RoutineCloudClient.Version? =
        versions.firstOrNull { isStandardName(it.name) }

    fun decideStandardOffer(
        versions: List<RoutineCloudClient.Version>,
        appliedUpdatedAt: String,
        neverUpdatedAt: String
    ): StandardOfferDecision {
        val standard = findStandard(versions) ?: return StandardOfferDecision.Skip
        if (standard.updatedAt.isBlank()) return StandardOfferDecision.Skip
        if (appliedUpdatedAt.isBlank()) return StandardOfferDecision.Baseline(standard)
        if (standard.updatedAt <= appliedUpdatedAt) return StandardOfferDecision.Skip
        if (standard.updatedAt == neverUpdatedAt) return StandardOfferDecision.Skip
        return StandardOfferDecision.Offer(standard)
    }

    suspend fun restore(
        context: Context,
        version: RoutineCloudClient.Version
    ): Result<RoutineCloudClient.Record> {
        val fetched = RoutineCloudClient.get(context, version.id)
        val record = fetched.getOrElse { return Result.failure(it) }
        return try {
            applyRecord(context, record)
            Result.success(record)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun applyRecord(context: Context, record: RoutineCloudClient.Record) {
        val entities = RoutineExport.toEntities(record.days)
            ?: error("Invalid routine: must contain exactly 7 days")
        val db = AppDatabase.getDatabase(context)
        val routineDao = db.routineDao()
        routineDao.deleteAll()
        routineDao.insertAll(*entities.toTypedArray())
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        db.workoutLogDao().deleteByDate(dateStr)
        WorkerRemote.saveLastRoutineName(context, record.name)
        WorkerRemote.markStandardAccepted(context, record.name, record.updatedAt)
    }

    fun formatUpdated(iso: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(iso)
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(iso)
            if (parsed == null) iso.replace('T', ' ').take(16)
            else SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            iso.replace('T', ' ').take(16)
        }
    }
}
