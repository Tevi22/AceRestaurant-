package com.example.acerestaurant.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.acerestaurant.data.model.MenuItem
import com.example.acerestaurant.data.repo.MenuRepository
import kotlin.math.min

/**
 * Filters menu items and generates search suggestions.
 *
 * For each query received from MenuFragment, this ViewModel:
 *  - Filters all items in the current category (or all categories)
 *  - Returns live results to be observed by MenuListFragment
 *  - Computes up to 3 “Did you mean…” names using Levenshtein distance
 */
class MenuListViewModel(
    private val repo: MenuRepository,
    private val categoryId: String
) : ViewModel() {

    data class Results(val items: List<MenuItem>, val suggestions: List<String>)

    private val _query = MutableLiveData("")
    fun setQuery(q: String) { _query.value = q }

    private val allItems = repo.itemsFor(categoryId)

    val results: LiveData<Results> = _query.map { q ->
        val query = q.trim()
        if (query.isBlank()) {
            Results(allItems, emptyList())
        } else {
            val lower = query.lowercase()
            val matches = allItems.filter {
                it.name.lowercase().contains(lower) || it.description.lowercase().contains(lower)
            }

            if (matches.isNotEmpty()) Results(matches, emptyList())
            else {
                val suggestions = repo.itemsFor("all")
                    .asSequence()
                    .map { it.name to levenshtein(it.name.lowercase(), lower) }
                    .sortedBy { it.second }
                    .take(3)
                    .map { it.first }
                    .toList()
                Results(emptyList(), suggestions)
            }
        }
    }

    /** Basic Levenshtein edit distance for string similarity. */
    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = min(
                    min(dp[j] + 1, dp[j - 1] + 1),
                    prev + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                prev = tmp
            }
        }
        return dp[b.length]
    }
}