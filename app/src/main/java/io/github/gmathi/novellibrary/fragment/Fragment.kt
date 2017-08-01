package io.github.gmathi.novellibrary.fragment

import android.app.Activity
import android.support.v4.app.Fragment
import android.view.inputmethod.InputMethodManager


fun Fragment.hideSoftKeyboard() {
    val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
}

