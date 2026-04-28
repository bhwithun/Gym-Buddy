package com.gymbuddy

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "daily_protein_logs")
data class DailyProteinLog(
    @PrimaryKey
    val date: String, // YYYY-MM-DD format
    val foodIntake: Map<String, Int> = emptyMap() // foodId -> portions eaten
) {
    fun getTotalProtein(foodList: List<FoodItem>): Int {
        return foodIntake.entries.sumOf { (foodId, portions) ->
            val food = foodList.find { it.id == foodId }
            food?.let { it.proteinPerPortion * portions } ?: 0
        }
    }

    fun getPortionsForFood(foodId: String): Int {
        return foodIntake[foodId] ?: 0
    }

    fun updatePortions(foodId: String, portions: Int): DailyProteinLog {
        val newIntake = foodIntake.toMutableMap()
        if (portions > 0) {
            newIntake[foodId] = portions
        } else {
            newIntake.remove(foodId)
        }
        return copy(foodIntake = newIntake)
    }
}