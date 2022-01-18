package io.github.gmathi.novellibrary.activity.settings.reader

import android.text.InputType
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.BaseSettingsActivity
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MAX
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MIN
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.system.startReaderBackgroundSettingsActivity
import io.github.gmathi.novellibrary.util.system.startScrollBehaviourSettingsActivity
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlin.math.abs

class ReaderSettingsActivity : BaseSettingsActivity<ReaderSettingsActivity, ReaderSetting>(OPTIONS) {

    companion object {
        private val OPTIONS = listOf(
            ReaderSetting(R.string.reader_mode, R.string.reader_mode_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.readerMode) { _, value ->
                    dataCenter.readerMode = value
                    if (value) {
                        dataCenter.javascriptDisabled = value
                        adapter.notifyDataSetChanged()
                    }
                }
            },
            ReaderSetting(R.string.disable_javascript, R.string.disable_javascript_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.javascriptDisabled) { _, value ->
                    dataCenter.javascriptDisabled = value
                    if (!value) {
                        dataCenter.readerMode = value
                        adapter.notifyDataSetChanged()
                    }
                }
            },
            ReaderSetting(R.string.reader_mode_colors, R.string.reader_mode_colors_description).bindChevron { _, _ ->
                startReaderBackgroundSettingsActivity()
            },
            ReaderSetting(R.string.reader_mode_scroll, R.string.reader_mode_scroll_description).bindChevron { _, _ ->
                startScrollBehaviourSettingsActivity()
            },
            ReaderSetting(R.string.swipe_right_for_next_chapter, R.string.swipe_right_for_next_chapter_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.japSwipe) { _, value -> dataCenter.japSwipe = value }
            },
            ReaderSetting(R.string.show_comments_section, R.string.show_comments_section_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.showChapterComments) { _, value -> dataCenter.showChapterComments = value }
            },
            ReaderSetting(R.string.keep_screen_on, R.string.keep_screen_on_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.keepScreenOn) { _, value -> dataCenter.keepScreenOn = value }
            },
            ReaderSetting(R.string.immersive_mode, R.string.immersive_mode_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.enableImmersiveMode) { _, value -> dataCenter.enableImmersiveMode = value }
            },
            ReaderSetting(R.string.show_navbar_at_chapter_end, R.string.show_navbar_at_chapter_end_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.showNavbarAtChapterEnd) { _, value -> dataCenter.showNavbarAtChapterEnd = value }
            },
            ReaderSetting(R.string.merge_pages, R.string.merge_pages_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.enableClusterPages) { _, value -> dataCenter.enableClusterPages = value }
            },
            ReaderSetting(R.string.directional_links, R.string.directional_links_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.enableDirectionalLinks) { _, value -> dataCenter.enableDirectionalLinks = value }
            },
            ReaderSetting(R.string.reader_mode_button_visibility, R.string.reader_mode_button_visibility_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.isReaderModeButtonVisible) { _, value -> dataCenter.isReaderModeButtonVisible = value }
            },
            ReaderSetting(R.string.keep_text_color, R.string.keep_text_color_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.keepTextColor) { _, value -> dataCenter.keepTextColor = value }
            },
            ReaderSetting(R.string.alternative_text_colors, R.string.alternative_text_colors_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.alternativeTextColors) { _, value -> dataCenter.alternativeTextColors = value }
            },
            ReaderSetting(R.string.limit_image_width, R.string.limit_image_width_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.limitImageWidth) { _, value -> dataCenter.limitImageWidth = value }
            },
            ReaderSetting(R.string.linkify_text, R.string.linkify_text_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.linkifyText) { _, value -> dataCenter.linkifyText = value }
            },
//            ReaderSetting(R.string.auto_read_next_chapter, R.string.auto_read_next_chapter_description).onBind { _, view, _ ->
//                view.bindSwitch(dataCenter.readAloudNextChapter) { _, value -> dataCenter.readAloudNextChapter = value }
//            },
            ReaderSetting(R.string.custom_query_lookups, R.string.custom_query_lookups_description).bindChevron { _, _ ->
                MaterialDialog(this).show {
                    title(R.string.custom_query_lookups_edit)
                    input(
                        hintRes = R.string.custom_query_lookups_hint,
                        prefill = dataCenter.userSpecifiedSelectorQueries,
                        inputType = InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_CLASS_TEXT
                    )
                    positiveButton(R.string.fui_button_text_save) { widget ->
                        dataCenter.userSpecifiedSelectorQueries = widget.getInputField().text.toString()
                        firebaseAnalytics.logEvent(FAC.Event.SELECTOR_QUERY) {
                            param(FirebaseAnalytics.Param.VALUE, widget.getInputField().text.toString())
                        }
                    }
                    negativeButton(R.string.cancel)
                }
            },
        )
    }

    private fun changeScrollDistance(textView: TextView) {
        var value = dataCenter.volumeScrollLength

        val dialog = MaterialDialog(this).show {
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

private typealias ReaderSetting = ListitemSetting<ReaderSettingsActivity>
