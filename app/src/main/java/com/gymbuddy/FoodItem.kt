package com.gymbuddy

data class FoodItem(
    val id: String,
    val name: String,
    val servingDescription: String,
    val proteinPerPortion: Int // grams per portion
)