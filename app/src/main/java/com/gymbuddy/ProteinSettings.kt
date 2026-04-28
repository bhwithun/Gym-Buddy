package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protein_settings")
data class ProteinSettings(
    @PrimaryKey
    val id: Int = 1, // Single row table
    val bodyWeight: Float = 150f, // in lbs
    val useLbs: Boolean = true, // true for lbs, false for kg
    val fitnessGoal: String = "build", // "maintain" or "build"
    val customGoal: Float? = null // manual override, null means use calculated
) {
    fun getBodyWeightInLbs(): Float {
        return if (useLbs) bodyWeight else bodyWeight * 2.20462f
    }

    fun getRecommendedRate(): Pair<Float, Float> {
        return when (fitnessGoal) {
            "maintain" -> Pair(0.5f, 0.7f) // g per lb
            "build" -> Pair(0.7f, 1.0f) // g per lb
            else -> Pair(0.7f, 1.0f)
        }
    }

    fun getDefaultRate(): Float {
        return when (fitnessGoal) {
            "maintain" -> 0.6f
            "build" -> 0.8f
            else -> 0.8f
        }
    }

    fun getDailyGoal(): Float {
        return customGoal ?: (getBodyWeightInLbs() * getDefaultRate())
    }
}