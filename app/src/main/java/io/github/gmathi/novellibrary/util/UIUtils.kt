package io.github.gmathi.novellibrary.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.system.getResourceColor

/**
 * Common UI utilities to reduce code duplication across the app
 */
object UIUtils {

    /**
     * Shows a toast message
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    /**
     * Shows a toast message with string resource
     */
    fun showToast(context: Context, @StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, messageRes, duration).show()
    }

    /**
     * Opens a URL in a custom tab or fallback browser
     */
    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = CustomTabsIntent.Builder()
                .setToolbarColor(context.getResourceColor(R.attr.colorPrimary))
                .build()
            intent.launchUrl(context, url.toUri())
        } catch (e: Exception) {
            // Fallback to regular browser
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                showToast(context, "No browser application found")
            }
        }
    }

    /**
     * Shows a confirmation dialog
     */
    fun showConfirmDialog(
        activity: AppCompatActivity,
        title: String? = null,
        message: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_warning_white_vector,
        onConfirm: () -> Unit = {}
    ) {
        MaterialDialog(activity).show {
            if (title != null) {
                title(text = title)
            } else {
                title(R.string.confirm_action)
            }
            message(text = message)
            icon(iconRes)
            positiveButton(R.string.okay) { onConfirm() }
        }
    }

    /**
     * Shows an alert dialog
     */
    fun showAlertDialog(
        activity: AppCompatActivity,
        title: String? = null,
        message: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_warning_white_vector
    ) {
        if (title.isNullOrBlank() && message.isNullOrBlank()) return
        
        MaterialDialog(activity).show {
            icon(iconRes)
            title?.let { title(text = it) }
            message?.let { message(text = it) }
            positiveButton(R.string.okay)
        }
    }

    /**
     * Shows an alert dialog from a fragment
     */
    fun showAlertDialog(
        fragment: Fragment,
        title: String? = null,
        message: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_warning_white_vector
    ) {
        fragment.requireActivity().let { activity ->
            showAlertDialog(activity as AppCompatActivity, title, message, iconRes)
        }
    }
} 