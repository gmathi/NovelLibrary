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
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.documentfile.provider.DocumentFile
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.createFileIfNotExists
import io.github.gmathi.novellibrary.extensions.getOrCreateDirectory
import io.github.gmathi.novellibrary.extensions.getOrCreateFile
import io.github.gmathi.novellibrary.model.Novel
import kotlinx.coroutines.CoroutineScope
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object Utils {

    private const val TAG = "UTILS"

    fun getImage(image: ByteArray): Bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)

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
        BufferedInputStream(file.inputStream(), Constants.FILE_BUFFER_SIZE).use {
            val entry = ZipEntry(file.path.substring(basePathLength))
            if (log) Log.i(TAG, "zip: file=${file.name}, entry=${entry.name}")
            entry.time = file.lastModified() // to keep modification time after unzipping
            outStream.putNextEntry(entry)
            val data = ByteArray(Constants.FILE_BUFFER_SIZE)
            var count = it.read(data, 0, Constants.FILE_BUFFER_SIZE)
            while (count != -1) {
                outStream.write(data, 0, count)
                count = it.read(data, 0, Constants.FILE_BUFFER_SIZE)
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
    fun unzip(inputStream: ZipInputStream, dir: File) {
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
                    val data = ByteArray(Constants.FILE_BUFFER_SIZE)
                    var count = inputStream.read(data, 0, Constants.FILE_BUFFER_SIZE)
                    while (count != -1) {
                        it.write(data, 0, count)
                        count = inputStream.read(data, 0, Constants.FILE_BUFFER_SIZE)
                    }
                }
            }
            entry = inputStream.nextEntry
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
        iconRes: Int = R.drawable.ic_warning_white_vector,
        isProgress: Boolean = false
    ): MaterialDialog.Builder {
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

    fun getCurrentFormattedDate(): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date())

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

}
