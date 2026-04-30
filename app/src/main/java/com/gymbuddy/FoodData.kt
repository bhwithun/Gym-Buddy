package com.gymbuddy

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FoodData {
    private val defaultFoodList = listOf(
        FoodItem("cereal", "Cereal with milk", "1 bowl", 13, 4),
        FoodItem("egg", "Whole egg", "hard boiled", 6),
        FoodItem("shake", "Protein shake", "with milk", 45, 3),
        FoodItem("milk", "Glass of milk", "10oz", 10, 3),
        FoodItem("chicken", "Chicken breast", "10oz", 35, 3),
        FoodItem("steak", "Steak", "10oz", 32, 3),
        FoodItem("pork", "Pork", "10oz", 30, 3),
        FoodItem("tilapia", "Tilapia", "10oz", 50, 3),
        FoodItem("salmon", "Salmon", "8oz", 45, 3),
        FoodItem("shrimp", "Shrimp", "8oz", 50, 3),
        FoodItem("yogurt", "Greek yogurt", "1 cup", 18),
        FoodItem("cottage", "Cottage cheese", "½ cup", 14),
        FoodItem("tuna", "Tuna", "1 can", 25),
        FoodItem("bacon", "Bacon", "1 strip", 4, 10),
        FoodItem("nuts", "Salted nuts", "handful", 7)
    )

    fun getFoodList(context: Context): List<FoodItem> {
        val prefs = context.getSharedPreferences("gym_buddy_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("food_list", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<FoodItem>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                defaultFoodList
            }
        } else {
            defaultFoodList
        }
    }

    fun saveFoodList(context: Context, foodList: List<FoodItem>) {
        val prefs = context.getSharedPreferences("gym_buddy_prefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(foodList)
        prefs.edit().putString("food_list", json).apply()
    }

    fun getFoodById(context: Context, id: String): FoodItem? {
        return getFoodList(context).find { it.id == id }
    }
}
