package com.example.acerestaurant.ui.menu

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.acerestaurant.R
import com.example.acerestaurant.data.model.MenuItem
import com.example.acerestaurant.databinding.ItemMenuBinding

/**
 * RecyclerView Adapter for showing menu items as cards.
 *
 * Consistent card layout (image, name, description, price).
 * Adds contentDescription to images, clear click target.
 * Keeps formatting and asset resolution in one place.
 */
class MenuAdapter(
    private var data: List<MenuItem>,
    private val onClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.VH>() {

    /** Holds view binding for item_menu.xml. */
    class VH(val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMenuBinding.inflate(
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
            // Bind text fields
            title.text = item.name
            description.text = item.description
            price.text = String.format("$%.2f", item.price)

            // ContentDescription for a11y readers
            image.contentDescription = ctx.getString(R.string.cd_menu_item_image, item.name)

            // Preferred JSON path: e.g. "image/pizza_margherita.jpg"
            val assetUrl = item.image?.trim()?.takeIf { it.isNotEmpty() }?.let {
                "file:///android_asset/$it"
            }
            // Fallback: construct from id convention
            val fallback = "file:///android_asset/image/${item.id}.jpg"

            image.load(assetUrl ?: fallback) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
                crossfade(true)
            }

            // Optional debug for missing assets (no PII)
            val rel = (assetUrl ?: fallback).removePrefix("file:///android_asset/")
            if (!assetExists(ctx, rel)) {
                Log.w("MenuAdapter", "Missing asset: $rel for id=${item.id}")
            }

            // Click navigation
            root.setOnClickListener { onClick(item) }
        }
    }

    override fun getItemCount(): Int = data.size

    /** Replace list; consider DiffUtil for large lists. */
    fun submit(list: List<MenuItem>) {
        data = list
        notifyDataSetChanged()
    }

    /** Check if an asset exists under /assets. */
    private fun assetExists(context: Context, relPath: String): Boolean = try {
        context.assets.open(relPath).close(); true
    } catch (_: Exception) {
        false
    }
}