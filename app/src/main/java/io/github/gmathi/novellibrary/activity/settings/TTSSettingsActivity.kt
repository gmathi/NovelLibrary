package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.other.TTSFilter
import io.github.gmathi.novellibrary.model.other.TTSFilterList
import io.github.gmathi.novellibrary.model.other.TTSFilterSource
import io.github.gmathi.novellibrary.model.ui.ListitemSetting
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.math.abs
import kotlin.math.roundToInt

private typealias TTSSetting = ListitemSetting<TTSSettingsActivity>
class TTSSettingsActivity : BaseSettingsActivity<TTSSettingsActivity, TTSSetting>(OPTIONS) {

    companion object {

        private val OPTIONS = listOf(
            TTSSetting(R.string.tts_pitch, R.string.tts_pitch_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                val value = dataCenter.ttsPitch
                @SuppressLint("SetTextI18n")
                view.currentValue.text = (value * 100).roundToInt().toString() + "%"
                view.root.setOnClickListener {
                    changePitch(view.currentValue)
                }
            },
            TTSSetting(R.string.tts_rate, R.string.tts_rate_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                val value = dataCenter.ttsSpeechRate
                @SuppressLint("SetTextI18n")
                view.currentValue.text = (value * 100).roundToInt().toString() + "%"
                view.root.setOnClickListener {
                    changeRate(view.currentValue)
                }
            },
            TTSSetting(R.string.auto_read_next_chapter, R.string.auto_read_next_chapter_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.readAloudNextChapter) { _, value -> dataCenter.readAloudNextChapter = value }
            },
            TTSSetting(R.string.tts_move_bookmark, R.string.tts_move_bookmark_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsMoveBookmark) { _, value -> dataCenter.ttsMoveBookmark = value }
            },
            TTSSetting(R.string.tts_mark_chapters_read, R.string.tts_mark_chapters_read_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsMarkChaptersRead) { _, value -> dataCenter.ttsMarkChaptersRead = value }
            },
            TTSSetting(R.string.tts_merge_buffer_chapters, R.string.tts_merge_buffer_chapters_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsMergeBufferChapters) { _, value -> dataCenter.ttsMergeBufferChapters = value }
            },
            TTSSetting(R.string.tts_filters, R.string.tts_filters_description).bindChevron { _, _ ->
                selectFilters()
            },
            TTSSetting(R.string.tts_update_filters, R.string.tts_update_filters_description).bindChevron { _, _ ->
                updateFilterSources()
            }
        )

        val filterSources: List<TTSFilterSource> = listOf(
            TTSFilterSource("naggers", "1.0.0-beta", "Crawler naggers",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/naggers.json"),
//            TTSFilterSource("notn", "TN be gone!", "")
        )

    }

    private var tts: TTSService? = null
    private var isServiceConnected: Boolean = false

    private val ttsConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as TTSService.TTSBinder
            tts = binder.getInstance()
            isServiceConnected = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isServiceConnected = false
            tts = null
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TTSService::class.java).also { intent ->
            bindService(intent, ttsConnection, 0)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(ttsConnection)
        tts = null
        isServiceConnected = false
    }

    fun selectFilters() {
        val originalFilters = dataCenter.ttsFilters
        val filters = originalFilters.toMutableList()

        val dialog = MaterialDialog(this).show {
            title(R.string.tts_filters)
            customView(R.layout.dialog_checkbox_list, scrollable = true)
            positiveButton(R.string.okay) {
                if (originalFilters.size != filters.size || !originalFilters.containsAll(filters)) {
                    dataCenter.ttsFilters = filters
                    val cache = dataCenter.ttsFilterCache
                    if (!filters.all { id -> cache.containsKey(id) }) {
                        updateFilterSources()
                    } else {
                        rebuildEnabledFilters(filters, cache)
                    }
                }
            }
            negativeButton(R.string.cancel) { dismiss() }
        }

        lateinit var checkToId: Map<Int, String>

        val onChangeListener = CompoundButton.OnCheckedChangeListener { button, checked ->
            val sourceId = checkToId[button.id] ?: return@OnCheckedChangeListener
            if (checked)
                filters.add(sourceId)
            else
                filters.remove(sourceId)
        }

        val group = dialog.getCustomView().findViewById<LinearLayout>(R.id.listGroup) ?: return
        checkToId = filterSources.map { source ->
            val button = SwitchCompat(this)
            button.id = View.generateViewId()
            button.text = source.name
            button.isChecked = filters.contains(source.id)
            button.setOnCheckedChangeListener(onChangeListener)
            group.addView(button)
            button.id to source.id
        }.toMap()
    }

    fun updateFilterSources() {
        if (!networkHelper.isConnectedToNetwork()) {
            toast(R.string.tts_need_connection_for_filters, Toast.LENGTH_LONG)
            return
        }
        val dialog = MaterialDialog(this).show {
            noAutoDismiss()
            title(R.string.tts_updating_filters)
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val cache = dataCenter.ttsFilterCache.toMutableMap()
                filterSources.forEach { source ->
                    val request = Request.Builder()
                        .url(source.url + "?timestamp=" + System.currentTimeMillis())
                        .get()
                        .build()
                    val response = networkHelper.client.newCall(request).safeExecute()
                    response.body?.string()?.let {
                        cache[source.id] = Gson().fromJson(it)
                    }
                }
                dataCenter.ttsFilterCache = cache
                rebuildEnabledFilters(dataCenter.ttsFilters, cache)
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun rebuildEnabledFilters(filters: List<String>, cache: Map<String, TTSFilterList>) {
        val list = mutableListOf<TTSFilter>()

        filters.forEach { name ->
            cache[name]?.let { list.addAll(it.list) }
        }

        dataCenter.ttsFilterList = list
    }

    fun changePitch(textView: TextView) {
        var value = dataCenter.ttsPitch
        val dialog = MaterialDialog(this).show {
            title(R.string.tts_rate)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.ttsPitch = value
                tts?.chooseMediaControllerActions(TTSService.ACTION_UPDATE_TTS_SETTINGS)
            }
        }

        // 50...200 range is bugged for TwoWaySeekBar as it's not including 0, so a crutch to offset it by -50
        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.setAbsoluteMinMaxValue(0.0, 150.0)
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = (progress.roundToInt() / 100.0).toFloat() + 0.5f
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_UPDATE_TTS_SETTINGS)
            @SuppressLint("SetTextI18n")
            textView.text = (progress+50.0).roundToInt().toString() + "%"
        }
        seekBar.setProgress(value.toDouble() * 100.0 - 50.0)
    }

    fun changeRate(textView: TextView) {
        var value = dataCenter.ttsSpeechRate
        val dialog = MaterialDialog(this).show {
            title(R.string.tts_rate)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.ttsSpeechRate = value
                tts?.chooseMediaControllerActions(TTSService.ACTION_UPDATE_TTS_SETTINGS)
            }
        }

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.setAbsoluteMinMaxValue(0.0, 150.0)
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = (progress.roundToInt() / 100.0).toFloat() + 0.5f
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_UPDATE_TTS_SETTINGS)
            @SuppressLint("SetTextI18n")
            textView.text = (progress+50.0).roundToInt().toString() + "%"
        }
        seekBar.setProgress(value.toDouble() * 100.0 - 50.0)
    }
}