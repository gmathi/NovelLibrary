package io.github.gmathi.novellibrary.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import io.github.gmathi.novellibrary.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records the last uncaught exception to a file so it can be shown (and copied) on the next
 * launch. Test builds have no working crash reporting service, and testers otherwise have no
 * way to retrieve a stack trace from the phone. The previous handler still runs afterwards, so
 * the process terminates exactly as before.
 */
object CrashLogger {

    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                File(appContext.filesDir, FILE_NAME).writeText(format(thread, throwable))
            } catch (_: Exception) {
                // Never let crash logging itself interfere with crash handling.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** Returns and clears the last recorded crash report, or null if there is none. */
    fun consumeLastReport(context: Context): String? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        val text = try { file.readText() } catch (_: Exception) { null }
        file.delete()
        return text?.takeIf { it.isNotBlank() }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("NovelLibrary crash report", text))
    }

    private fun format(thread: Thread, throwable: Throwable): String {
        val trace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            append("NovelLibrary ").append(BuildConfig.VERSION_NAME).append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
            append("Android ").append(Build.VERSION.RELEASE).append(" / ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append(time).append(" on thread ").append(thread.name).append("\n\n")
            append(trace)
        }
    }
}
