package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.format.Formatter
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.github.johnpersano.supertoasts.library.Style
import com.github.johnpersano.supertoasts.library.SuperActivityToast
import com.github.johnpersano.supertoasts.library.SuperToast
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.createNovel
import io.github.gmathi.novellibrary.database.createNovelSection
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.Utils.recursiveCopy
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class BackupSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "BackupSettingsActivity"

        private const val BACKUP_FILE_NAME = "NovelLibrary.backup.zip"
        private val ZIP_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "application/zip"

        private const val SIMPLE_NOVEL_BACKUP_FILE_NAME = "SimpleNovelBackup.txt"
        private const val DATABASES_DIR = "databases"
        private const val FILES_DIR = "files"
        private const val SHARED_PREFS_DIR = "shared_prefs"

        private const val CREATE_BACKUP_REQUEST_CODE = 1121
        private const val RESTORE_BACKUP_REQUEST_CODE = 1122

        private const val DATA_SUBFOLDER = """/data/${BuildConfig.APPLICATION_ID}"""
    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private var confirmDialog: MaterialDialog? = null

    private var simpleText: Boolean = false
    private var database: Boolean = false
    private var preferences: Boolean = false
    private var files: Boolean = false

    private var progressToast: SuperToast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.backup_and_restore_titles_list).asList())
        settingsItemsDescription = ArrayList(resources.getStringArray(R.array.backup_and_restore_subtitles_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.widgetChevron.visibility = View.GONE
        itemView.widgetSwitch.visibility = View.GONE
        itemView.currentValue.visibility = View.GONE
        itemView.widget.visibility = View.GONE
        itemView.blackOverlay.visibility = View.GONE

        itemView.title.applyFont(assets).text = item
        itemView.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemView.widgetSwitch.setOnCheckedChangeListener(null)

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.internal_clear_data) ->
                deleteFilesDialog()

            getString(R.string.backup_data) -> {
                MaterialDialog.Builder(this)
                        .title(getString(R.string.backup_data))
                        .items(R.array.backup_and_restore_options)
                        .itemsCallbackMultiChoice(arrayOf(0, 1, 2, 3)) { _, which, _ ->
                            if (which.isNotEmpty())
                                backupData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
                            true
                        }
                        .positiveText(R.string.okay)
                        .show()
            }

            getString(R.string.restore_data) -> {
                MaterialDialog.Builder(this)
                        .title(getString(R.string.restore_data))
                        .items(R.array.backup_and_restore_options)
                        .itemsCallbackMultiChoice(arrayOf(0, 1, 2, 3)) { _, which, _ ->
                            if (which.isNotEmpty())
                                restoreData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
                            true
                        }
                        .positiveText(R.string.okay)
                        .show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressToast?.dismiss()
        async.cancelAll()
    }

    private fun showDialog(title: String? = null, content: String? = null, iconRes: Int = R.drawable.ic_warning_white_vector, isProgress: Boolean = false) {
        if (confirmDialog != null && confirmDialog!!.isShowing)
            confirmDialog!!.dismiss()

        val confirmDialogBuilder = MaterialDialog.Builder(this)

        if (title != null)
            confirmDialogBuilder.title(getString(R.string.confirm_action))

        if (isProgress)
            confirmDialogBuilder.progress(true, 100)

        if (content != null)
            confirmDialogBuilder.content(content)

        confirmDialogBuilder
                .iconRes(iconRes)

        if (!isProgress)
            confirmDialogBuilder.positiveText(getString(R.string.okay)).onPositive { dialog, _ -> dialog.dismiss() }

        confirmDialog = confirmDialogBuilder.build()
        confirmDialog?.show()
    }

    private val zipIntent: Intent
        get() {
            return Intent()
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(ZIP_MIME_TYPE)
                    .putExtra(Intent.EXTRA_TITLE, BACKUP_FILE_NAME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

    private fun backupData(shouldSimpleTextBackup: Boolean = true, shouldBackupDatabase: Boolean = true, shouldBackupPreferences: Boolean = true, shouldBackupFiles: Boolean = true) {
        simpleText = shouldSimpleTextBackup
        database = shouldBackupDatabase
        preferences = shouldBackupPreferences
        files = shouldBackupFiles

        if (dataCenter.showBackupHint) {
            SuperActivityToast.create(this, Style(), Style.TYPE_BUTTON)
                    .setButtonText(getString(R.string.dont_show_again))
                    .setOnButtonClickListener("hint", null
                    ) { _, _ ->
                        dataCenter.showBackupHint = false
                    }
                    .setText(getString(R.string.backup_hint))
                    .setDuration(Style.DURATION_VERY_LONG)
                    .setFrame(Style.FRAME_LOLLIPOP)
                    .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE_GREY))
                    .setAnimations(Style.ANIMATIONS_POP)
                    .setOnDismissListener { _, _ -> doBackup() }
                    .show()
        } else doBackup()
    }

    private fun doBackup() {
        val intent = zipIntent.setAction(Intent.ACTION_CREATE_DOCUMENT)
        startActivityForResult(intent, CREATE_BACKUP_REQUEST_CODE)
    }

    private fun restoreData(shouldRestoreSimpleText: Boolean = true, shouldRestoreDatabase: Boolean = true, shouldRestorePreferences: Boolean = true, shouldRestoreFiles: Boolean = true) {
        simpleText = shouldRestoreSimpleText
        database = shouldRestoreDatabase
        preferences = shouldRestorePreferences
        files = shouldRestoreFiles

        if (dataCenter.showRestoreHint) {
            SuperActivityToast.create(this, Style(), Style.TYPE_BUTTON)
                    .setButtonText(getString(R.string.dont_show_again))
                    .setOnButtonClickListener("hint", null
                    ) { _, _ ->
                        dataCenter.showRestoreHint = false
                    }
                    .setText(getString(R.string.restore_hint))
                    .setDuration(Style.DURATION_VERY_LONG)
                    .setFrame(Style.FRAME_LOLLIPOP)
                    .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE_GREY))
                    .setAnimations(Style.ANIMATIONS_POP)
                    .setOnDismissListener { _, _ -> doRestore() }
                    .show()
        } else doRestore()
    }

    private fun doRestore() {
        val intent = zipIntent.setAction(Intent.ACTION_OPEN_DOCUMENT)
        startActivityForResult(intent, RESTORE_BACKUP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null)
            return

        if (requestCode == CREATE_BACKUP_REQUEST_CODE || requestCode == RESTORE_BACKUP_REQUEST_CODE) {
            val uri = data.data
            if (uri != null) {
                if (progressToast == null)
                    progressToast = SuperActivityToast.create(this, Style(), Style.TYPE_PROGRESS_BAR)
                            .setIndeterminate(true)
                            .setFrame(Style.FRAME_LOLLIPOP)
                            .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE_GREY))
                            .setAnimations(Style.ANIMATIONS_FADE)
                progressToast?.show()

                val dataDir = Environment.getDataDirectory()
                val baseDir = File(dataDir, DATA_SUBFOLDER)

                val currentDBsDir = File(baseDir, DATABASES_DIR)
                val currentSharedPrefsDir = File(baseDir, SHARED_PREFS_DIR)
                val currentFilesDir = File(baseDir, FILES_DIR)

                when (requestCode) {
                    CREATE_BACKUP_REQUEST_CODE -> {
                        async {
                            try {
                                ZipOutputStream(BufferedOutputStream(contentResolver.openOutputStream(uri)!!)).use {
                                    // Backup To TextFile
                                    if (simpleText) {
                                        val novelsArray = dbHelper.getAllNovels()
                                        val novelSectionsArray = dbHelper.getAllNovelSections()
                                        val map = HashMap<String, Any>()
                                        map["novels"] = novelsArray
                                        map["novelSections"] = novelSectionsArray
                                        val jsonString = Gson().toJson(map)
                                        val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                                        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(simpleTextFile)))
                                        writer.use { writer.write(jsonString) }
                                        await { Utils.zip(simpleTextFile, it) }
                                    }

                                    // Backup Databases
                                    if (database && currentDBsDir.exists() && currentDBsDir.isDirectory) {
                                        await { Utils.zip(currentDBsDir, it) }
                                    }

                                    // Backup Shared Preferences
                                    if (preferences && currentSharedPrefsDir.exists() && currentSharedPrefsDir.isDirectory) {
                                        await { Utils.zip(currentSharedPrefsDir, it) }
                                    }

                                    // Backup Files
                                    if (files && currentFilesDir.exists() && currentFilesDir.isDirectory) {
                                        await { Utils.zip(currentFilesDir, it) }
                                    }
                                }

                                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Backup Successful")

                            } catch (e: Exception) {
                                Logs.error(TAG, e.localizedMessage, e)
                                if ("No space left on device" == e.localizedMessage) {
                                    val databasesDirSize = Utils.getFolderSize(currentDBsDir)
                                    val filesDirSize = Utils.getFolderSize(currentFilesDir)
                                    val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsDir)

                                    val formattedSize = Formatter.formatFileSize(this@BackupSettingsActivity, databasesDirSize + filesDirSize + sharedPrefsDirSize)
                                    Logs.debug("Size", formattedSize)
                                    showDialog(content = "No space left on device! Please make enough space - $formattedSize and try again!")
                                } else
                                    showDialog(content = "Backup Failed!")
                            } finally {
                                progressToast?.dismiss()
                            }
                        }
                    }
                    RESTORE_BACKUP_REQUEST_CODE -> {
                        async {
                            try {
                                ZipInputStream(BufferedInputStream(contentResolver.openInputStream(uri)!!)).use {
                                    // todo: if possible extract selectively, directly to the data directory without using the cache directory
                                    await { Utils.unzip(it, cacheDir) }
                                }

                                val backupDBsDir = File(cacheDir, DATABASES_DIR)
                                val backupFilesDir = File(cacheDir, FILES_DIR)
                                val backupSharedPrefsDir = File(cacheDir, SHARED_PREFS_DIR)

                                // Restore From Text File
                                val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                                if (simpleText && simpleTextFile.exists() && simpleTextFile.canRead()) {
                                    val reader = BufferedReader(InputStreamReader(FileInputStream(simpleTextFile)))
                                    val jsonString = reader.readLine()
                                    val jsonObject = JSONObject(jsonString)
                                    val novelsArray = jsonObject.getJSONArray("novels")
                                    val novelSectionsArray = jsonObject.getJSONArray("novelSections")
                                    val oldIdMap = HashMap<Long, String>()
                                    val newIdMap = HashMap<String, Long>()
                                    for (i in 0 until novelSectionsArray.length()) {
                                        val novelSection = novelSectionsArray.getJSONObject(i)
                                        val name = novelSection.getString("name")
                                        oldIdMap[novelSection.getLong("id")] = name
                                        newIdMap[name] = dbHelper.createNovelSection(novelSection.getString("name"))
                                    }
                                    for (i in 0 until novelsArray.length()) {
                                        val novelJson = novelsArray.getJSONObject(i)
                                        val novel = Novel(name = novelJson.getString("name"), url = novelJson.getString("url"))
                                        if (novelJson.has("imageUrl"))
                                            novel.imageUrl = novelJson.getString("imageUrl")
                                        if (novelJson.has("currentlyReading"))
                                            novel.currentWebPageUrl = novelJson.getString("currentlyReading")
                                        if (novelJson.has("metaData"))
                                            novel.metaData = Gson().fromJson(novelJson.getString("metaData"), object : TypeToken<HashMap<String, String>>() {}.type)

                                        novel.novelSectionId = newIdMap[oldIdMap[novelJson.getLong("novelSectionId")]] ?: -1L
                                        dbHelper.createNovel(novel)
                                    }
                                }

                                //Restore Databases
                                if (database && backupDBsDir.exists() && backupDBsDir.isDirectory) {
                                    if (!currentDBsDir.exists()) currentDBsDir.mkdir()
                                    backupDBsDir.listFiles()?.forEach {
                                        await { Utils.copyFile(it, File(currentDBsDir, it.name)) }
                                    }
                                }

                                //Restore Shared Preferences
                                if (preferences && backupSharedPrefsDir.exists() && backupSharedPrefsDir.isDirectory) {
                                    if (!currentSharedPrefsDir.exists()) currentSharedPrefsDir.mkdir()
                                    backupSharedPrefsDir.listFiles()?.forEach {
                                        await { Utils.copyFile(it, File(currentSharedPrefsDir, it.name)) }
                                    }
                                }

                                //Restore Files
                                if (files && backupFilesDir.exists() && backupFilesDir.isDirectory) {
                                    if (!currentFilesDir.exists()) currentFilesDir.mkdir()
                                    await { recursiveCopy(backupFilesDir, currentFilesDir) }
                                }

                                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Restore Successful!")

                            } catch (e: Exception) {
                                Logs.error(TAG, e.localizedMessage, e)
                                if ("No space left on device" == e.localizedMessage) {
                                    val databasesDirSize = Utils.getFolderSize(currentDBsDir)
                                    val filesDirSize = Utils.getFolderSize(currentFilesDir)
                                    val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsDir)

                                    val formattedSize = Formatter.formatFileSize(this@BackupSettingsActivity, databasesDirSize + filesDirSize + sharedPrefsDirSize)
                                    Logs.debug("Size", formattedSize)
                                    showDialog(content = "No space left on device! Please make enough space - $formattedSize and try again!")
                                }
                                showDialog(content = "Restore Failed!")
                            } finally {
                                progressToast?.dismiss()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun deleteFilesDialog() {
        MaterialDialog.Builder(this)
                .title(getString(R.string.clear_data))
                .content(getString(R.string.clear_data_description))
                .positiveText(R.string.clear)
                .negativeText(R.string.cancel)
                .onPositive { dialog, _ ->
                    val progressDialog = MaterialDialog.Builder(this)
                            .title(getString(R.string.clearing_data))
                            .content(getString(R.string.please_wait))
                            .progress(true, 0)
                            .cancelable(false)
                            .canceledOnTouchOutside(false)
                            .show()
                    deleteFiles(progressDialog)
                    dialog.dismiss()
                }
                .onNegative { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun deleteFiles(dialog: MaterialDialog) {
        try {
            deleteDir(cacheDir)
            deleteDir(filesDir)
            dbHelper.removeAll()
            dataCenter.saveSearchHistory(ArrayList())
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null)
                for (i in children.indices)
                    deleteDir(File(dir, children[i]))
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }
}
