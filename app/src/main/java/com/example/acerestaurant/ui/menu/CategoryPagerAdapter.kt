package com.example.acerestaurant.ui.menu

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Pager adapter that creates a [MenuListFragment] per menu category.
 *
 * Keeps category navigation consistent via ViewPager2 tabs.
 * Narrow responsibility (creates fragments only).
 *
 * @param fragment host fragment (used for lifecycle + childManager)
 * @param categories list of Pair<categoryId, tabTitle>
 */
class CategoryPagerAdapter(
    fragment: Fragment,
    private val categories: List<Pair<String, String>>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = categories.size

    override fun createFragment(position: Int): Fragment {
        val categoryId = categories[position].first
        return MenuListFragment.newInstance(categoryId)
    }
}
