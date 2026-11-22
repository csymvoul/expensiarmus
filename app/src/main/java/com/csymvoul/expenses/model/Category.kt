package com.csymvoul.expenses.model

enum class Category(val label: String, val hint: String) {
    RENT("Rent", "Monthly rent"),
    SUPERMARKET("Super market", "Groceries and household items"),
    ENTERTAINMENT("Entertainment", "Food, cinema, theatre, etc."),
    BILLS("Bills", "Water, electricity, gas, etc."),
    OTHER("Other", "Presents and anything else"),
}

/** Optional subgroup for food expenses, which belong to [Category.ENTERTAINMENT]. */
enum class FoodType(val label: String, val hint: String) {
    DELIVERY("Delivery", "Food ordered in"),
    DINE_OUT("Dine out", "Restaurants, tavernas, bars"),
    DAY_TO_DAY("Day-to-day", "Coffee at work, snacks, quick bites"),
}
