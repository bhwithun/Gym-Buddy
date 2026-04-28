package com.gymbuddy

object FoodData {
    val foodList = listOf(
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

    fun getFoodById(id: String): FoodItem? {
        return foodList.find { it.id == id }
    }
}