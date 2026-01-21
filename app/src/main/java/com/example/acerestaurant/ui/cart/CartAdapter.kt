package com.example.acerestaurant.ui.cart

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.acerestaurant.data.cart.CartItem
import com.example.acerestaurant.databinding.ItemCartBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

/**
 * RecyclerView adapter for the cart list.
 *
 * Consistent row layout (image, title, options, qty controls, price).
 * No PII is logged; destructive action (remove) requires confirmation.
 * Content descriptions are applied to critical controls per item.
 *
 * @param data the current cart items (use [submit] to refresh)
 * @param onMinus callback when quantity minus is pressed for an item position
 * @param onPlus callback when quantity plus is pressed for an item position
 * @param onRemove callback when a removal is confirmed for an item position
 */
class CartAdapter(
    private var data: List<CartItem>,
    private val onMinus: (position: Int) -> Unit,
    private val onPlus: (position: Int) -> Unit,
    private val onRemove: (position: Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.VH>() {

    private val currency = NumberFormat.getCurrencyInstance(Locale.getDefault())

    /** Simple ViewHolder exposing view binding. */
    class VH(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        val ctx = holder.binding.root.context

        with(holder.binding) {
            // Title and options summary
            title.text = item.name
            val opts = listOfNotNull(
                item.selectedSize?.let { "Size: $it" },
                item.selectedCrust?.let { "Crust: $it" },
                item.selectedTopping?.let { "Topping: $it" },
                item.notes?.takeIf { it.isNotBlank() }?.let { "Notes: $it" }
            ).joinToString(" · ")
            options.text = opts

            // Quantity & price
            qty.text = item.quantity.toString()
            linePrice.text = currency.format(item.linePrice())

            // --- Image binding ---
            // Prefer named drawable; fall back to assets; last resort to gallery icon.
            val drawableRes = item.imageResName?.let { name ->
                getDrawableIdByName(ctx, name)
            } ?: 0

            if (drawableRes != 0) {
                thumb.load(drawableRes) { crossfade(true) }
            } else {
                val assetRelPath = "image/${item.id}.jpg"
                val assetUrl = "file:///android_asset/$assetRelPath"
                if (assetExists(ctx, assetRelPath)) {
                    thumb.load(assetUrl) { crossfade(true) }
                } else {
                    Log.w("CartAdapter", "Missing asset: $assetRelPath")
                    thumb.load(android.R.drawable.ic_menu_gallery)
                }
            }

            // Accessibility: describe per-item controls
            btnMinus.contentDescription = "Decrease ${item.name} quantity"
            btnPlus.contentDescription = "Increase ${item.name} quantity"
            btnRemove.contentDescription = "Remove ${item.name} from cart"

            // Callbacks
            btnMinus.setOnClickListener { onMinus(holder.bindingAdapterPosition) }
            btnPlus.setOnClickListener { onPlus(holder.bindingAdapterPosition) }

            // Remove with confirmation dialog (ethical: avoid accidental destructive action)
            btnRemove.setOnClickListener {
                MaterialAlertDialogBuilder(root.context)
                    .setTitle("Remove item?")
                    .setMessage("Are you sure you want to remove ${item.name} from your cart?")
                    .setPositiveButton("Remove item") { _, _ ->
                        onRemove(holder.bindingAdapterPosition)
                    }
                    .setNegativeButton("Go Back") { dialog, _ -> dialog.dismiss() }
                    .setCancelable(true)
                    .show()
            }
        }
    }

    override fun getItemCount(): Int = data.size

    /** Replaces adapter data; prefer DiffUtil for large lists. */
    fun submit(list: List<CartItem>) {
        data = list
        notifyDataSetChanged()
    }

    /** Resolves a drawable by name (e.g., "img_pizza"). */
    private fun getDrawableIdByName(context: Context, name: String): Int {
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    /** Checks whether an asset exists under assets/ using a relative path. */
    private fun assetExists(context: Context, relPath: String): Boolean = try {
        context.assets.open(relPath).close(); true
    } catch (_: Exception) {
        false
    }
}