package io.github.gmathi.novellibrary.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.inputmethod.InputMethodManager
import io.github.gmathi.novellibrary.activity.ReaderPagerActivity
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants


fun Fragment.hideSoftKeyboard() {
    val inputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(activity?.window?.decorView?.windowToken, 0)
}

fun Fragment.startReaderPagerActivity(novel: Novel, webPage: WebPage, chapters: ArrayList<WebPage>?) {
    val intent = Intent(activity, ReaderPagerActivity::class.java)
    val bundle = Bundle()
    bundle.putSerializable("novel", novel)
    bundle.putSerializable("webPage", webPage)
    bundle.putSerializable("chapters", chapters)
    intent.putExtras(bundle)
    startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
}

fun Fragment.isFragmentActive(): Boolean {
    return activity != null && isResumed && !isRemoving && !isDetached
}

