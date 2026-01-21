package com.example.acerestaurant.data.repo

import android.content.Context
import com.example.acerestaurant.data.model.*
import com.google.gson.Gson
import com.example.acerestaurant.R

/**
 * Loads and serves static menu data from /res/raw/ace_menu_mock_data.json.
 *
 * - Safe for demo (no network, no PII).
 * - Provides category lists, per-category items, "all" aggregation, and text search.
 */
class MenuRepository(private val context: Context) {

    /** Parsed menu JSON (lazy to avoid blocking on app start). */
    private val menu: Menu by lazy { loadJson() }

    /** All categories (id + title) from JSON. */
    fun categories(): List<Category> = menu.categories

    /**
     * Items for a given category.
     * - Pass "all" (any case) to get every item.
     * - Category comparison is case-insensitive.
     */
    fun itemsFor(categoryId: String): List<MenuItem> =
        if (categoryId.equals("all", ignoreCase = true)) {
            menu.items
        } else {
            menu.items.filter { it.category.equals(categoryId, ignoreCase = true) }
        }

    /**
     * Case-insensitive search of item name/description within a category.
     * Use categoryId = "all" to search across the entire menu.
     */
    fun search(categoryId: String, q: String): List<MenuItem> {
        val base = itemsFor(categoryId)
        if (q.isBlank()) return base
        val needle = q.trim().lowercase()
        return base.filter {
            it.name.lowercase().contains(needle) || it.description.lowercase().contains(needle)
        }
    }

    /** Find an item by id (used on the detail screen). */
    fun findById(id: String): MenuItem? = menu.items.firstOrNull { it.id == id }

    /** Map item.image (e.g., "pizza_margherita") to a drawable resource id if present. */
    fun imageResIdFor(item: MenuItem): Int? {
        val name = item.image ?: return null
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else null
    }

    /** Load and parse JSON from res/raw. */
    private fun loadJson(): Menu {
        return try {
            context.resources.openRawResource(R.raw.ace_menu_mock_data)
                .bufferedReader()
                .use { Gson().fromJson(it.readText(), Menu::class.java) }
        } catch (_: Exception) {
            // Fail-safe: empty menu if the file is missing or malformed.
            Menu(emptyList(), emptyList())
        }
    }
}
