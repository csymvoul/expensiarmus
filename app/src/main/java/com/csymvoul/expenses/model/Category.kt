package com.csymvoul.expenses.model

/** [RENT] keeps its code so saved data still loads; on screen it is "Home" (until 1.12 it was "Rent"). */
enum class Category(val label: String, val hint: String) {
    RENT("Home", "Rent and shared building costs (κοινόχρηστα)"),
    SUPERMARKET("Super market", "Groceries and household items"),
    ENTERTAINMENT("Entertainment", "Food, cinema, theatre, etc."),
    BILLS("Bills", "Water, electricity, gas, etc."),
    SHOPPING("Shopping", "Shoes, clothes, jewellery, etc."),
    FUEL("Fuel", "Petrol (gas), diesel, LPG"),
    OTHER("Other", "Presents and anything else"),
}

/** Optional subgroup for food expenses, which belong to [Category.ENTERTAINMENT]. */
enum class FoodType(val label: String, val hint: String) {
    DELIVERY("Delivery", "Food ordered in"),
    DINE_OUT("Dine out", "Restaurants, tavernas, bars"),
    DAY_TO_DAY("Day-to-day", "Coffee at work, snacks, quick bites"),
}
