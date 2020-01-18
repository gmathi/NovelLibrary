package io.github.gmathi.novellibrary.util

import android.app.ActivityManager
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import android.util.TypedValue
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


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
    fun getImage(image: ByteArray): Bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)

    //endregion


    fun getHostDir(context: Context, url: String): File {
        val uri = Uri.parse(url)
        val path = context.filesDir

        val dirName = (uri.host ?: "NoHostNameFound").writableFileName()
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

    /**
     * returns - True - if there is connection to the internet
     */
    fun isConnectedToNetwork(context: Context?): Boolean {
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
            outChannel.close()
        }
    }

    @Throws(IOException::class)
    fun copyFile(src: InputStream, dst: File) {
        val outStream = FileOutputStream(dst)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int = src.read(buffer)
        while (bytesRead != -1) {
            outStream.write(buffer, 0, bytesRead)
            bytesRead = src.read(buffer)
        }
        src.close()
        outStream.close()
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
            for (i in fileList!!.indices) {
                // Recursive call if it's a directory
                result += if (fileList[i].isDirectory) {
                    getFolderSize(fileList[i])
                } else {
                    // Sum the file size in bytes
                    fileList[i].length()
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

    fun dialogBuilder(activity: AppCompatActivity, title: String? = null, content: String? = null, iconRes: Int = R.drawable.ic_warning_white_vector, isProgress: Boolean = false): MaterialDialog.Builder {
        val dialogBuilder = MaterialDialog.Builder(activity)

        if (title != null)
            dialogBuilder.title(activity.getString(R.string.confirm_action))

        if (isProgress)
            dialogBuilder.progress(true, 100)

        if (content != null)
            dialogBuilder.content(content)

        dialogBuilder
                .iconRes(iconRes)

        if (!isProgress)
            dialogBuilder.positiveText(activity.getString(R.string.okay)).onPositive { dialog, _ -> dialog.dismiss() }

        return dialogBuilder
    }

    fun getCurrentFormattedDate() = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())

    fun getBitmapFromDrawable(context: Context, @DrawableRes drawableId: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableId)

        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else if (drawable is VectorDrawableCompat || drawable is VectorDrawable) {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            bitmap
        } else {
            throw IllegalArgumentException("unsupported drawable type")
        }
    }

}
