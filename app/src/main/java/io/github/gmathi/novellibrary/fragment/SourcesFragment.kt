package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.createOrUpdateLargePreference
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.getPreferenceKey
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SourcesFragment : BaseFragment(), GenericAdapter.Listener<HttpSource> {

    companion object {
        fun newInstance() = SourcesFragment()
    }

    private lateinit var binding: ContentRecyclerViewBinding
    private lateinit var adapter: GenericAdapter<HttpSource>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false)
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.recyclerView.setDefaults(adapter)
        binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: HttpSource, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.widgetChevron.visibility = View.GONE
        itemBinding.widgetSwitch.visibility = View.VISIBLE
        itemBinding.currentValue.visibility = View.GONE
        itemBinding.blackOverlay.visibility = View.GONE
        itemBinding.subtitle.visibility = View.GONE
        itemBinding.icon.visibility = View.GONE


        val drawable = Injekt.get<ExtensionManager>().getAppIconForSource(item, itemView.context)
        if (drawable != null) {
            itemBinding.icon.visibility = View.VISIBLE
            itemBinding.icon.setImageDrawable(drawable)
        }

        itemBinding.title.applyFont(requireActivity().assets).text = item.name

        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)

        itemBinding.widgetSwitch.isChecked = dataCenter.isSourceEnabled(item.getPreferenceKey())
        itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableSource(item.getPreferenceKey(), value) }
        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(requireContext(), R.color.black_transparent)
            else ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
    }

    override fun onItemClick(item: HttpSource, position: Int) {

    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
//        menuInflater.inflate(R.menu.menu_recently_viewed_novels, menu)
        return super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear)
            MaterialDialog(requireActivity()).show {
                message(text = "Are you sure you want to clear all the recently viewed novels list?")
                positiveButton(text = "Yes") { dialog ->
                    dbHelper.createOrUpdateLargePreference(
                        Constants.LargePreferenceKeys.RVN_HISTORY,
                        "[]"
                    )
                    adapter.updateData(ArrayList())
                    dialog.dismiss()
                }
                negativeButton(R.string.cancel) {
                    it.dismiss()
                }
            }
        return super.onOptionsItemSelected(item)
    }
    //endregion

    override fun onResume() {
        super.onResume()
        adapter.updateData(ArrayList(sourceManager.getOnlineSources()))
    }
}