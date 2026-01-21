package com.example.acerestaurant.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.acerestaurant.databinding.FragmentCartBinding
import androidx.navigation.fragment.findNavController
import com.example.acerestaurant.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Displays the user's cart and order totals.
 *
 * Shows empty state vs. content state clearly; announces total changes for a11y.
 * Confirms destructive actions; avoids submitting when cart is empty.
 * Uses shared [CartViewModel] scoped to activity.
 */
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    // Shared across fragments (lives in the Activity scope)
    private val vm: CartViewModel by activityViewModels()

    private lateinit var adapter: CartAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = CartAdapter(
            data = emptyList(),
            onMinus = { vm.decrement(it) },
            onPlus = { vm.increment(it) },
            onRemove = { vm.removeAt(it) }
        )

        binding.cartList.layoutManager = LinearLayoutManager(requireContext())
        binding.cartList.adapter = adapter

        vm.items.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)

            val isEmpty = list.isNullOrEmpty()
            binding.emptyGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.contentGroup.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (!isEmpty) {
                val sub = vm.formatCurrency(vm.subtotal())
                val tax = vm.formatCurrency(vm.tax())
                val tot = vm.formatCurrency(vm.total())

                binding.subtotal.text = "Subtotal: $sub"
                binding.tax.text = "Tax: $tax"
                binding.total.text = "Total: $tot"

                // A11y: Announce totals & cart size change
                val msg = getString(R.string.cd_checkout_totals, sub, tax, tot)
                binding.total.announceForAccessibility(msg)
                val countMsg = getString(R.string.a11y_cart_updated, list.size, tot)
                binding.root.announceForAccessibility(countMsg)
            }
        }

        binding.btnBrowseMenu.setOnClickListener {
            // Return to menu
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Ethical UX: confirm clear cart
        binding.btnClear.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear cart?")
                .setMessage("This will remove all items from your cart.")
                .setPositiveButton("Clear") { _, _ -> vm.clear() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnCheckout.setOnClickListener {
            val action = CartFragmentDirections.actionCartToCheckout()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}