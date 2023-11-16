package com.example.comp3606assignmentsolution.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.comp3606assignmentsolution.R
import com.example.comp3606assignmentsolution.models.MarketItem

class MarketplaceListAdapter: RecyclerView.Adapter<MarketplaceListAdapter.ViewHolder>() {
    private val adapterDataset = mutableListOf<MarketItem>()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quantityTextView: TextView = itemView.findViewById(R.id.tv_quantity)
        val itemNameTextView: TextView = itemView.findViewById(R.id.tv_item_name)
        val sellerNameTextView: TextView = itemView.findViewById(R.id.tv_seller_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.marketplace_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return adapterDataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = adapterDataset[position]
        holder.quantityTextView.text = item.itemQuantity
        holder.itemNameTextView.text = item.itemName
        holder.sellerNameTextView.text = item.sellerName
    }

    fun updateList(item: MarketItem){
        adapterDataset.add(0,item)
        notifyItemInserted(0)

    }
}