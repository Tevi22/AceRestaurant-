package com.example.acerestaurant.ui.menu

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.acerestaurant.data.repo.MenuRepository
import com.example.acerestaurant.databinding.FragmentMenuBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Menu screen:
 * - Dynamic categories in ViewPager2 + TabLayout
 * - Debounced, case-insensitive search
 * - Always shows search results in the "All" tab
 * - Robust re-broadcasts so newly-visible pages definitely receive the query
 */
class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    /** (categoryId -> title) for tabs. */
    private lateinit var categories: List<Pair<String, String>>

    /** Index of "All" tab (0 when enabled). */
    private var allTabIndex: Int = -1

    /** Last search query (persists across config changes). */
    private var lastQuery: String = ""

    /** Debounce job for typing. */
    private var debounceJob: Job? = null

    /** Always include an "All" tab so searches show there. */
    private val includeAllTab = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        lastQuery = savedInstanceState?.getString(STATE_QUERY).orEmpty()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) Load categories from repo (ids match JSON)
        val repo = MenuRepository(requireContext().applicationContext)
        val loaded = repo.categories().map { it.id to it.title }
        categories = if (includeAllTab && loaded.isNotEmpty()) {
            listOf("all" to "All") + loaded
        } else {
            loaded
        }
        allTabIndex = if (includeAllTab) 0 else -1

        // 2) Setup pager + tabs
        val pagerAdapter = CategoryPagerAdapter(this, categories)
        binding.pager.adapter = pagerAdapter
        binding.pager.offscreenPageLimit = categories.size

        TabLayoutMediator(binding.tabs, binding.pager) { tab, pos ->
            tab.text = categories[pos].second
        }.attach()

        // 3) Re-broadcast the current query whenever the visible page changes
        binding.pager.registerOnPageChangeCallback(
            object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Tiny delay ensures the fragment attaches its listener
                        delay(30)
                        broadcastQuery(lastQuery)
                    }
                }
            }
        )

        // 4) Search field (debounce + IME)
        setupSearch()

        // 5) Initial sync (after tabs/pager attached)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(100)
            // If restoring with a non-blank query, ensure All tab is visible
            if (lastQuery.isNotBlank() && allTabIndex >= 0) {
                binding.pager.setCurrentItem(allTabIndex, false)
                delay(30)
            }
            broadcastQuery(lastQuery)
        }
    }

    /**
     * Configure the TextInputEditText with debounce + IME handling.
     * Any non-blank query auto-switches to the "All" tab, then re-broadcasts.
     */
    private fun setupSearch() = with(binding.search) {
        if (lastQuery.isNotBlank()) {
            setText(lastQuery)
            setSelection(lastQuery.length)
        }

        addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                val q = text?.toString()?.trim().orEmpty()
                if (q == lastQuery) return@addTextChangedListener
                debounceJob?.cancel()
                debounceJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300) // debounce
                    lastQuery = q
                    if (q.isNotBlank() && allTabIndex >= 0 && binding.pager.currentItem != allTabIndex) {
                        binding.pager.setCurrentItem(allTabIndex, true)
                        delay(30) // let the All page attach its listener
                    }
                    broadcastQuery(q)
                }
            }
        )

        setOnEditorActionListener { v, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (actionId == EditorInfo.IME_NULL && event?.keyCode == KeyEvent.KEYCODE_ENTER)
            if (isSearchAction) {
                val q = text?.toString()?.trim().orEmpty()
                lastQuery = q
                viewLifecycleOwner.lifecycleScope.launch {
                    if (q.isNotBlank() && allTabIndex >= 0 && binding.pager.currentItem != allTabIndex) {
                        binding.pager.setCurrentItem(allTabIndex, true)
                        delay(30)
                    }
                    broadcastQuery(q)
                }
                v.clearFocus()
                true
            } else {
                false
            }
        }
    }

    /**
     * Broadcast query to child pages (must use childFragmentManager so children receive it).
     */
    private fun broadcastQuery(q: String) {
        childFragmentManager.setFragmentResult(
            MenuListFragment.REQUEST_MENU_QUERY,
            Bundle().apply { putString(MenuListFragment.KEY_QUERY, q) }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_QUERY, binding.search.text?.toString().orEmpty())
    }

    override fun onDestroyView() {
        debounceJob?.cancel()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val STATE_QUERY = "state_query"
    }
}