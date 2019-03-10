package io.github.gmathi.novellibrary.fragment

import android.support.v7.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.inputmethod.InputMethodManager
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.activity.WebViewActivity
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.Constants


fun Fragment.hideSoftKeyboard() {
    val inputMethodManager = activity?.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(activity?.window?.decorView?.windowToken, 0)
}

fun Fragment.isFragmentActive(): Boolean {
    return activity != null && isResumed && !isRemoving && !isDetached
}

fun Fragment.startReaderDBPagerActivity(novel: Novel) {
    val intent = Intent(context, ReaderDBPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}

fun Fragment.startReaderDBPagerActivity(novel: Novel, sourceId: Long) {
    val intent = Intent(context, ReaderDBPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    bundle.putLong("sourceId", sourceId)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}

fun Fragment.startWebViewActivity(url: String) {
    val intent = Intent(context, WebViewActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("url", url)
    intent.putExtras(bundle)
    startActivity(intent)
}

fun Fragment.shareUrl(url: String) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
    i.putExtra(Intent.EXTRA_TEXT, url)
    if (i.resolveActivity(activity?.packageManager) != null)
        startActivity(Intent.createChooser(i, "Share URL(s)"))
}


