package io.github.gmathi.novellibrary.util.system

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.activity.WebViewActivity
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants


fun Fragment.hideSoftKeyboard() {
    val inputMethodManager = activity?.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(activity?.window?.decorView?.windowToken, 0)
}

fun Fragment.isFragmentActive(): Boolean {
    return activity != null && isResumed && !isRemoving && !isDetached
}

fun Fragment.showAlertDialog(title: String? = null, message: String? = null, icon: Int = R.drawable.ic_warning_white_vector) {
    requireActivity().showAlertDialog(title, message, icon)
}

