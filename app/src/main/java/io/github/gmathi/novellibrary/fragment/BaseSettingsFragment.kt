package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewbinding.ViewBinding
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaults

/**
 * Base fragment for settings screens that use the ListitemSetting pattern.
 * Provides common functionality for settings fragments with RecyclerView.
 */
open class BaseSettingsFragment<V, T: ListitemSetting<V>>(val options: List<T>) : BaseFragment(), GenericAdapter.Listener<T> {

    lateinit var adapter: GenericAdapter<T>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerView()
    }

    protected open fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(options), layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        
        // Find the RecyclerView in the content layout - it's nested inside contentRecyclerView
        val contentRecyclerView = view?.findViewById<View>(R.id.contentRecyclerView)
        val recyclerView = contentRecyclerView?.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val swipeRefreshLayout = contentRecyclerView?.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        
        recyclerView?.let {
            it.setDefaults(adapter)
            it.addItemDecoration(CustomDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
        
        swipeRefreshLayout?.isEnabled = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun bind(item: T, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        bindSettingListitemDefaults(itemBinding, resources.getString(item.name), resources.getString(item.description), position)

        options.getOrNull(position)?.bindCallback?.let { it(this as V, item, itemBinding, position) }
    }

    /**
     * Fragment version of bindSettingListitemDefaults
     */
    private fun bindSettingListitemDefaults(itemBinding: ListitemTitleSubtitleWidgetBinding, name: String, description: String, position: Int) {
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE
        itemBinding.subtitle.visibility = View.VISIBLE
        itemBinding.widget.visibility = View.VISIBLE
        itemBinding.currentValue.text = ""

        itemBinding.title.applyFont(requireContext().assets).text = name
        itemBinding.subtitle.applyFont(requireContext().assets).text = description
        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        itemBinding.root.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(requireContext(), R.color.black_transparent)
            else ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun onItemClick(item: T, position: Int) {
        options.getOrNull(position)?.clickCallback?.let { it(this as V, item, position) }
    }
}