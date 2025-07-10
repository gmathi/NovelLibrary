package io.github.gmathi.novellibrary.util

import android.app.ActivityManager
import android.content.*
import android.content.Context.ACTIVITY_SERVICE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.CompiledTTSFilter
import io.github.gmathi.novellibrary.model.other.TTSFilterTarget
import io.github.gmathi.novellibrary.model.other.TTSFilterType
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.writableFileName
import io.github.gmathi.novellibrary.util.storage.createFileIfNotExists
import io.github.gmathi.novellibrary.util.storage.getOrCreateDirectory
import io.github.gmathi.novellibrary.util.storage.getOrCreateFile
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import uy.kohesive.injekt.injectLazy
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


object Utils {

    private const val TAG = "UTILS"
    private const val BUFFER_SIZE = 16384

    private val dbHelper: DBHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()

    fun getImage(image: ByteArray): Bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)

    fun getNovelDir(context: Context, novelName: String, novelId: Long): File {
        val path = context.filesDir
        var writableNovelName = novelName.writableFileName()
        if (writableNovelName.isEmpty()) {
            writableNovelName = UUID.randomUUID().toString().writableFileName()
        }
        val dirName = "$writableNovelName-$novelId"
        val novelDir = File(path, dirName)
        if (!novelDir.exists()) novelDir.mkdir()
        return novelDir
    }

    @Deprecated(message = "Use the above function `getNovelDir(context, novelName, novelId)`")
    fun getHostDir(context: Context, url: String): File {
        val uri = Uri.parse(url)
        val path = context.filesDir

        val dirName = (uri.host ?: "NoHostNameFound").writableFileName()
        val hostDir = File(path, dirName)
        if (!hostDir.exists()) hostDir.mkdir()

        return hostDir
    }

    @Deprecated(message = "Use the above function `getNovelDir(context, novelName, novelId)`")
    fun getNovelDir(hostDir: File, novelName: String): File {
        val novelDir = File(hostDir, novelName.writableFileName())
        if (!novelDir.exists()) novelDir.mkdir()
        return novelDir
    }

    fun deleteDownloadedChapters(context: Context, novel: Novel) {
        //This is the old download data
        val hostDir = getHostDir(context, novel.url)
        val novelDir = getNovelDir(hostDir, novel.name)
        novelDir.deleteRecursively()

        //This is the new folder structure
        val newNovelDir = getNovelDir(context, novel.name, novel.id)
        newNovelDir.deleteRecursively()
    }

    fun broadcastNovelDelete(context: Context, novel: Novel) {
        val localIntent = Intent()
        val extras = Bundle()
        extras.putLong(Constants.NOVEL_ID, novel.id)
        localIntent.action = Constants.NOVEL_DELETED
        localIntent.putExtras(extras)
        localIntent.addCategory(Intent.CATEGORY_DEFAULT)
        context.sendBroadcast(localIntent)
    }

    /**
     * Copies data from input stream to file using a buffer
     */
    @Throws(IOException::class)
    private fun copyStreamToFile(inputStream: InputStream, outputStream: OutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
    }

    @Throws(IOException::class)
    fun copyFile(inputStream: InputStream, dst: File) {
        inputStream.use { inStream ->
            FileOutputStream(dst).use { outStream ->
                copyStreamToFile(inStream, outStream)
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
                copyStreamToFile(inStream, outStream)
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(contentResolver: ContentResolver, src: DocumentFile, dst: File) {
        contentResolver.openInputStream(src.uri)?.use { inStream ->
            FileOutputStream(dst).use { outStream ->
                copyStreamToFile(inStream, outStream)
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
        BufferedInputStream(file.inputStream(), BUFFER_SIZE).use { inStream ->
            val entry = ZipEntry(file.path.substring(basePathLength))
            if (log) Log.i(TAG, "zip: file=${file.name}, entry=${entry.name}")
            entry.time = file.lastModified() // to keep modification time after unzipping
            outStream.putNextEntry(entry)
            copyStreamToFile(inStream, outStream)
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
        contentResolver.openInputStream(uri)?.let { inputStream ->
            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.isDirectory) {
                    val subDir = File(dir, entry.name)
                    if (!subDir.exists())
                        subDir.mkdirs()
                } else {
                    val file = File(dir, entry.name)
                    file.createFileIfNotExists()
                    file.outputStream().use { outStream ->
                        copyStreamToFile(zipInputStream, outStream)
                    }
                }
                entry = zipInputStream.nextEntry
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
     * @deprecated Use DiskUtil.getDirectorySize() instead
     */
    @Deprecated("Use DiskUtil.getDirectorySize() instead")
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
    @Deprecated("Use Context.isServiceRunning() extension function instead")
    fun isServiceRunning(context: Context, serviceQualifiedName: String): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { serviceQualifiedName == it.service.className }
    }

    @Deprecated("Use UIUtils.showConfirmDialog() instead")
    fun dialogBuilder(
        activity: AppCompatActivity,
        title: String? = null,
        content: String? = null,
        @DrawableRes iconRes: Int = R.drawable.ic_warning_white_vector
    ) = MaterialDialog(activity).show {
        if (title != null)
            title(text = title)
        else
            title(R.string.confirm_action)

        message(text = content)

        icon(iconRes)

        positiveButton(R.string.okay)
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
        val doc = clone()
        val body = doc.body()
        body.select("[tts-disable=\"true\"]").remove()
        val content = body.select("[data-role=\"RContent\"]")
        if (content.isNotEmpty()) {
            content.select("[data-role=\"RHeader\"]").remove()
            content.select("[data-role=\"RFooter\"]").remove()
            content.select("[data-role=\"RNavigation\"]").remove()
            content.select("[data-role=\"RPage\"]").remove()
            content.select("[data-role=\"RChapterLink\"]").remove()
            content.select("[data-role=\"RRealChapter\"]").remove()
            content.select("select,input,button").remove()
            body.children().remove()
            if (!dataCenter.ttsPreferences.stripHeader) body.append(doc.title())
            content.forEach { body.appendChild(it) }
            doc.head().children().remove()
//            doc.head().html("")
        }
        doc.select("br,p,hr").forEach {
            it.after("\n")
        }

        val filters = dataCenter.ttsPreferences.filterList

        filters.forEach {
            if (it.type == TTSFilterType.Selector) {
                doc.select(it.lookup).remove()
            }
        }

        // Unwrap various notation to simplify processing.
        val notations = doc.select("em,strong,italic,s,i,a")
        // HACK: unwrap seem to strip whitespaces before/after the tag, so we have to ensure they have spacing within the tag.
        notations.forEach { el -> el.prependText(" "); el.appendText(" ") }
        notations.unwrap()

        val textFilters = filters.filter { it.target == TTSFilterTarget.TextChunk }.map { it.compile(doc) }

        applyFilters(doc.body(), filters.filter { it.target == TTSFilterTarget.Element }.map { it.compile(doc) })
        val cleaner = Cleaner(Safelist.none())
        val cleanDoc = cleaner.clean(doc)
        cleanDoc.outputSettings(Document.OutputSettings().prettyPrint(false))
        var text = cleanDoc.body().html()
            // Replace no-break spaces with a regular space
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", " and ")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            // Perform a limited trim operation on excessive spacing, causing "     " to turn into just " " as well as changing \n\n\n\n into \n
            .replace("""([\s ])+""".toRegex(RegexOption.MULTILINE)) { it.groups[0]?.value ?: "" }
            // Shorten long repeating characters such as =====, -----, -=-=-=-=-, ***** or !!!!!!!!
            .replace("""((?>[◆◇＝_~=*#|+<>\-─＊] ?){4,}|\.{4,}|!{4,}|\?{4,})""".toRegex()) { it.value.replace(" ", "").substring(0, 3) }
            .trim()

        textFilters.forEach { filter ->
            text = filter.apply(text)
        }
//        val htmlString: String = html()//.replace("\\\\n", "\n")
//        return Jsoup.clean(htmlString, "", Whitelist.none(), Document.OutputSettings().prettyPrint(false)).replace("&nbsp", "")
        return text
    }

    private fun applyFilters(element: Element, filters: List<CompiledTTSFilter>) {
        if (element.childrenSize() == 0 && element.hasText()) {
            var text = element.ownText()
            filters.forEach { filter -> text = filter.apply(text) }
            element.text(text)
        } else {
            element.children().forEach {
                applyFilters(it, filters)
            }
        }
    }

    fun copyErrorToClipboard(e: Exception, activity: AppCompatActivity) {
        val errorMessage = e.localizedMessage ?: "Unknown Error" + "\n" + e.stackTrace.joinToString(separator = "\n") { it.toString() }
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("Error Message", errorMessage)
        clipboard.setPrimaryClip(clip)
        MaterialDialog(activity).show {
            title(text = "Error!")
            message(text = "The error message has been copied to clipboard. Please paste it in discord #bugs channel.")
            lifecycleOwner(activity)
        }
    }


    @Throws(MalformedURLException::class)
    fun replaceHostInUrl(originalUrl: String?, newHostName: String?): String? {
        val url = originalUrl ?: return null
        var newHost = newHostName ?: return null

        val originalURL = URL(url)
        val hostHasPort = newHostName.indexOf(":") != -1
        var newPort: Int
        if (hostHasPort) {
            val hostURL = URL("http://$newHostName")
            newHost = hostURL.host
            newPort = hostURL.port
        } else {
            newPort = -1
        }

        // Use implicit port if it's a default port
        val isHttps: Boolean = originalURL.protocol.equals("https")
        val useDefaultPort = newPort == 443 && isHttps || newPort == 80 && !isHttps
        newPort = if (useDefaultPort) -1 else newPort
        val newURL = URL(originalURL.protocol, newHost, newPort, originalURL.file)
        return newURL.toString()
    }

}
