package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.FragmentReaderSettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MAX
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MIN
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import kotlin.math.abs

@AndroidEntryPoint
class ReaderSettingsFragment : BaseViewBindingFragment<FragmentReaderSettingsBinding>(), GenericAdapter.Listener<ReaderSetting> {

    companion object {
        private val OPTIONS = listOf(

    private lateinit var adapter: GenericAdapter<ReaderSetting>
    
    companion object {
        private val OPTIONS = listOf(
            ReaderSetting(R.string.reader_mode, R.string.reader_mode_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.readerMode) { _, value ->
                    fragment.dataCenter.readerMode = value
                    if (value) {
                        fragment.dataCenter.javascriptDisabled = value
                        fragment.adapter.notifyDataSetChanged()
                    }
                }
            },
            ReaderSetting(R.string.disable_javascript, R.string.disable_javascript_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.javascriptDisabled) { _, value ->
                    fragment.dataCenter.javascriptDisabled = value
                    if (!value) {
                        fragment.dataCenter.readerMode = value
                        fragment.adapter.notifyDataSetChanged()
                    }
                }
            },
            ReaderSetting(R.string.reader_mode_colors, R.string.reader_mode_colors_description).bindChevron { fragment, _ ->
                fragment.findNavController().navigate(
                    ReaderSettingsFragmentDirections.actionReaderSettingsToBackgroundSettings()
                )
            },
            ReaderSetting(R.string.reader_mode_scroll, R.string.reader_mode_scroll_description).bindChevron { fragment, _ ->
                fragment.findNavController().navigate(
                    ReaderSettingsFragmentDirections.actionReaderSettingsToScrollBehaviour()
                )
            },
            ReaderSetting(R.string.swipe_right_for_next_chapter, R.string.swipe_right_for_next_chapter_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.japSwipe) { _, value -> fragment.dataCenter.japSwipe = value }
            },
            ReaderSetting(R.string.show_comments_section, R.string.show_comments_section_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.showChapterComments) { _, value -> fragment.dataCenter.showChapterComments = value }
            },
            ReaderSetting(R.string.keep_screen_on, R.string.keep_screen_on_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.keepScreenOn) { _, value -> fragment.dataCenter.keepScreenOn = value }
            },
            ReaderSetting(R.string.immersive_mode, R.string.immersive_mode_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.enableImmersiveMode) { _, value -> fragment.dataCenter.enableImmersiveMode = value }
            },
            ReaderSetting(R.string.show_navbar_at_chapter_end, R.string.show_navbar_at_chapter_end_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.showNavbarAtChapterEnd) { _, value -> fragment.dataCenter.showNavbarAtChapterEnd = value }
            },
            ReaderSetting(R.string.merge_pages, R.string.merge_pages_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.enableClusterPages) { _, value -> fragment.dataCenter.enableClusterPages = value }
            },
            ReaderSetting(R.string.directional_links, R.string.directional_links_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.enableDirectionalLinks) { _, value -> fragment.dataCenter.enableDirectionalLinks = value }
            },
            ReaderSetting(R.string.reader_mode_button_visibility, R.string.reader_mode_button_visibility_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.isReaderModeButtonVisible) { _, value -> fragment.dataCenter.isReaderModeButtonVisible = value }
            },
            ReaderSetting(R.string.keep_text_color, R.string.keep_text_color_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.keepTextColor) { _, value -> fragment.dataCenter.keepTextColor = value }
            },
            ReaderSetting(R.string.alternative_text_colors, R.string.alternative_text_colors_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.alternativeTextColors) { _, value -> fragment.dataCenter.alternativeTextColors = value }
            },
            ReaderSetting(R.string.limit_image_width, R.string.limit_image_width_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.limitImageWidth) { _, value -> fragment.dataCenter.limitImageWidth = value }
            },
            ReaderSetting(R.string.linkify_text, R.string.linkify_text_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.linkifyText) { _, value -> fragment.dataCenter.linkifyText = value }
            },
            ReaderSetting(R.string.custom_query_lookups, R.string.custom_query_lookups_description).bindChevron { fragment, _ ->
                MaterialDialog(fragment.requireContext()).show {
                    title(R.string.custom_query_lookups_edit)
                    input(
                        hintRes = R.string.custom_query_lookups_hint,
                        prefill = fragment.dataCenter.userSpecifiedSelectorQueries,
                        inputType = InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_CLASS_TEXT
                    )
                    positiveButton(com.firebase.ui.auth.R.string.fui_button_text_save) { widget ->
                        fragment.dataCenter.userSpecifiedSelectorQueries = widget.getInputField().text.toString()
                        fragment.firebaseAnalytics.logEvent(FAC.Event.SELECTOR_QUERY) {
                            param(FirebaseAnalytics.Param.VALUE, widget.getInputField().text.toString())
                        }
                    }
                    negativeButton(R.string.cancel)
                }
            },
        )
    }

    override fun getLayoutId() = R.layout.fragment_reader_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding(FragmentReaderSettingsBinding.bind(view))
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(OPTIONS), layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: ReaderSetting, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        bindSettingListitemDefaults(itemBinding, resources.getString(item.name), resources.getString(item.description), position)

        OPTIONS.getOrNull(position)?.bindCallback?.let { it(this, item, itemBinding, position) }
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

    override fun onItemClick(item: ReaderSetting, position: Int) {
        OPTIONS.getOrNull(position)?.clickCallback?.let { it(this, item, position) }
    }

    private fun changeScrollDistance(textView: TextView) {
        var value = dataCenter.volumeScrollLength

        val dialog = MaterialDialog(requireContext()).show {
            title(R.string.volume_scroll_length)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.volumeScrollLength = value
            }
        }

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = progress.toInt()
            textView.text = resources.getString(
                R.string.volume_scroll_length_template,
                if (value < 0) resources.getString(R.string.reverse) else "",
                abs(value)
            )
        }
        seekBar.setAbsoluteMinMaxValue(VOLUME_SCROLL_LENGTH_MIN.toDouble(), VOLUME_SCROLL_LENGTH_MAX.toDouble())
        seekBar.setProgress(dataCenter.volumeScrollLength.toDouble())
    }
}

private typealias ReaderSetting = ListitemSetting<ReaderSettingsFragment>