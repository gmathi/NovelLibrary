package io.github.gmathi.novellibrary.util.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.*
import io.github.gmathi.novellibrary.activity.settings.*
import io.github.gmathi.novellibrary.activity.settings.reader.ScrollBehaviourSettingsActivity
import io.github.gmathi.novellibrary.activity.settings.reader.ReaderBackgroundSettingsActivity
import io.github.gmathi.novellibrary.activity.settings.reader.ReaderSettingsActivity
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.view.TransitionHelper

//#region Helpers

inline fun <reified T : Activity> Activity.startActivity() =
    startActivity(Intent(this, T::class.java))

inline fun <reified T : Activity> Activity.startActivity(bundle: Bundle) =
    startActivity(Intent(this, T::class.java).putExtras(bundle))

inline fun <reified T : Activity> Activity.startActivityForResult(requestCode: Int) =
    startActivityForResult(Intent(this, T::class.java), requestCode)

inline fun <reified T : Activity> Activity.startActivityForResult(bundle: Bundle, requestCode: Int) =
    startActivityForResult(Intent(this, T::class.java).putExtras(bundle), requestCode)

inline fun <reified T : Activity> Fragment.startActivity() =
    startActivity(Intent(context, T::class.java))

inline fun <reified T : Activity> Fragment.startActivity(bundle: Bundle) =
    startActivity(Intent(context, T::class.java).putExtras(bundle))

inline fun <reified T : Activity> Fragment.startActivityForResult(requestCode: Int) =
    startActivityForResult(Intent(context, T::class.java), requestCode)

inline fun <reified T : Activity> Fragment.startActivityForResult(bundle: Bundle, requestCode: Int) =
    startActivityForResult(Intent(context, T::class.java).putExtras(bundle), requestCode)

inline fun <reified T : Activity> Context?.intentOf(): Intent = Intent(this, T::class.java)
inline fun <reified T : Activity> Context?.intentOf(bundle: Bundle): Intent = Intent(this, T::class.java).putExtras(bundle)

//#endregion

//#region Bundle creators

private fun novelBundle(novel: Novel): Bundle {
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    return bundle
}

private fun novelBundle(novel: Novel, translatorSourceName: String?): Bundle {
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    if (translatorSourceName != null)
        bundle.putString("translatorSourceName", translatorSourceName)
    return bundle
}

private fun novelBundle(novel: Novel, jumpToReader: Boolean): Bundle {
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    if (jumpToReader)
        bundle.putBoolean(Constants.JUMP, jumpToReader)
    return bundle
}

private fun urlBundle(url: String): Bundle {
    val bundle = Bundle()
    bundle.putString("url", url)
    return bundle
}

//#endregion

//#region Generic

fun AppCompatActivity.startNavDrawerActivity() = startActivity<NavDrawerActivity>()

fun AppCompatActivity.startChaptersActivity(novel: Novel, jumpToReader: Boolean = false) =
    startActivityForResult<ChaptersPagerActivity>(novelBundle(novel, jumpToReader), Constants.CHAPTER_ACT_REQ_CODE)

fun AppCompatActivity.startLibrarySearchActivity() = startActivity<LibrarySearchActivity>()

fun AppCompatActivity.startImportLibraryActivity() = startActivityForResult<ImportLibraryActivity>(Constants.IMPORT_LIBRARY_ACT_REQ_CODE)

fun AppCompatActivity.startNovelSectionsActivity() = startActivityForResult<NovelSectionsActivity>(Constants.NOVEL_SECTIONS_ACT_REQ_CODE)

//#endregion

//#region Search / Recents

fun AppCompatActivity.startSearchResultsActivity(title: String, url: String) {
    val bundle = Bundle()
    bundle.putString("title", title)
    bundle.putString("url", url)
    startActivityForResult<SearchUrlActivity>(bundle, Constants.SEARCH_RESULTS_ACT_REQ_CODE)
}

fun AppCompatActivity.startRecentNovelsPagerActivity() = startActivityForResult<RecentNovelsPagerActivity>(Constants.RECENT_NOVELS_PAGER_ACT_REQ_CODE)

//#endregion

//#region Novel info

fun AppCompatActivity.startImagePreviewActivity(url: String?, filePath: String?, view: View) {
    val intent = Intent(this, ImagePreviewActivity::class.java)
    intent.putExtra("url", url)
    intent.putExtra("filePath", filePath)
    TransitionHelper.startSharedImageTransition(this, view, getString(R.string.transition_image_preview), intent)
}

fun Activity.startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean = false) =
    startActivityForResult<NovelDetailsActivity>(novelBundle(novel, jumpToReader), Constants.NOVEL_DETAILS_REQ_CODE)

fun AppCompatActivity.startMetadataActivity(novel: Novel) = startActivityForResult<MetaDataActivity>(novelBundle(novel), Constants.METADATA_ACT_REQ_CODE)

//#endregion

//#region Settings

fun AppCompatActivity.startExtensionsPagerActivity() = startActivityForResult<ExtensionsPagerActivity>(Constants.RECENT_NOVELS_PAGER_ACT_REQ_CODE)

fun AppCompatActivity.startSettingsActivity() = startActivityForResult<MainSettingsActivity>(Constants.SETTINGS_ACT_REQ_CODE)

fun AppCompatActivity.startLanguagesActivity(changeLanguage: Boolean = false) =
    startActivityForResult(intentOf<LanguageActivity>().putExtra("changeLanguage", changeLanguage), Constants.LANG_ACT_REQ_CODE)

fun AppCompatActivity.startGeneralSettingsActivity() = startActivity<GeneralSettingsActivity>()

fun AppCompatActivity.startBackupSettingsActivity() = startActivity<BackupSettingsActivity>()

fun AppCompatActivity.startReaderSettingsActivity() = startActivityForResult<ReaderSettingsActivity>(Constants.READER_SETTINGS_ACT_REQ_CODE)

fun AppCompatActivity.startScrollBehaviourSettingsActivity() = startActivityForResult<ScrollBehaviourSettingsActivity>(Constants.READER_BACKGROUND_SETTINGS_ACT_REQ_CODE)

fun AppCompatActivity.startReaderBackgroundSettingsActivity() = startActivityForResult<ReaderBackgroundSettingsActivity>(Constants.READER_BACKGROUND_SETTINGS_ACT_REQ_CODE)

fun AppCompatActivity.startMentionSettingsActivity() = startActivity<MentionSettingsActivity>()

fun AppCompatActivity.startCopyrightActivity() = startActivity<CopyrightActivity>()

fun AppCompatActivity.startLibrariesUsedActivity() = startActivity<LibrariesUsedActivity>()

fun AppCompatActivity.startContributionsActivity() = startActivity<ContributionsActivity>()

fun AppCompatActivity.startTTSSettingsActivity() = startActivity<TTSSettingsActivity>()

//fun AppCompatActivity.startCloudFlareBypassActivity(hostName: String) {
//    val intent = Intent(this, CloudFlareBypassActivity::class.java)
//    val bundle = Bundle()
//    bundle.putString("host", hostName)
//    intent.putExtras(bundle)
//    startActivity(intent)
//}

fun AppCompatActivity.startSyncSettingsSelectionActivity() = startActivity<SyncSettingsSelectionActivity>()

fun AppCompatActivity.startSyncSettingsActivity(url: String) = startActivity<SyncSettingsActivity>(urlBundle(url))

fun AppCompatActivity.startSyncLoginActivity(url: String, lookup: String) {
    val bundle = Bundle()
    bundle.putString("url", url)
    bundle.putString("lookup", lookup)
    startActivity<SyncLoginActivity>(bundle)
}

//#endregion

//#region Reader

fun Fragment.startReaderDBPagerActivity(novel: Novel) =
    startActivityForResult<ReaderDBPagerActivity>(novelBundle(novel), Constants.READER_ACT_REQ_CODE)

fun AppCompatActivity.startReaderDBPagerActivity(novel: Novel) =
    startActivityForResult<ReaderDBPagerActivity>(novelBundle(novel), Constants.READER_ACT_REQ_CODE)

fun Fragment.startReaderDBPagerActivity(novel: Novel, translatorSourceName: String?) =
    startActivityForResult<ReaderDBPagerActivity>(novelBundle(novel, translatorSourceName), Constants.READER_ACT_REQ_CODE)

fun AppCompatActivity.startReaderDBPagerActivity(novel: Novel, translatorSourceName: String?) =
    startActivityForResult<ReaderDBPagerActivity>(novelBundle(novel, translatorSourceName), Constants.READER_ACT_REQ_CODE)

fun Fragment.startWebViewActivity(url: String) =
    startActivity<WebViewActivity>(urlBundle(url))

fun AppCompatActivity.startWebViewActivity(url: String) =
    startActivity<WebViewActivity>(urlBundle(url))

//#endregion

//#region TTS

fun AppCompatActivity.startTTSService(audioText: String, linkedPages: ArrayList<LinkedPage>, title: String,
                                      novelId: Long, translatorSourceName: String?, chapterIndex: Int = 0) {
    val serviceIntent = Intent(this, TTSService::class.java)
    serviceIntent.action = TTSService.ACTION_STARTUP
    val bundle = Bundle()
    bundle.putString(TTSService.AUDIO_TEXT_KEY, audioText)
    bundle.putString(TTSService.TITLE, title)
    bundle.putLong(TTSService.NOVEL_ID, novelId)
    bundle.putParcelableArrayList(TTSService.LINKED_PAGES, linkedPages)
    if (translatorSourceName != null) {
        bundle.putString(TTSService.TRANSLATOR_SOURCE_NAME, translatorSourceName)
    }
    bundle.putInt(TTSService.CHAPTER_INDEX, chapterIndex)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}
fun AppCompatActivity.startTTSService(novelId: Long, translatorSourceName: String?, chapterIndex: Int) {
    val serviceIntent = Intent(this, TTSService::class.java)
    serviceIntent.action = TTSService.ACTION_STARTUP
    val bundle = Bundle()
    bundle.putLong(TTSService.NOVEL_ID, novelId)
    if (translatorSourceName != null) {
        bundle.putString(TTSService.TRANSLATOR_SOURCE_NAME, translatorSourceName)
    }
    bundle.putInt(TTSService.CHAPTER_INDEX, chapterIndex)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

fun AppCompatActivity.startTTSActivity() = startActivity<TextToSpeechControlsActivity>()

//#endregion

//#region Downloads

fun AppCompatActivity.startDownloadNovelService(novelId: Long) {
    val serviceIntent = Intent(this, DownloadNovelService::class.java)
    val bundle = Bundle()
    bundle.putLong(DownloadNovelService.NOVEL_ID, novelId)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

fun AppCompatActivity.startNovelDownloadsActivity() = startActivityForResult<NovelDownloadsActivity>(Constants.SETTINGS_ACT_REQ_CODE)

//#endregion