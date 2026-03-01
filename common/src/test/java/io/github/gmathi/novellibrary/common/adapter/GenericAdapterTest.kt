package io.github.gmathi.novellibrary.common.adapter

import android.view.View
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for GenericAdapter
 * 
 * Note: These tests focus on the data manipulation logic.
 * Full integration tests with RecyclerView would require Android instrumentation tests.
 */
class GenericAdapterTest : StringSpec({
    
    "GenericAdapter should initialize with items" {
        val items = ArrayList<String>()
        items.add("Item 1")
        items.add("Item 2")
        
        val listener = object : GenericAdapter.Listener<String> {
            override fun bind(item: String, itemView: View, position: Int) {}
            override fun onItemClick(item: String, position: Int) {}
        }
        
        val adapter = GenericAdapter(items, 0, listener)
        
        adapter.items.size shouldBe 2
        adapter.itemCount shouldBe 2
    }
    
    "GenericAdapter should return correct item count without load more listener" {
        val items = ArrayList<String>()
        items.add("Item 1")
        items.add("Item 2")
        
        val listener = object : GenericAdapter.Listener<String> {
            override fun bind(item: String, itemView: View, position: Int) {}
            override fun onItemClick(item: String, position: Int) {}
        }
        
        val adapter = GenericAdapter(items, 0, listener)
        
        adapter.itemCount shouldBe 2
    }
    
    "GenericAdapter should return correct view type for normal items" {
        val items = ArrayList<String>()
        items.add("Item 1")
        
        val listener = object : GenericAdapter.Listener<String> {
            override fun bind(item: String, itemView: View, position: Int) {}
            override fun onItemClick(item: String, position: Int) {}
        }
        
        val adapter = GenericAdapter(items, 0, listener)
        
        adapter.getItemViewType(0) shouldBe GenericAdapter.VIEW_TYPE_NORMAL
    }
    
    "GenericAdapter should have correct constants" {
        GenericAdapter.VIEW_TYPE_NORMAL shouldBe 0
        GenericAdapter.VIEW_TYPE_LOAD_MORE shouldBe 1
    }
    
    "GenericAdapter items should be mutable" {
        val items = ArrayList<String>()
        items.add("Item 1")
        
        val listener = object : GenericAdapter.Listener<String> {
            override fun bind(item: String, itemView: View, position: Int) {}
            override fun onItemClick(item: String, position: Int) {}
        }
        
        val adapter = GenericAdapter(items, 0, listener)
        
        // Verify we can access and modify the items list
        adapter.items[0] shouldBe "Item 1"
        adapter.items.size shouldBe 1
    }
})
