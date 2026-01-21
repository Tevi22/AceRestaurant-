package com.example.acerestaurant.data.model

/**
 * Data model for an individual menu item.
 *
 * HCI: Optional lists drive which option dropdowns appear in DetailFragment.
 * Professional: Keep this model pure (no Android deps).
 */
data class MenuItem(
    val id: String,
    val category: String,
    val name: String,
    val price: Double,
    /** Asset-relative path, e.g., "image/pizza_margherita.jpg" */
    val image: String? = null,
    val description: String,
    val sizes: List<String>? = null,   // ["Small", "Medium", "Large"]
    val crusts: List<String>? = null,  // ["Thin", "Hand-tossed", "Deep dish"]
    val toppings: List<String>? = null // ["Pepperoni", "Mushroom", ...]
)