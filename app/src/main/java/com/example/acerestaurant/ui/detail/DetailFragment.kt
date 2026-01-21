package com.example.acerestaurant.ui.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.acerestaurant.R
import com.example.acerestaurant.data.cart.CartItem
import com.example.acerestaurant.data.model.MenuItem
import com.example.acerestaurant.data.repo.MenuRepository
import com.example.acerestaurant.databinding.FragmentDetailBinding
import com.example.acerestaurant.ui.cart.CartViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Displays details for a single menu item and lets the user customize it.
 *
 * HCI: Shows only relevant dropdowns (sizes, crusts, toppings) to reduce clutter.
 * Accessibility: Image has contentDescription using item name.
 * Professional: Uses Safe Args for deterministic navigation inputs.
 */
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val cartVm: CartViewModel by activityViewModels()
    private val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())

    // Safe Args: id of the menu item to display
    private var itemId: String? = null
    private var item: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = DetailFragmentArgs.fromBundle(requireArguments())
        itemId = args.menuItemId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = MenuRepository(requireContext().applicationContext)
        item = itemId?.let { repo.findById(it) }

        val current = item
        if (current != null) {
            // Basic bindings
            binding.name.text = current.name
            binding.description.text = current.description
            binding.price.text = currency.format(current.price)
            binding.image.contentDescription =
                getString(R.string.cd_menu_item_image, current.name)

            // Load image from assets or fallback
            val url = current.image?.let { "file:///android_asset/$it" }
            binding.image.load(url ?: R.drawable.ic_menu_gallery) { crossfade(true) }

            // Conditional dropdowns
            if (current.sizes.isNullOrEmpty()) {
                binding.sizeLayout.isGone = true
            } else {
                binding.sizeLayout.isVisible = true
                binding.sizeDropdown.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        current.sizes!!
                    )
                )
            }

            if (current.crusts.isNullOrEmpty()) {
                binding.crustLayout.isGone = true
            } else {
                binding.crustLayout.isVisible = true
                binding.crustDropdown.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        current.crusts!!
                    )
                )
            }

            if (current.toppings.isNullOrEmpty()) {
                binding.toppingsLayout.isGone = true
            } else {
                binding.toppingsLayout.isVisible = true
                binding.toppingsDropdown.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        current.toppings!!
                    )
                )
            }
        } else {
            // Fallback UI when id not found
            binding.name.text = getString(R.string.app_name)
            binding.description.text = getString(R.string.item_not_found)
            binding.price.text = ""
            binding.sizeLayout.isGone = true
            binding.crustLayout.isGone = true
            binding.toppingsLayout.isGone = true
            binding.image.load(android.R.drawable.ic_menu_gallery)
        }

        // Cancel → back
        binding.btnCancel.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Add to Cart with selected options
        binding.btnAddToCart.setOnClickListener {
            val currentItem = item ?: return@setOnClickListener

            val selectedSize = binding.sizeDropdown.text?.toString().orEmpty().ifBlank { null }
            val selectedCrust = binding.crustDropdown.text?.toString().orEmpty().ifBlank { null }
            val selectedTopping = binding.toppingsDropdown.text?.toString().orEmpty().ifBlank { null }
            val notes = binding.notes.text?.toString().orEmpty().ifBlank { null }

            val assetPath = current?.image

            cartVm.add(
                CartItem(
                    id = currentItem.id,
                    name = currentItem.name,
                    basePrice = currentItem.price,
                    selectedSize = selectedSize,
                    selectedCrust = selectedCrust,
                    selectedTopping = selectedTopping,
                    notes = notes,
                    quantity = 1,
                    imageResName = assetPath
                )
            )

            // Navigate to cart
            findNavController().navigate(R.id.action_global_cartFragment)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

/**
 * Extension helper for DetailFragment to find a MenuItem by ID across all categories.
 */
private fun MenuRepository.findById(id: String): MenuItem? {
    // Search across all categories for the matching menu item ID
    return categories().asSequence()
        .flatMap { itemsFor(it.id).asSequence() }
        .firstOrNull { it.id == id }
}
