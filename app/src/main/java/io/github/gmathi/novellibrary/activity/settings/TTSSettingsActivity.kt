package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
import io.github.gmathi.novellibrary.util.fromHumanPercentage
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.bindChevron
import io.github.gmathi.novellibrary.util.system.bindHeader
import io.github.gmathi.novellibrary.util.system.bindSwitch
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.toHumanPercentage
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.*
import kotlin.math.round
import kotlin.math.roundToInt

private typealias TTSSetting = ListitemSetting<TTSSettingsActivity>
class TTSSettingsActivity : BaseSettingsActivity<TTSSettingsActivity, TTSSetting>(OPTIONS) {

    companion object {

        private val OPTIONS = listOf(
            TTSSetting(R.string.tts_header_playback, R.string.empty).bindHeader(),
            TTSSetting(R.string.tts_pitch, R.string.tts_pitch_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                @SuppressLint("SetTextI18n")
                view.currentValue.text = (dataCenter.ttsPreferences.pitch * 100).roundToInt().toString() + "%"
                view.root.setOnClickListener {
                    sliderMenu(view.currentValue, dataCenter.ttsPreferences.pitch, TTSService.PITCH_MIN, TTSService.PITCH_MAX) { value, closing ->
                        if (closing) dataCenter.ttsPreferences.pitch = value
                        value
                    }
                }
            },
            TTSSetting(R.string.tts_rate, R.string.tts_rate_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                @SuppressLint("SetTextI18n")
                view.currentValue.text = (dataCenter.ttsPreferences.speechRate * 100).roundToInt().toString() + "%"
                view.root.setOnClickListener {
                    sliderMenu(view.currentValue, dataCenter.ttsPreferences.speechRate, TTSService.SPEECH_RATE_MIN, TTSService.SPEECH_RATE_MAX) { value, closing ->
                        if (closing) dataCenter.ttsPreferences.speechRate = value
                        value
                    }
                }
            },
            TTSSetting(R.string.auto_read_next_chapter, R.string.auto_read_next_chapter_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.readAloudNextChapter) { _, value -> dataCenter.readAloudNextChapter = value }
            },
            TTSSetting(R.string.tts_move_bookmark, R.string.tts_move_bookmark_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.moveBookmark) { _, value -> dataCenter.ttsPreferences.moveBookmark = value }
            },
            TTSSetting(R.string.tts_mark_chapters_read, R.string.tts_mark_chapters_read_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.markChaptersRead) { _, value -> dataCenter.ttsPreferences.markChaptersRead = value }
            },
            TTSSetting(R.string.tts_language, R.string.tts_language_description).bindChevron { _, _ ->
                selectLanguage()
            },
            TTSSetting(R.string.tts_downpitch_dialogue, R.string.tts_downpitch_dialogue_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.downpitchDialogue) { _, value -> dataCenter.ttsPreferences.downpitchDialogue = value }
            },
            TTSSetting(R.string.tts_use_legacy_player, R.string.tts_use_legacy_player_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.useLegacyPlayer) { _, value -> dataCenter.ttsPreferences.useLegacyPlayer = value }
            },
            TTSSetting(R.string.tts_chapter_change_sfx, R.string.tts_chapter_change_sfx_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.chapterChangeSFX) { _, value -> dataCenter.ttsPreferences.chapterChangeSFX = value }
            },
            TTSSetting(R.string.tts_announce_last_chapter, R.string.tts_announce_last_chapter_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.announceFinalChapter) { _, value -> dataCenter.ttsPreferences.announceFinalChapter = value }
            },

            TTSSetting(R.string.tts_header_processing, R.string.empty).bindHeader(),
            TTSSetting(R.string.tts_merge_buffer_chapters, R.string.tts_merge_buffer_chapters_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.mergeBufferChapters) { _, value -> dataCenter.ttsPreferences.mergeBufferChapters = value }
            },
            TTSSetting(R.string.tts_discard_first_page, R.string.tts_discard_first_page_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.discardInitialBufferPage) { _, value -> dataCenter.ttsPreferences.discardInitialBufferPage = value }
            },
            TTSSetting(R.string.tts_use_longest_page, R.string.tts_use_longest_page_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.useLongestPage) { _, value -> dataCenter.ttsPreferences.useLongestPage = value }
            },
            TTSSetting(R.string.tts_strip_header, R.string.tts_strip_header_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.stripHeader) { _, value -> dataCenter.ttsPreferences.stripHeader = value }
            },
            TTSSetting(R.string.tts_header_filters, R.string.empty).bindHeader(),
            TTSSetting(R.string.tts_filters, R.string.tts_filters_description).bindChevron { _, _ ->
                selectFilters()
            },
            TTSSetting(R.string.tts_update_filters, R.string.tts_update_filters_description).bindChevron { _, _ ->
                updateFilterSources()
            },
            TTSSetting(R.string.tts_header_remote, R.string.empty).bindHeader(),
            TTSSetting(R.string.tts_swap_rewind_skip, R.string.tts_swap_rewind_skip_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.swapRewindSkip) { _, value -> dataCenter.ttsPreferences.swapRewindSkip = value }
            },
            TTSSetting(R.string.tts_rewind_amount, R.string.tts_rewind_amount_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                @SuppressLint("SetTextI18n")
                view.currentValue.text = dataCenter.ttsPreferences.rewindSentences.toString()
                view.root.setOnClickListener {
                    sliderMenu(view.currentValue, dataCenter.ttsPreferences.rewindSentences.toFloat(), 1f, 20f, false) { value, closing ->
                        if (closing) dataCenter.ttsPreferences.rewindSentences = value.roundToInt()
                        round(value)
                    }
                }
            },
            TTSSetting(R.string.tts_forward_amount, R.string.tts_forward_amount_description).onBind { _, view, _ ->
                view.currentValue.visibility = View.VISIBLE
                @SuppressLint("SetTextI18n")
                view.currentValue.text = dataCenter.ttsPreferences.forwardSentences.toString()
                view.root.setOnClickListener {
                    sliderMenu(view.currentValue, dataCenter.ttsPreferences.forwardSentences.toFloat(), 1f, 20f, false) { value, closing ->
                        if (closing) dataCenter.ttsPreferences.forwardSentences = value.roundToInt()
                        round(value)
                    }
                }
            },
            // TODO: Separate setting to swap < > buttons on notification
            // TODO: Rewind/forward distance
            TTSSetting(R.string.tts_rewind_to_skip, R.string.tts_rewind_to_skip_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.rewindToSkip) { _, value -> dataCenter.ttsPreferences.rewindToSkip = value }
            },

            TTSSetting(R.string.tts_header_misc, R.string.empty).bindHeader(),
            TTSSetting(R.string.tts_keep_screen_on, R.string.tts_keep_screen_on_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.keepScreenOn) { _, value -> dataCenter.ttsPreferences.keepScreenOn = value }
            },
            TTSSetting(R.string.tts_stop_on_error, R.string.tts_stop_on_error_description).onBind { _, view, _ ->
                view.bindSwitch(dataCenter.ttsPreferences.stopOnLoadError) { _, value -> dataCenter.ttsPreferences.stopOnLoadError = value }
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
            var language: Locale? = dataCenter.ttsPreferences.language

            val dialog = MaterialDialog(this).show {
                title(R.string.tts_language)
                customView(R.layout.dialog_list, scrollable = true)
                positiveButton(R.string.okay) {
                    dataCenter.ttsPreferences.language = language
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
        val originalFilters = dataCenter.ttsPreferences.filters
        val filters = originalFilters.toMutableList()

        val dialog = MaterialDialog(this).show {
            title(R.string.tts_filters)
            customView(R.layout.dialog_checkbox_list, scrollable = true)
            positiveButton(R.string.okay) {
                if (originalFilters.size != filters.size || !originalFilters.containsAll(filters)) {
                    dataCenter.ttsPreferences.filters = filters
                    val cache = dataCenter.ttsPreferences.filterCache
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

        val filterCache = dataCenter.ttsPreferences.filterCache
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
                val cache = dataCenter.ttsPreferences.filterCache.toMutableMap()
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
                dataCenter.ttsPreferences.filterCache = cache
                rebuildEnabledFilters(dataCenter.ttsPreferences.filters, cache)
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

        dataCenter.ttsPreferences.filterList = list
    }

    fun sliderMenu(textView: TextView, initVal:Float, min: Float, max: Float, percentage: Boolean = true, callback: (value: Float, closing: Boolean)->Float) {
        var value = initVal
        val dialog = MaterialDialog(this).show {
            title(R.string.tts_rate)
            customView(R.layout.dialog_slider_ext, scrollable = true)
            onDismiss {
                callback(value, true)
                tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            }
        }

        // 50...200 range is bugged for TwoWaySeekBar as it's not including 0, so a crutch to offset it by -50
        val sliderValue = dialog.getCustomView().findViewById<TextView>(R.id.sliderValue) ?: return
        sliderValue.text = textView.text

        val seekBar = dialog.getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.setAbsoluteMinMaxValue(0.0, (max - min).toDouble())
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = if (percentage) (round(progress.toFloat() * 100.0f) / 100.0f) + min else progress.toFloat() + min
            value = callback(value, false)
            tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            @SuppressLint("SetTextI18n")
            if (percentage)
                textView.text = value.toHumanPercentage().roundToInt().toString() + "%"
            else
                textView.text = value.roundToInt().toString()
            sliderValue.text = textView.text
        }
        seekBar.setProgress(value.toDouble() - min)
        val numberInput = dialog.getCustomView().findViewById<EditText>(R.id.seekBarCustomNumber) ?: return


        val numberChange = object : TextView.OnEditorActionListener, View.OnFocusChangeListener {
            override fun onEditorAction(view: TextView?, i: Int, keyEvent: KeyEvent?): Boolean {
                return if (i == EditorInfo.IME_ACTION_SEARCH || i == EditorInfo.IME_ACTION_DONE || keyEvent?.keyCode == KeyEvent.KEYCODE_ENTER) {
                    applyTextInput()
                    true
                } else false
            }

            override fun onFocusChange(p0: View?, p1: Boolean) {
                applyTextInput()
            }

            fun applyTextInput() {
                numberInput.text?.toString()?.toFloatOrNull()?.let {
                    value = if (percentage) it.fromHumanPercentage() else it
                    if (value < min) value = min
                    value = callback(value, false)
                    @SuppressLint("SetTextI18n")
                    if (percentage)
                        textView.text = value.toHumanPercentage().roundToInt().toString() + "%"
                    else
                        textView.text = value.roundToInt().toString()
                    sliderValue.text = textView.text
                    tts?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
                }
            }
        }

        numberInput.onFocusChangeListener = numberChange
        numberInput.setOnEditorActionListener(numberChange)

        dialog.getCustomView().findViewById<ImageButton>(R.id.toggleModeButton)?.let {
            it.setOnClickListener {
                if (numberInput.visibility == View.GONE) {
                    numberInput.visibility = View.VISIBLE
                    seekBar.visibility = View.GONE
                    sliderValue.visibility = View.GONE
                    numberInput.setText(value.toHumanPercentage().roundToInt().toString())
                } else {
                    numberInput.visibility = View.GONE
                    seekBar.visibility = View.VISIBLE
                    sliderValue.visibility = View.VISIBLE
                    seekBar.setProgress(value.toDouble() - min)
                }
            }
        }
    }
}