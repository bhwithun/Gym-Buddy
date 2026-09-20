package com.gymbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineSyncTest {

    private fun version(name: String, updatedAt: String, id: String = "id") =
        RoutineCloudClient.Version(id = id, name = name, updatedAt = updatedAt)

    @Test
    fun skipWhenStandardMissing() {
        val decision = RoutineSync.decideStandardOffer(
            listOf(version("Other", "2026-09-19T12:00:00.000Z")),
            appliedUpdatedAt = "",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun baselineWhenNeverApplied() {
        val standard = version("Standard", "2026-09-19T12:00:00.000Z")
        val decision = RoutineSync.decideStandardOffer(
            listOf(standard),
            appliedUpdatedAt = "",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Baseline(standard), decision)
    }

    @Test
    fun offerWhenServerNewer() {
        val standard = version("standard", "2026-09-19T13:00:00.000Z")
        val decision = RoutineSync.decideStandardOffer(
            listOf(standard),
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Offer(standard), decision)
    }

    @Test
    fun skipWhenSameTimestamp() {
        val decision = RoutineSync.decideStandardOffer(
            listOf(version("Standard", "2026-09-19T12:00:00.000Z")),
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun skipWhenNeverForThisTimestamp() {
        val newer = "2026-09-19T13:00:00.000Z"
        val decision = RoutineSync.decideStandardOffer(
            listOf(version("Standard", newer)),
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = newer
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun offerAgainAfterNeverWhenTimestampChanges() {
        val newest = "2026-09-19T14:00:00.000Z"
        val standard = version("Standard", newest)
        val decision = RoutineSync.decideStandardOffer(
            listOf(standard),
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = "2026-09-19T13:00:00.000Z"
        )
        assertEquals(StandardOfferDecision.Offer(standard), decision)
    }

    @Test
    fun isStandardNameIsCaseInsensitive() {
        assertTrue(RoutineSync.isStandardName("Standard"))
        assertTrue(RoutineSync.isStandardName("standard"))
        assertTrue(RoutineSync.isStandardName("STANDARD"))
    }
}
