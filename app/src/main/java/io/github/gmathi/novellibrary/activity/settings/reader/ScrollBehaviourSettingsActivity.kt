package io.github.gmathi.novellibrary.activity.settings.reader

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.BaseSettingsActivity
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MAX
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MIN
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlin.math.abs

private typealias Settings = ListitemSetting<ScrollBehaviourSettingsActivity>
class ScrollBehaviourSettingsActivity : BaseSettingsActivity<ScrollBehaviourSettingsActivity, Settings>(OPTIONS) {

    companion object {
        @SuppressLint("NotifyDataSetChanged")
        private val OPTIONS = listOf(

            Settings(R.string.show_reader_scroll, R.string.show_reader_scroll_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.showReaderScroll) { _, value -> dataCenter.showReaderScroll = value }
            },

            //region Volume Scroll Settings
            Settings(R.string.volume_scroll, R.string.volume_scroll_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.enableVolumeScroll) { _, value ->
                    dataCenter.enableVolumeScroll = value
                    adapter.notifyDataSetChanged()
                }
            },

            Settings(R.string.volume_scroll_length, R.string.volume_scroll_length_description).onBind { _, view, _ ->
                val enable = dataCenter.enableVolumeScroll
                //itemView.enabled(enable)
                view.blackOverlay.visibility =
                    if (enable)
                        View.INVISIBLE
                    else
                        View.VISIBLE
                view.currentValue.visibility = View.VISIBLE
                val value = dataCenter.volumeScrollLength
                view.currentValue.text = resources.getString(
                    R.string.volume_scroll_length_template,
                    if (value < 0) resources.getString(R.string.reverse) else "",
                    abs(value)
                )
                if (enable)
                    view.root.setOnClickListener {
                        changeVolumeScrollDistance(view.currentValue)
                    }
                else view.root.setOnClickListener(null)
            },
            //endregion

            //region Auto Scroll Settings
            Settings(R.string.auto_scroll, R.string.auto_scroll_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.enableAutoScroll) { _, value ->
                    dataCenter.enableAutoScroll = value
                    adapter.notifyDataSetChanged()
                }
            },

            Settings(R.string.auto_scroll_length, R.string.auto_scroll_length_description).onBind { _, view, _ ->
                val enable = dataCenter.enableAutoScroll
                //itemView.enabled(enable)
                view.blackOverlay.visibility =
                    if (enable)
                        View.INVISIBLE
                    else
                        View.VISIBLE
                view.currentValue.visibility = View.VISIBLE
                val value = dataCenter.autoScrollLength
                view.currentValue.text = abs(value).toString()
                if (enable)
                    view.root.setOnClickListener {
                        changeAutoScrollDistance(view.currentValue)
                    }
                else view.root.setOnClickListener(null)
            },

            Settings(R.string.auto_scroll_interval, R.string.auto_scroll_inteval_description).onBind { _, view, _ ->
                val enable = dataCenter.enableAutoScroll
                //itemView.enabled(enable)
                view.blackOverlay.visibility =
                    if (enable)
                        View.INVISIBLE
                    else
                        View.VISIBLE
                view.currentValue.visibility = View.VISIBLE
                val value = dataCenter.autoScrollInterval
                view.currentValue.text = abs(value).toString()
                if (enable)
                    view.root.setOnClickListener {
                        changeAutoScrollInterval(view.currentValue)
                    }
                else view.root.setOnClickListener(null)
            },
            //endregion
        )
    }

    private fun changeVolumeScrollDistance(textView: TextView) {
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

    private fun changeAutoScrollDistance(textView: TextView) {
        var value = dataCenter.autoScrollLength

        val dialog = MaterialDialog(this).show {
            title(R.string.volume_scroll_length)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.autoScrollLength = value
            }
        }

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = progress.toInt()
            textView.text = abs(value).toString()
        }
        seekBar.setAbsoluteMinMaxValue(0.toDouble(), VOLUME_SCROLL_LENGTH_MAX.toDouble())
        seekBar.setProgress(dataCenter.autoScrollLength.toDouble())
    }

    private fun changeAutoScrollInterval(textView: TextView) {
        var value = dataCenter.autoScrollInterval

        val dialog = MaterialDialog(this).show {
            title(R.string.volume_scroll_length)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.autoScrollInterval = value
            }
        }

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = progress.toInt()
            textView.text = abs(value).toString()
        }
        seekBar.setAbsoluteMinMaxValue(0.toDouble(), VOLUME_SCROLL_LENGTH_MAX.toDouble())
        seekBar.setProgress(dataCenter.autoScrollInterval.toDouble())
    }

}
