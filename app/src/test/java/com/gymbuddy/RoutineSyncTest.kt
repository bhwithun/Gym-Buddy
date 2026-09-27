package com.gymbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineSyncTest {

    @Test
    fun skipWhenRoutineMissing() {
        val decision = RoutineSync.decideStandardOffer(
            updatedAt = null,
            appliedUpdatedAt = "",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun baselineWhenNeverApplied() {
        val updatedAt = "2026-09-19T12:00:00.000Z"
        val decision = RoutineSync.decideStandardOffer(
            updatedAt,
            appliedUpdatedAt = "",
            neverUpdatedAt = ""
        )
        assertTrue(decision is StandardOfferDecision.Baseline)
    }

    @Test
    fun offerWhenServerNewer() {
        val decision = RoutineSync.decideStandardOffer(
            updatedAt = "2026-09-19T13:00:00.000Z",
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = ""
        )
        assertTrue(decision is StandardOfferDecision.Offer)
    }

    @Test
    fun skipWhenSameTimestamp() {
        val decision = RoutineSync.decideStandardOffer(
            updatedAt = "2026-09-19T12:00:00.000Z",
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = ""
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun skipWhenNeverForThisTimestamp() {
        val newer = "2026-09-19T13:00:00.000Z"
        val decision = RoutineSync.decideStandardOffer(
            updatedAt = newer,
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = newer
        )
        assertEquals(StandardOfferDecision.Skip, decision)
    }

    @Test
    fun offerAgainAfterNeverWhenTimestampChanges() {
        val decision = RoutineSync.decideStandardOffer(
            updatedAt = "2026-09-19T14:00:00.000Z",
            appliedUpdatedAt = "2026-09-19T12:00:00.000Z",
            neverUpdatedAt = "2026-09-19T13:00:00.000Z"
        )
        assertTrue(decision is StandardOfferDecision.Offer)
    }
}
