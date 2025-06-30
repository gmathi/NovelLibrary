package io.github.gmathi.novellibrary.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class OptimizedAdapter<T, VH : RecyclerView.ViewHolder>(
    private val items: MutableList<T> = mutableListOf()
) : RecyclerView.Adapter<VH>() {
    
    init {
        setHasStableIds(true)
    }
    
    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }
    
    override fun getItemCount(): Int = items.size
    
    fun getItem(position: Int): T = items[position]
    
    fun getItems(): List<T> = items.toList()
    
    fun updateItems(newItems: List<T>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            
            override fun getNewListSize(): Int = newItems.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areItemsTheSameImpl(items[oldItemPosition], newItems[newItemPosition])
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return areContentsTheSameImpl(items[oldItemPosition], newItems[newItemPosition])
            }
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }
    
    fun addItem(item: T) {
        val position = items.size
        items.add(item)
        notifyItemInserted(position)
    }
    
    fun addItems(newItems: List<T>) {
        val startPosition = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }
    
    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
    
    fun updateItem(position: Int, item: T) {
        if (position in items.indices) {
            items[position] = item
            notifyItemChanged(position)
        }
    }
    
    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }
    
    protected abstract fun areItemsTheSameImpl(oldItem: T, newItem: T): Boolean
    protected abstract fun areContentsTheSameImpl(oldItem: T, newItem: T): Boolean
} 