package com.example.acerestaurant.ui.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.acerestaurant.data.cart.CartItem
import java.text.NumberFormat
import java.util.Locale

/**
 * Shared cart state across the app.
 *
 * Centralizes totals for consistent UI updates.
 * No storage of sensitive data; only item metadata and quantities.
 * Exposes immutable LiveData to the UI.
 */
class CartViewModel : ViewModel() {

    private val _items = MutableLiveData<List<CartItem>>(emptyList())
    val items: LiveData<List<CartItem>> = _items

    /** Simple fixed tax for demo; adjust per locale/business rules. */
    private val taxRate = 0.07

    /** Adds an item; merges with existing line when id/options match. */
    fun add(item: CartItem) {
        val current = _items.value.orEmpty().toMutableList()
        val idx = current.indexOfFirst {
            it.id == item.id &&
                    it.selectedSize == item.selectedSize &&
                    it.selectedCrust == item.selectedCrust &&
                    it.selectedTopping == item.selectedTopping &&
                    (it.notes ?: "") == (item.notes ?: "")
        }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = existing.copy(quantity = existing.quantity + item.quantity)
        } else {
            current.add(item)
        }
        _items.value = current
    }

    /** Removes an item at the given adapter position (no-op if out of range). */
    fun removeAt(position: Int) {
        val current = _items.value.orEmpty().toMutableList()
        if (position in current.indices) {
            current.removeAt(position)
            _items.value = current
        }
    }

    /** Increments quantity of the item at [position]. */
    fun increment(position: Int) {
        val current = _items.value.orEmpty().toMutableList()
        if (position in current.indices) {
            val it = current[position]
            current[position] = it.copy(quantity = it.quantity + 1)
            _items.value = current
        }
    }

    /** Decrements quantity but never below 1. */
    fun decrement(position: Int) {
        val current = _items.value.orEmpty().toMutableList()
        if (position in current.indices) {
            val it = current[position]
            val newQty = (it.quantity - 1).coerceAtLeast(1)
            current[position] = it.copy(quantity = newQty)
            _items.value = current
        }
    }

    /** Clears the cart (used after successful checkout or explicit user action). */
    fun clear() { _items.value = emptyList() }

    /** Pricing helpers used by UI for totals. */
    fun subtotal(): Double = _items.value.orEmpty().sumOf { it.linePrice() }
    fun tax(): Double = subtotal() * taxRate
    fun total(): Double = subtotal() + tax()

    /** Localized currency formatter. */
    fun formatCurrency(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale.getDefault()).format(value)
}