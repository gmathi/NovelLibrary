package io.github.gmathi.novellibrary.util

import android.app.ActivityManager
import android.content.ContentResolver
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
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.storage.createFileIfNotExists
import io.github.gmathi.novellibrary.util.storage.getOrCreateDirectory
import io.github.gmathi.novellibrary.util.storage.getOrCreateFile
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.sync.NovelSync
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object Utils {

    private const val TAG = "UTILS"
    private const val BUFFER_SIZE = 16384

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
        NovelSync.getInstance(novel)?.applyAsync { if (dataCenter.getSyncDeleteNovels(it.host)) it.removeNovel(novel) }
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
    fun copyFile(inputStream: InputStream, dst: File) {
        inputStream.use {
            FileOutputStream(dst).use { outStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int = inputStream.read(buffer)
                while (bytesRead != -1) {
                    outStream.write(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(src: File, dst: File) {
        copyFile(src.inputStream(), dst)
    }

    @Throws(IOException::class)
    fun copyFile(contentResolver: ContentResolver, src: File, dst: DocumentFile) {
        FileInputStream(src).use { inStream ->
            contentResolver.openOutputStream(dst.uri)?.use { outStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = inStream.read(buffer)
                while (bytesRead != -1) {
                    outStream.write(buffer, 0, bytesRead)
                    bytesRead = inStream.read(buffer)
                }
            }
        }

    }

    @Throws(IOException::class)
    fun copyFile(contentResolver: ContentResolver, src: DocumentFile, dst: File) {
        contentResolver.openInputStream(src.uri)?.use { inStream ->
            FileOutputStream(dst).use { outStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead = inStream.read(buffer)
                while (bytesRead != -1) {
                    outStream.write(buffer, 0, bytesRead)
                    bytesRead = inStream.read(buffer)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun recursiveCopy(src: File, dst: File) {
        src.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val destDir = File(dst, file.name)
                if (!destDir.exists()) destDir.mkdir()
                recursiveCopy(file, destDir)
            } else {
                copyFile(file, File(dst, file.name))
            }
        }
    }

    @Throws(IOException::class)
    fun recursiveCopy(contentResolver: ContentResolver, src: File, dst: DocumentFile) {
        src.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val destDir = dst.getOrCreateDirectory(file.name)!!
                recursiveCopy(contentResolver, file, destDir)
            } else {
                copyFile(contentResolver, file, dst.getOrCreateFile(src.name)!!)
            }
        }
    }

    @Throws(IOException::class)
    fun recursiveCopy(contentResolver: ContentResolver, src: DocumentFile, dst: File) {
        src.listFiles().forEach { file ->
            val name = file.name
            if (name != null) {
                if (file.isDirectory) {
                    val destDir = File(dst, name)
                    recursiveCopy(contentResolver, file, destDir)
                } else {
                    copyFile(contentResolver, file, File(dst, name))
                }
            }
        }
    }

    @Throws(IOException::class)
    fun zip(contentResolver: ContentResolver, file: File, zip: DocumentFile, log: Boolean = false) {
        ZipOutputStream(BufferedOutputStream(contentResolver.openOutputStream(zip.uri)!!)).use {
            zip(file, it, log)
        }
    }

    @Throws(IOException::class)
    fun zip(file: File, outStream: ZipOutputStream, log: Boolean = false) {
        val basePathLength = (file.parent?.length ?: file.path.lastIndexOf('/')) + 1
        if (log) Log.i(TAG, "zip: file=${file.name}, basePathLength=$basePathLength")
        if (file.isFile) {
            zipFile(file, outStream, basePathLength, log)
        } else {
            zipDirectory(file, outStream, basePathLength, log)
        }
    }

    @Throws(IOException::class)
    private fun zipFile(file: File, outStream: ZipOutputStream, basePathLength: Int, log: Boolean = false) {
        BufferedInputStream(file.inputStream(), BUFFER_SIZE).use {
            val entry = ZipEntry(file.path.substring(basePathLength))
            if (log) Log.i(TAG, "zip: file=${file.name}, entry=${entry.name}")
            entry.time = file.lastModified() // to keep modification time after unzipping
            outStream.putNextEntry(entry)
            val data = ByteArray(BUFFER_SIZE)
            var count = it.read(data, 0, BUFFER_SIZE)
            while (count != -1) {
                outStream.write(data, 0, count)
                count = it.read(data, 0, BUFFER_SIZE)
            }
        }
    }

    @Throws(IOException::class)
    private fun zipDirectory(dir: File, outStream: ZipOutputStream, basePathLength: Int, log: Boolean = false) {
        dir.listFiles()?.forEach {
            if (it.isFile) {
                zipFile(it, outStream, basePathLength, log)
            } else {
                zipDirectory(it, outStream, basePathLength, log)
            }
        }
    }

    @Throws(IOException::class)
    fun unzip(contentResolver: ContentResolver, uri: Uri, dir: File) {
        contentResolver.openInputStream(uri)?.let {
            val inputStream = ZipInputStream(BufferedInputStream(it))
            var entry = inputStream.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    val subDir = File(dir, entry.name)
                    if (!subDir.exists())
                        subDir.mkdirs()
                } else {
                    val file = File(dir, entry.name)
                    file.createFileIfNotExists()
                    file.outputStream().use {
                        val data = ByteArray(BUFFER_SIZE)
                        var count = inputStream.read(data, 0, BUFFER_SIZE)
                        while (count != -1) {
                            it.write(data, 0, count)
                            count = inputStream.read(data, 0, BUFFER_SIZE)
                        }
                    }
                }
                entry = inputStream.nextEntry
            }
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
    
    fun createSnackProgressBarManager(view: View, lifecycleOwner: LifecycleOwner?): SnackProgressBarManager {
        val result = SnackProgressBarManager(view, lifecycleOwner)
        result
            .setProgressBarColor(R.color.colorAccent)
            .setBackgroundColor(SnackProgressBarManager.BACKGROUND_COLOR_DEFAULT)
            .setTextSize(14f)
            .setMessageMaxLines(2)
        return result
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

    fun dialogBuilder(
        activity: AppCompatActivity,
        title: String? = null,
        content: String? = null,
        iconRes: Int = R.drawable.ic_warning_white_vector
    ): MaterialDialog.Builder {
        val dialogBuilder = MaterialDialog.Builder(activity)

        if (title != null)
            dialogBuilder.title(activity.getString(R.string.confirm_action))

        if (content != null)
            dialogBuilder.content(content)

        dialogBuilder
            .iconRes(iconRes)

        dialogBuilder.positiveText(activity.getString(R.string.okay)).onPositive { dialog, _ -> dialog.dismiss() }

        return dialogBuilder
    }

    fun getCurrentFormattedDate(): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())

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

    //Unique Notification IDs for notifications
    private class NotificationId {
        companion object {
            // First 1000 reserved for other notifications
            private const val notificationIdStartFrom = 1000

            @JvmStatic
            internal val notificationIdCounter: AtomicInteger = AtomicInteger(notificationIdStartFrom)
        }
    }

    fun getUniqueNotificationId() = NotificationId.notificationIdCounter.getAndIncrement()

    fun measureTime(codeBlock: () -> Unit) {
        val startTime = System.currentTimeMillis()
        codeBlock()
        val diff = (System.currentTimeMillis() - startTime) / 1000f
        Logs.info("MeasuredTime", "$diff")
    }

    fun Document.getFormattedText(): String {
        outputSettings(Document.OutputSettings().prettyPrint(false))
        val htmlString: String = html()//.replace("\\\\n", "\n")
        return Jsoup.clean(htmlString, "", Whitelist.none(), Document.OutputSettings().prettyPrint(false)).replace("&nbsp", "")
    }

}
