package io.github.gmathi.novellibrary.fragment

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
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class TTSSettingsFragment : BaseSettingsFragment<TTSSettingsFragment, TTSSetting>(OPTIONS) {

    companion object {
        private val OPTIONS = listOf(
            TTSSetting(R.string.tts_header_playback, R.string.empty).bindHeader(),
            TTSSetting(R.string.auto_read_next_chapter, R.string.auto_read_next_chapter_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.readAloudNextChapter) { _, value -> fragment.dataCenter.readAloudNextChapter = value }
            },
            TTSSetting(R.string.tts_move_bookmark, R.string.tts_move_bookmark_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.moveBookmark) { _, value -> fragment.dataCenter.ttsPreferences.moveBookmark = value }
            },
            TTSSetting(R.string.tts_mark_chapters_read, R.string.tts_mark_chapters_read_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.markChaptersRead) { _, value -> fragment.dataCenter.ttsPreferences.markChaptersRead = value }
            },
            TTSSetting(R.string.tts_language, R.string.tts_language_description).bindChevron { fragment, _ ->
                fragment.selectLanguage()
            },
            TTSSetting(R.string.tts_downpitch_dialogue, R.string.tts_downpitch_dialogue_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.downpitchDialogue) { _, value -> fragment.dataCenter.ttsPreferences.downpitchDialogue = value }
            },
            TTSSetting(R.string.tts_use_legacy_player, R.string.tts_use_legacy_player_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.useLegacyPlayer) { _, value -> fragment.dataCenter.ttsPreferences.useLegacyPlayer = value }
            },
            TTSSetting(R.string.tts_chapter_change_sfx, R.string.tts_chapter_change_sfx_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.chapterChangeSFX) { _, value -> fragment.dataCenter.ttsPreferences.chapterChangeSFX = value }
            },
            TTSSetting(R.string.tts_announce_last_chapter, R.string.tts_announce_last_chapter_description).onBind { fragment, view, _ ->
                view.bindSwitch(fragment.dataCenter.ttsPreferences.announceFinalChapter) { _, value -> fragment.dataCenter.ttsPreferences.announceFinalChapter = value }
            },
        )

        val filterSources: List<TTSFilterSource> = listOf(
            TTSFilterSource("naggers", "1.0.0-beta", "Crawler naggers",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/naggers.json"),
            TTSFilterSource("tts-adjust", "1.0.0-beta", "TTS pronunciation fixes",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/tts-adjust.json"),
            TTSFilterSource("scene-changers", "1.0.0-beta", "Scene change sound effect",
                "https://gist.github.com/Yanrishatum/4aa3f71c6307171bd18154aa5c2eca8d/raw/scene-changers.json")
        )
    }

    private var tts: MediaControllerCompat? = null
    private lateinit var browser: MediaBrowserCompat
    private val connCallback = object : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()
            browser.sessionToken.also {
                tts = MediaControllerCompat(this@TTSSettingsFragment.requireContext(), it)
            }
        }

        override fun onConnectionSuspended() {
            tts = null
        }
    }

    override fun getLayoutId() = R.layout.fragment_tts_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        browser = MediaBrowserCompat(requireContext(), ComponentName(requireContext(), TTSService::class.java), connCallback, null)
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
        engine = TextToSpeech(requireContext()) {
            var language: Locale? = dataCenter.ttsPreferences.language

            val dialog = MaterialDialog(requireContext()).show {
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
                val button = RadioButton(requireContext())
                button.id = View.generateViewId()
                button.text = getString(R.string.tts_language_default)
                button.isChecked = language == null
                group.addView(button)
            }

            val buttons = engine.availableLanguages.sortedBy {
                it.getDisplayName(Locale.getDefault())
            }.map {
                val button = RadioButton(requireContext())
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
}

private typealias TTSSetting = ListitemSetting<TTSSettingsFragment>