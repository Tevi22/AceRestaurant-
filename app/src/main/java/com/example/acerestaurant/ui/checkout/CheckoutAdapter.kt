package com.example.acerestaurant.ui.checkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.acerestaurant.R
import com.example.acerestaurant.data.cart.CartItem
import com.example.acerestaurant.databinding.ItemCheckoutLineBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter for the Checkout screen's order-summary list.
 *
 * HCI: Mirrors cart row layout to reduce cognitive load.
 * Accessibility: Adds content descriptions to item actions.
 * Professional: Keeps formatting in one place for consistency.
 */
class CheckoutAdapter(
    private var data: List<CartItem>,
    private val onPlus: (Int) -> Unit,
    private val onMinus: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<CheckoutAdapter.VH>() {

    private val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())

    /** ViewHolder that exposes view binding. */
    class VH(val binding: ItemCheckoutLineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCheckoutLineBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        val b = holder.binding

        // Title & options
        b.title.text = item.name
        val opts = listOfNotNull(
            item.selectedSize?.let { "Size: $it" },
            item.selectedCrust?.let { "Crust: $it" },
            item.selectedTopping?.let { "Topping: $it" },
            item.notes?.takeIf { it.isNotBlank() }?.let { "Notes: $it" }
        ).joinToString(" · ")
        b.options.text = opts

        // Quantity and line price
        b.qty.text = item.quantity.toString()
        b.linePrice.text = currency.format(item.linePrice())

        // Image: prefer assets path (contains "/"), else treat as drawable name
        val assetPath = item.imageResName
        if (!assetPath.isNullOrBlank() && assetPath.contains("/")) {
            b.image.load("file:///android_asset/$assetPath") { crossfade(true) }
        } else {
            val resId = if (!assetPath.isNullOrBlank())
                b.image.context.resources.getIdentifier(
                    assetPath, "drawable", b.image.context.packageName
                ) else 0
            b.image.load(if (resId != 0) resId else R.drawable.ic_menu_gallery) { crossfade(true) }
        }

        // A11y control labels
        b.btnMinus.contentDescription = "Decrease ${item.name} quantity"
        b.btnPlus.contentDescription = "Increase ${item.name} quantity"
        b.btnRemove.contentDescription = "Remove ${item.name}"

        // Clicks (guard NO_POSITION)
        b.btnMinus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onMinus(pos)
        }
        b.btnPlus.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onPlus(pos)
        }
        b.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount(): Int = data.size

    /** Simple submit (can upgrade to ListAdapter+DiffUtil later). */
    fun submit(list: List<CartItem>) {
        data = list
        notifyDataSetChanged()
    }
}