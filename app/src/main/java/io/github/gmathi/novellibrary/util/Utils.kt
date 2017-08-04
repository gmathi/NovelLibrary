package io.github.gmathi.novellibrary.util


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import java.io.ByteArrayOutputStream
import java.io.File



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

    fun deleteNovel(context: Context, novel: Novel?) {
        if (novel == null) return
        val hostDir = getHostDir(context, novel.url!!)
        val novelDir = getNovelDir(hostDir, novel.name!!)
        novelDir.deleteRecursively()
        dbHelper.cleanupNovelData(novel.id)
        broadcastNovelDelete(context, novel)
    }

    fun broadcastNovelDelete(context: Context, novel: Novel?) {
        val localIntent = Intent()
        val extras = Bundle()
        extras.putLong(Constants.NOVEL_ID, novel!!.id)
        localIntent.action = Constants.NOVEL_DELETED
        localIntent.putExtras(extras)
        localIntent.addCategory(Intent.CATEGORY_DEFAULT)
        context.sendBroadcast(localIntent)
    }

    fun checkNetwork(context: Context): Boolean {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val netInfo = connectivityManager?.activeNetworkInfo
        return netInfo != null && netInfo.isConnected
    }
}
