package io.github.gmathi.novellibrary.fragment

import android.text.InputType
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindSwitch

@AndroidEntryPoint
class ReaderSettingsFragment : BaseSettingsFragment<ReaderSettingsFragment, ReaderSetting>(OPTIONS) {

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
}

private typealias ReaderSetting = ListitemSetting<ReaderSettingsFragment>