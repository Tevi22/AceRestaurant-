package com.example.acerestaurant.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acerestaurant.R
import com.example.acerestaurant.data.repo.MenuRepository
import com.example.acerestaurant.databinding.FragmentMenuListBinding

/**
 * Displays a list of menu items for a specific category.
 *
 * Each instance corresponds to a tab (Appetizers, Entrees, Desserts).
 * It observes a shared query broadcast from MenuFragment and updates
 * the RecyclerView accordingly, showing “Did you mean” suggestions
 * or “No items match” when appropriate.
 */
class MenuListFragment : Fragment() {

    private var _binding: FragmentMenuListBinding? = null
    private val binding get() = _binding!!

    private lateinit var categoryId: String
    private lateinit var adapter: MenuAdapter

    private val vm: MenuListViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = MenuRepository(requireContext().applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MenuListViewModel(repo, categoryId) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        categoryId = requireArguments().getString(ARG_CATEGORY_ID) ?: "all"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = MenuAdapter(emptyList()) { item ->
            val action = MenuFragmentDirections.actionMenuToDetail(item.id)
            findNavController().navigate(action)
        }
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        vm.results.observe(viewLifecycleOwner) { res ->
            if (res.items.isNotEmpty()) {
                adapter.submit(res.items)
                binding.list.visibility = View.VISIBLE
                binding.empty.visibility = View.GONE
            } else {
                binding.list.visibility = View.GONE
                binding.empty.visibility = View.VISIBLE

                val base = getString(R.string.no_items_match)
                binding.empty.text = if (res.suggestions.isEmpty()) {
                    base
                } else {
                    val hint = res.suggestions.joinToString(", ")
                    "$base\n${getString(R.string.did_you_mean)} $hint?"
                }
            }
        }

        // Listen for broadcasted search queries from parent
        parentFragmentManager.setFragmentResultListener(
            REQUEST_MENU_QUERY, viewLifecycleOwner
        ) { _, bundle ->
            val query = bundle.getString(KEY_QUERY).orEmpty()
            vm.setQuery(query)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_CATEGORY_ID = "categoryId"
        const val REQUEST_MENU_QUERY = "menu_query_request"
        const val KEY_QUERY = "query"

        fun newInstance(categoryId: String) = MenuListFragment().apply {
            arguments = Bundle().apply { putString(ARG_CATEGORY_ID, categoryId) }
        }
    }
}