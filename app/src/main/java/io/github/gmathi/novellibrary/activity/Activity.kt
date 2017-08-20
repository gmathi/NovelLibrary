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
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.service.DownloadChapterService
import io.github.gmathi.novellibrary.service.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.TransitionHelper


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

fun Activity.startChaptersActivity(novel: Novel, jumpToReader: Boolean) {
    val intent = Intent(this, ChaptersUltimateActivity::class.java)
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

fun Activity.startReaderPagerActivity(novel: Novel, webPage: WebPage, chapters: ArrayList<WebPage>?) {
    val intent = Intent(this, ReaderPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    bundle.putSerializable("webPage", webPage)
    bundle.putSerializable("chapters", chapters)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}

fun Activity.startReaderPagerDBActivity(novel: Novel) {
    val intent = Intent(this, ReaderPagerDBActivity::class.java)
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

fun Activity.startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean) {
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
        "&body=" + Uri.encode(body)
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

fun Activity.startNovelDownloadService(novelId: Long) {
    val serviceIntent = Intent(this, DownloadNovelService::class.java)
    serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
    startService(serviceIntent)
}

fun Activity.startChapterDownloadService(novel: Novel, webPages: ArrayList<WebPage>) {
    val serviceIntent = Intent(this, DownloadChapterService::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    bundle.putSerializable("webPages", webPages)
    serviceIntent.putExtras(bundle)
    startService(serviceIntent)
}


