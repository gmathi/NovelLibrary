package io.github.gmathi.novellibrary.util.system

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.Utils

fun AppCompatActivity.hideSoftKeyboard() {
    val inputMethodManager = getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun AppCompatActivity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun AppCompatActivity.snackBar(view: View, message: String) {
    com.google.android.material.snackbar.Snackbar.make(view, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
        .setAction("Action", null).show()
}

fun AppCompatActivity.openInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        startActivity(intent)
    } catch (ex: ActivityNotFoundException) {
        this.toast("No Application Found!")
    }
}

fun AppCompatActivity.sendEmail(email: String, subject: String, body: String) {
    val mailTo = "mailto:" + email +
            "?&subject=" + Uri.encode(subject) +
            "&body=" + Uri.encode(body + Utils.getDeviceInfo())
    val emailIntent = Intent(Intent.ACTION_VIEW)
    emailIntent.data = Uri.parse(mailTo)

    try {
        startActivity(emailIntent)
    } catch (ex: ActivityNotFoundException) {
        this.toast("No Application Found!")
    }
}

fun AppCompatActivity.shareUrl(url: String) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
    i.putExtra(Intent.EXTRA_TEXT, url)
    try {
        startActivity(Intent.createChooser(i, "Share URL(s)"))
    } catch (ex: ActivityNotFoundException) {
        this.toast("No Application Found!")
    }
}

fun Activity.showAlertDialog(title: String? = null, message: String? = null, @DrawableRes icon: Int = R.drawable.ic_warning_white_vector) {
    if (title.isNullOrBlank() && message.isNullOrBlank()) return
    val builder = MaterialDialog(this).show {
        icon(icon)
        title?.let { title(text = it) }
        message?.let { message(text = it) }
        positiveButton(R.string.okay)
    }
}

/**
 * Checks whether if the device has a display cutout (i.e. notch, camera cutout, etc.).
 *
 * Only works in Android 9+.
 */
fun Activity.hasDisplayCutout(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            window.decorView.rootWindowInsets?.displayCutout != null
}

