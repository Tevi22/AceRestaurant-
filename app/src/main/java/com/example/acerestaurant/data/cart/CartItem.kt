package com.example.acerestaurant.data.cart

/**
 * Line item stored in the in-memory cart.
 *
 * stored menu selections.
 */
data class CartItem(
    val id: String,            // menu item id
    val name: String,
    val basePrice: Double,
    val selectedSize: String? = null,
    val selectedCrust: String? = null,
    val selectedTopping: String? = null, // simple single-select for now
    val notes: String? = null,
    var quantity: Int = 1,
    /** Use asset-relative path like "image/pizza_margherita.jpg". */
    val imageResName: String? = null,
    val imageUrl: String? = null
) {
    /** Total price for this line = base * qty. */
    fun linePrice(): Double = basePrice * quantity
}