package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
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
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.*
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
            TTSSetting(R.string.tts_language, R.string.tts_language_description).bindChevron { _, _ ->
                selectLanguage()
            },
            TTSSetting(R.string.tts_filters, R.string.tts_filters_description).bindChevron { _, _ ->
                selectFilters()
            },
            TTSSetting(R.string.tts_update_filters, R.string.tts_update_filters_description).bindChevron { _, _ ->
                updateFilterSources()
            },
            TTSSetting(R.string.tts_strip_header, R.string.tts_strip_header_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsStripHeader) { _, value -> dataCenter.ttsStripHeader = value }
            },
            TTSSetting(R.string.tts_chapter_change_sfx, R.string.tts_chapter_change_sfx_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsChapterChangeSFX) { _, value -> dataCenter.ttsChapterChangeSFX = value }
            },
        )

        val filterSources: List<TTSFilterSource> = listOf(
            TTSFilterSource("naggers", "1.0.0-beta", "Crawler naggers",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/naggers.json"),
            TTSFilterSource("tts-adjust", "1.0.0-beta", "TTS pronunciation fixes",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/tts-adjust.json"),
            TTSFilterSource("scene-changers", "1.0.0-beta", "Scene change sound effect",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/scene-changers.json")
//            TTSFilterSource("notn", "TN be gone!", "")
        )

    }

    private var tts: MediaControllerCompat? = null
    private lateinit var browser: MediaBrowserCompat
    private val connCallback = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()
            browser.sessionToken.also {
                tts = MediaControllerCompat(this@TTSSettingsActivity, it)
            }
        }

        override fun onConnectionSuspended() {
            tts = null
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browser = MediaBrowserCompat(this, ComponentName(this, TTSService::class.java), connCallback, null)
    }

    override fun onStart() {
        super.onStart()
        browser.connect()
    }

    override fun onStop() {
        super.onStop()
        browser.disconnect()
    }

    fun selectLanguage() {
        @Suppress("JoinDeclarationAndAssignment")
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(this) {
            var language: Locale? = dataCenter.ttsLanguage

            val dialog = MaterialDialog(this).show {
                title(R.string.tts_language)
                customView(R.layout.dialog_list, scrollable = true)
                positiveButton(R.string.okay) {
                    dataCenter.ttsLanguage = language
                    tts?.sendCommand(TTSService.COMMAND_UPDATE_LANGUAGE, null, null)
                }
                negativeButton(R.string.cancel) {  dismiss() }
            }


            val group = dialog.getCustomView().findViewById<RadioGroup>(R.id.listGroup) ?: return@TextToSpeech

            run {
                // Default setting
                val button = RadioButton(this)
                button.id = View.generateViewId()
                button.text = getString(R.string.tts_language_default)
                button.isChecked = language == null
                group.addView(button)
            }

            val buttons = engine.availableLanguages.sortedBy {
                it.getDisplayName(Locale.getDefault())
            }.map {
                val button = RadioButton(this)
                button.id = View.generateViewId()
                button.text = it.getDisplayName(Locale.getDefault())
                button.isChecked = language == it
                group.addView(button)
                button.id to it
            }.toMap()

            group.setOnCheckedChangeListener { _, _ ->
                language = buttons[group.checkedRadioButtonId]
            }

        }
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

        val filterCache = dataCenter.ttsFilterCache
        val group = dialog.getCustomView().findViewById<LinearLayout>(R.id.listGroup) ?: return
        checkToId = filterSources.map { source ->
            val button = SwitchCompat(this)
            button.id = View.generateViewId()
            @SuppressLint("SetTextI18n")
            button.text = source.name + " [" + (filterCache[source.id]?.version ?: "not cached") + "]"
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
        @SuppressLint("CheckResult")
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
                tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            }
        }

        // 50...200 range is bugged for TwoWaySeekBar as it's not including 0, so a crutch to offset it by -50
        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.setAbsoluteMinMaxValue(0.0, 150.0)
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = (progress.roundToInt() / 100.0).toFloat() + 0.5f
            tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
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
                tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            }
        }

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.setAbsoluteMinMaxValue(0.0, 150.0)
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = (progress.roundToInt() / 100.0).toFloat() + 0.5f
            tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            @SuppressLint("SetTextI18n")
            textView.text = (progress+50.0).roundToInt().toString() + "%"
        }
        seekBar.setProgress(value.toDouble() * 100.0 - 50.0)
    }
}