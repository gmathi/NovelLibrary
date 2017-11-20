package io.github.gmathi.novellibrary.util

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.TypedValue
import com.hanks.library.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import java.io.*


object Utils {

    //region color utils
    fun getThemeAccentColor(context: Context): Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(
            typedValue.data,
            intArrayOf(R.attr.colorAccent)
        )
        val color = a.getColor(0, Color.CYAN)
        a.recycle()

        return color
    }

    fun getThemePrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        val a = context.obtainStyledAttributes(
            typedValue.data,
            intArrayOf(R.attr.colorPrimary)
        )
        val color = a.getColor(0, Color.BLUE)
        a.recycle()

        return color
    }

    fun lighten(color: Int, factor: Float): Int {
        val red = ((Color.red(color) * (1 - factor) / 255 + factor) * 255).toInt()
        val green = ((Color.green(color) * (1 - factor) / 255 + factor) * 255).toInt()
        val blue = ((Color.blue(color) * (1 - factor) / 255 + factor) * 255).toInt()
        return Color.argb(Color.alpha(color), red, green, blue)
    }
    //endregion

    //region Bitmap to byte[] Conversions

    fun getBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream)
        return stream.toByteArray()
    }

    // convert from byte array to bitmap
    fun getImage(image: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(image, 0, image.size)
    }

    //endregion

    //region UtilLogs
    fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun info(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    fun warning(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    fun warning(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        }
    }

    fun error(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }


    //endregion

    fun getHostDir(context: Context, url: String): File {
        val uri = Uri.parse(url)
        val path = context.filesDir

        val dirName = uri.host.writableFileName()
        val hostDir = File(path, dirName)
        if (!hostDir.exists()) hostDir.mkdir()

        return hostDir
    }

    fun getNovelDir(hostDir: File, novelName: String): File {
        val novelDir = File(hostDir, novelName.writableFileName())
        if (!novelDir.exists()) novelDir.mkdir()
        return novelDir
    }

    fun deleteNovel(context: Context, novelId: Long) {
        deleteNovel(context, dbHelper.getNovel(novelId))
    }

    private fun deleteNovel(context: Context, novel: Novel?) {
        if (novel == null) return
        val hostDir = getHostDir(context, novel.url)
        val novelDir = getNovelDir(hostDir, novel.name)
        novelDir.deleteRecursively()
        dbHelper.cleanupNovelData(novel)
        broadcastNovelDelete(context, novel)
    }

    private fun broadcastNovelDelete(context: Context, novel: Novel?) {
        val localIntent = Intent()
        val extras = Bundle()
        extras.putLong(Constants.NOVEL_ID, novel!!.id)
        localIntent.action = Constants.NOVEL_DELETED
        localIntent.putExtras(extras)
        localIntent.addCategory(Intent.CATEGORY_DEFAULT)
        context.sendBroadcast(localIntent)
    }

    fun checkNetwork(context: Context?): Boolean {
        context?.let {
            val connectivityManager = it.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val netInfo = connectivityManager?.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }
        return false
    }

    @Throws(IOException::class)
    fun copyFile(src: File, dst: File) {
        val inChannel = FileInputStream(src).channel
        val outChannel = FileOutputStream(dst).channel
        try {
            inChannel!!.transferTo(0, inChannel.size(), outChannel)
        } finally {
            inChannel?.close()
            outChannel?.close()
        }
    }

    /**
     * Returns whether an SD card is present and writable
     */
    val isSDCardPresent: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    /**
     * Query the media store for a directory size
     *
     * @param dir
     * the directory on primary storage
     * @return the size of the directory
     */
    fun getFolderSize(dir: File): Long {
        if (dir.exists()) {
            var result: Long = 0
            val fileList = dir.listFiles()
            for (i in fileList.indices) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory) {
                    result += getFolderSize(fileList[i])
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length()
                }
            }
            return result // return the file size
        }
        return 0
    }

    fun getDeviceInfo(): String {
        val sb = StringBuilder()
        sb.append("\nAppVersion: ${BuildConfig.VERSION_NAME}")
        sb.append("\nPhone OS Name: ${Build.VERSION.RELEASE}")
        sb.append("\nPhone Version: ${Build.VERSION.SDK_INT}")
        sb.append("\nPhone Model: ${Build.DEVICE}, ${Build.MODEL}")
        return sb.toString()
    }

    @Suppress("DEPRECATION")
    fun isServiceRunning(context: Context, serviceQualifiedName: String): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { serviceQualifiedName == it.service.className }
    }


}
