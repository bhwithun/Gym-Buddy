package com.gymbuddy

object FoodData {
    val foodList = listOf(
        FoodItem("cereal", "Cereal with milk", "1 bowl", 12),
        FoodItem("egg", "Hard-boiled egg", "1", 6),
        FoodItem("shake", "Protein shake", "1 scoop", 22),
        FoodItem("milk", "2% milk", "1 cup", 8),
        FoodItem("chicken", "Chicken breast", "palm-sized portion", 35),
        FoodItem("steak", "Steak", "palm-sized portion", 32),
        FoodItem("pork", "Pork", "palm-sized portion", 30),
        FoodItem("yogurt", "Greek yogurt", "1 cup", 18),
        FoodItem("cottage", "Cottage cheese", "½ cup", 14),
        FoodItem("tuna", "Tuna in water", "1 can", 25),
        FoodItem("lentils", "Lentils", "1 cup cooked", 18),
        FoodItem("nuts", "Cashews & pecans", "heaping tablespoon", 7)
    )

    fun getFoodById(id: String): FoodItem? {
        return foodList.find { it.id == id }
    }
}