package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.service.download.DownloadService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.TransitionHelper
import io.github.gmathi.novellibrary.util.Utils

fun Activity.startNavDrawerActivity() {
    val intent = Intent(this, NavDrawerActivity::class.java)
    startActivity(intent)
}

fun Activity.startMainActivity() {
    val intent = Intent(this, MainActivity::class.java)
    startActivityForResult(intent, Constants.IWV_ACT_REQ_CODE)
}

fun Activity.startImagePreviewActivity(url: String?, filePath: String?, view: View) {
    val intent = Intent(this, ImagePreviewActivity::class.java)
    intent.putExtra("url", url)
    intent.putExtra("filePath", filePath)
    TransitionHelper.startSharedImageTransition(this, view, getString(R.string.transition_image_preview), intent)
}

fun Activity.hideSoftKeyboard() {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun Activity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.snackBar(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        .setAction("Action", null).show()
}

fun Activity.startChaptersActivity(novel: Novel, jumpToReader: Boolean = false) {
    val intent = Intent(this, ChaptersActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    if (jumpToReader)
        bundle.putBoolean(Constants.JUMP, true)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.CHAPTER_ACT_REQ_CODE)
}

fun Activity.startMetadataActivity(novel: Novel) {
    val intent = Intent(this, MetaDataActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.METADATA_ACT_REQ_CODE)
}

fun Activity.startWebViewActivity(url: String) {
    val intent = Intent(this, WebViewActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("url", url)
    intent.putExtras(bundle)
    startActivity(intent)
}

fun Activity.startReaderDBPagerActivity(novel: Novel) {
    val intent = Intent(this, ReaderDBPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}


fun Activity.startSearchResultsActivity(title: String, url: String) {
    val intent = Intent(this, SearchUrlActivity::class.java)
    val bundle = Bundle()
    bundle.putString("title", title)
    bundle.putString("url", url)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.SEARCH_RESULTS_ACT_REQ_CODE)
}

fun Activity.startRecentlyViewedNovelsActivity() {
    startActivityForResult(Intent(this, RecentlyViewedNovelsActivity::class.java), Constants.RECENT_VIEWED_ACT_REQ_CODE)
}

fun Activity.startRecentlyUpdatedNovelsActivity() {
    startActivityForResult(Intent(this, RecentlyUpdatedNovelsActivity::class.java), Constants.RECENT_UPDATED_ACT_REQ_CODE)
}

fun Activity.startSettingsActivity() {
    startActivityForResult(Intent(this, SettingsActivity::class.java), Constants.SETTINGS_ACT_REQ_CODE)
}

fun Activity.startLanguagesActivity() {
    startActivityForResult(Intent(this, LanguageActivity::class.java), Constants.LANG_ACT_REQ_CODE)
}

fun Activity.startGeneralSettingsActivity() {
    val intent = Intent(this, GeneralSettingsActivity::class.java)
    startActivity(intent)
}

fun Activity.startBackupSettingsActivity() {
    val intent = Intent(this, BackupSettingsActivity::class.java)
    startActivity(intent)
}

fun Activity.startReaderSettingsActivity() {
    val intent = Intent(this, ReaderSettingsActivity::class.java)
    startActivity(intent)
}


fun Activity.startMentionSettingsActivity() {
    val intent = Intent(this, MentionSettingsActivity::class.java)
    startActivity(intent)
}

fun Activity.startCopyrightActivity() {
    val intent = Intent(this, CopyrightActivity::class.java)
    startActivity(intent)
}

fun Activity.startLibrariesUsedActivity() {
    val intent = Intent(this, LibrariesUsedActivity::class.java)
    startActivity(intent)
}

fun Activity.startContributionsActivity() {
    val intent = Intent(this, ContributionsActivity::class.java)
    startActivity(intent)
}

fun Activity.startImportLibraryActivity() {
    startActivityForResult(Intent(this, ImportLibraryActivity::class.java), Constants.IMPORT_LIBRARY_ACT_REQ_CODE)
}

fun Activity.startNovelDownloadsActivity() {
    startActivityForResult(Intent(this, NovelDownloadsActivity::class.java), Constants.SETTINGS_ACT_REQ_CODE)
}

fun Activity.startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean = false) {
    val intent = Intent(this, NovelDetailsActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    if (jumpToReader)
        bundle.putBoolean(Constants.JUMP, true)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
}


fun Activity.openInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(packageManager) != null)
        startActivity(intent)
}

fun Activity.sendEmail(email: String, subject: String, body: String) {
    val mailTo = "mailto:" + email +
        "?&subject=" + Uri.encode(subject) +
        "&body=" + Uri.encode(body + Utils.getDeviceInfo())
    val emailIntent = Intent(Intent.ACTION_VIEW)
    emailIntent.data = Uri.parse(mailTo)

    if (emailIntent.resolveActivity(packageManager) != null)
        startActivity(emailIntent)
}

fun Activity.shareUrl(url: String) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
    i.putExtra(Intent.EXTRA_TEXT, url)
    if (i.resolveActivity(packageManager) != null)
        startActivity(Intent.createChooser(i, "Share URL(s)"))
}

fun Activity.startDownloadService() {
    val serviceIntent = Intent(this, DownloadService::class.java)
    val bundle = Bundle()
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

fun Activity.startDownloadNovelService(novelName: String) {
    val serviceIntent = Intent(this, DownloadNovelService::class.java)
    val bundle = Bundle()
    bundle.putString(DownloadNovelService.NOVEL_NAME, novelName)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}

