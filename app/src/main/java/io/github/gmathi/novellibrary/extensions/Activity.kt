package io.github.gmathi.novellibrary.extensions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tapadoo.alerter.Alerter
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.*
import io.github.gmathi.novellibrary.activity.settings.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.TransitionHelper
import io.github.gmathi.novellibrary.util.Utils

fun AppCompatActivity.startNavDrawerActivity() {
    val intent = Intent(this, NavDrawerActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startImagePreviewActivity(url: String?, filePath: String?, view: View) {
    val intent = Intent(this, ImagePreviewActivity::class.java)
    intent.putExtra("url", url)
    intent.putExtra("filePath", filePath)
    TransitionHelper.startSharedImageTransition(this, view, getString(R.string.transition_image_preview), intent)
}

fun AppCompatActivity.hideSoftKeyboard() {
    val inputMethodManager = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun AppCompatActivity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.snackBar(view: View, message: String) {
    com.google.android.material.snackbar.Snackbar.make(view, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .setAction("Action", null).show()
}

fun AppCompatActivity.startChaptersActivity(novel: Novel, jumpToReader: Boolean = false) {
    val intent = Intent(this, ChaptersPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    if (jumpToReader)
        bundle.putBoolean(Constants.JUMP, true)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.CHAPTER_ACT_REQ_CODE)
}

//fun Activity.startOldChaptersActivity(novel: Novel, jumpToReader: Boolean = false) {
//    val intent = Intent(this, OldChaptersActivity::class.java)
//    val bundle = Bundle()
//    bundle.putParcelable("novel", novel)
//    if (jumpToReader)
//        bundle.putBoolean(Constants.JUMP, true)
//    intent.putExtras(bundle)
//    startActivityForResult(intent, Constants.CHAPTER_ACT_REQ_CODE)
//}

fun AppCompatActivity.startMetadataActivity(novel: Novel) {
    val intent = Intent(this, MetaDataActivity::class.java)
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.METADATA_ACT_REQ_CODE)
}

fun AppCompatActivity.startWebViewActivity(url: String) {
    val intent = Intent(this, WebViewActivity::class.java)
    val bundle = Bundle()
    bundle.putString("url", url)
    intent.putExtras(bundle)
    startActivity(intent)
}

fun AppCompatActivity.startReaderDBPagerActivity(novel: Novel) {
    val intent = Intent(this, ReaderDBPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}


fun AppCompatActivity.startSearchResultsActivity(title: String, url: String) {
    val intent = Intent(this, SearchUrlActivity::class.java)
    val bundle = Bundle()
    bundle.putString("title", title)
    bundle.putString("url", url)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.SEARCH_RESULTS_ACT_REQ_CODE)
}

fun AppCompatActivity.startRecentlyViewedNovelsActivity() {
    startActivityForResult(Intent(this, RecentlyViewedNovelsActivity::class.java), Constants.RECENT_VIEWED_ACT_REQ_CODE)
}

fun AppCompatActivity.startRecentlyUpdatedNovelsActivity() {
    startActivityForResult(Intent(this, RecentlyUpdatedNovelsActivity::class.java), Constants.RECENT_UPDATED_ACT_REQ_CODE)
}

fun AppCompatActivity.startSettingsActivity() {
    startActivityForResult(Intent(this, SettingsActivity::class.java), Constants.SETTINGS_ACT_REQ_CODE)
}

fun AppCompatActivity.startLanguagesActivity(changeLanguage: Boolean = false) {
    startActivityForResult(Intent(this, LanguageActivity::class.java).putExtra("changeLanguage", changeLanguage), Constants.LANG_ACT_REQ_CODE)
}

fun AppCompatActivity.startGeneralSettingsActivity() {
    val intent = Intent(this, GeneralSettingsActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startBackupSettingsActivity() {
    val intent = Intent(this, BackupSettingsActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startReaderSettingsActivity() {
    val intent = Intent(this, ReaderSettingsActivity::class.java)
    startActivity(intent)
}


fun AppCompatActivity.startMentionSettingsActivity() {
    val intent = Intent(this, MentionSettingsActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startCopyrightActivity() {
    val intent = Intent(this, CopyrightActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startLibrariesUsedActivity() {
    val intent = Intent(this, LibrariesUsedActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startContributionsActivity() {
    val intent = Intent(this, ContributionsActivity::class.java)
    startActivity(intent)
}

fun AppCompatActivity.startImportLibraryActivity() {
    startActivityForResult(Intent(this, ImportLibraryActivity::class.java), Constants.IMPORT_LIBRARY_ACT_REQ_CODE)
}

fun AppCompatActivity.startNovelSectionsActivity() {
    startActivityForResult(Intent(this, NovelSectionsActivity::class.java), Constants.NOVEL_SECTIONS_ACT_REQ_CODE)
}

fun AppCompatActivity.startNovelDownloadsActivity() {
    startActivityForResult(Intent(this, NovelDownloadsActivity::class.java), Constants.SETTINGS_ACT_REQ_CODE)
}

fun AppCompatActivity.startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean = false) {
    val intent = Intent(this, NovelDetailsActivity::class.java)
    val bundle = Bundle()
    bundle.putParcelable("novel", novel)
    if (jumpToReader)
        bundle.putBoolean(Constants.JUMP, true)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
}


fun AppCompatActivity.openInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(packageManager) != null)
        startActivity(intent)
}

fun AppCompatActivity.sendEmail(email: String, subject: String, body: String) {
    val mailTo = "mailto:" + email +
            "?&subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body + Utils.getDeviceInfo())
    val emailIntent = Intent(Intent.ACTION_VIEW)
    emailIntent.data = Uri.parse(mailTo)

    if (emailIntent.resolveActivity(packageManager) != null)
        startActivity(emailIntent)
}

fun AppCompatActivity.shareUrl(url: String) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
    i.putExtra(Intent.EXTRA_TEXT, url)
    if (i.resolveActivity(packageManager) != null)
        startActivity(Intent.createChooser(i, "Share URL(s)"))
}

fun AppCompatActivity.startDownloadNovelService(novelName: String) {
    val serviceIntent = Intent(this, DownloadNovelService::class.java)
    val bundle = Bundle()
    bundle.putString(DownloadNovelService.NOVEL_NAME, novelName)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

fun AppCompatActivity.startTTSService(audioText: String, title: String, novelId: Long) {
    val serviceIntent = Intent(this, TTSService::class.java)
    val bundle = Bundle()
    bundle.putString(TTSService.AUDIO_TEXT_KEY, audioText)
    bundle.putString(TTSService.TITLE, title)
    bundle.putLong(TTSService.NOVEL_ID, novelId)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

fun AppCompatActivity.alertToast(title: String? = null, message: String? = null, color: Int = R.color.colorTextWarningRed, duration: Long = 2500, icon: Int = R.drawable.ic_warning_white_vector) {
    if (title.isNullOrBlank() && message.isNullOrBlank()) return
    val alerter = Alerter.create(this)
    alerter.setBackgroundColorRes(color)
    if (!title.isNullOrBlank()) alerter.setTitle(title)
    if (!message.isNullOrBlank()) alerter.setText(message)
    alerter.setDuration(duration)
    alerter.setIcon(icon)
    alerter.show()
}
