package io.github.gmathi.novellibrary.activity.settings

import android.Manifest
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.text.format.Formatter
import android.util.Log
import android.view.MenuItem
import android.view.View
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.thanosfisherman.mayi.Mayi
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import java.io.File
import java.io.IOException


class GeneralSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private var confirmDialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(io.github.gmathi.novellibrary.R.array.general_titles_list).asList())
        settingsItemsDescription = ArrayList(resources.getStringArray(io.github.gmathi.novellibrary.R.array.general_subtitles_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.widgetChevron.visibility = View.INVISIBLE
        itemView.widgetSwitch.visibility = View.INVISIBLE
        itemView.widgetButton.visibility = View.INVISIBLE

        itemView.title.applyFont(assets).text = item
        itemView.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemView.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            0 -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.backup)
                itemView.widgetButton.setOnClickListener {
                    Mayi.withActivity(this@GeneralSettingsActivity)
                        .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onResult {
                            if (it.isGranted)
                                backupData()
                            else
                                showDialog(content = "Enable \"Write External Storage\" permission for Novel Library " +
                                    "from your device Settings -> Applications -> Novel Library -> Permissions")
                        }.check()
                }
            }
            1 -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.restore)
                itemView.widgetButton.setOnClickListener {
                    Mayi.withActivity(this@GeneralSettingsActivity)
                        .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onResult {
                            if (it.isGranted)
                                restoreData()
                            else
                                showDialog(content = "Enable \"Read External Storage\" permission for Novel Library " +
                                    "from your device Settings -> Applications -> Novel Library -> Permissions")
                        }.check()

                }
            }
            2 -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.clear)
                itemView.widgetButton.setOnClickListener { deleteFilesDialog() }
            }
//            3 -> {
//                itemView.widgetSwitch.visibility = View.VISIBLE
//                itemView.widgetSwitch.isChecked = dataCenter.experimentalDownload
//                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.experimentalDownload = value }
//            }
            3 -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.loadLibraryScreen
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.loadLibraryScreen = value }
            }

        }

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
//        if (item == getString(R.string.sync_interval)) {
//            showSyncIntervalDialog()
//        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region Delete Files
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
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                deleteDir(File(dir, children[i]))
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }
    //endregion

    //region Backup & Restore Data

    private val databasesDirName = "databases"
    private val filesDirName = "files"
    private val sharedPrefsDirName = "shared_prefs"

    private fun backupData() {
        async {
            val data = Environment.getDataDirectory()
            val baseDir = File(data, "//data//io.github.gmathi.novellibrary")
            val currentDBsPath = File(baseDir, databasesDirName)
            val currentFilesDir = File(baseDir, filesDirName)
            val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)

            val sd = Environment.getExternalStorageDirectory()
            val backupDir = File(sd, Constants.BACKUP_DIR)
            val backupDBsPath = File(backupDir, databasesDirName)
            val backupFilesDir = File(backupDir, filesDirName)
            val backupSharedPrefsPath = File(backupDir, sharedPrefsDirName)

            try {
                showDialog(isProgress = true, content = "Backing up data...")
                if (!Utils.isSDCardPresent) {
                    showDialog(content = "No SD card found!")
                    return@async
                }

                //Log.e("Permission", "Check: " + (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)))

                if (sd.canWrite()) {

                    //create backup directory
                    if (!backupDir.exists())
                        backupDir.mkdir()

                    //Backup Databases
                    if (currentDBsPath.exists() && currentDBsPath.isDirectory) {
                        if (!backupDBsPath.exists()) backupDBsPath.mkdir()
                        currentDBsPath.listFiles().forEach {
                            await { Utils.copyFile(it, File(backupDBsPath, it.name)) }
                        }
                    }

                    //Backup Files
                    if (currentFilesDir.exists() && currentFilesDir.isDirectory) {
                        if (!backupFilesDir.exists()) backupFilesDir.mkdir()
                        await { recursiveCopy(currentFilesDir, backupFilesDir) }
                    }

                    //Backup Shared Preferences
                    // --> /data/data/io.github.gmathi.novellibrary/shared_prefs
                    if (currentSharedPrefsPath.exists() && currentSharedPrefsPath.isDirectory) {
                        if (!backupSharedPrefsPath.exists()) backupSharedPrefsPath.mkdir()
                        currentSharedPrefsPath.listFiles().forEach {
                            await { Utils.copyFile(it, File(backupSharedPrefsPath, it.name)) }
                        }
                    }

                    showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Backup Successful")

                } else showDialog(content = "Cannot write to SD card. Please check your SD card permissions")
            } catch (e: Exception) {
                e.printStackTrace()
                if ("No space left on device" == e.localizedMessage) {
                    val databasesDirSize = Utils.getFolderSize(currentDBsPath)
                    val filesDirSize = Utils.getFolderSize(currentFilesDir)
                    val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsPath)

                    val formattedSize = Formatter.formatFileSize(this@GeneralSettingsActivity, databasesDirSize + filesDirSize + sharedPrefsDirSize)
                    Log.e("Size", formattedSize)
                    showDialog(content = "No space left on device! Please make enough space - $formattedSize and try again!")
                } else
                    showDialog(content = "Backup Failed!")
            }
        }

    }

    @Throws(IOException::class)
    private fun recursiveCopy(src: File, dest: File) {
        src.listFiles().forEach { file ->
            if (file.isDirectory) {
                val destDir = File(dest, file.name)
                if (!destDir.exists()) destDir.mkdir()
                recursiveCopy(file, destDir)
            } else {
                //File(dest, file.name).createNewFile()
                Utils.copyFile(file, File(dest, file.name))
            }
        }
    }

    private fun restoreData() {
        async {
            try {
                showDialog(isProgress = true, content = "Restoring data...")
                if (!Utils.isSDCardPresent) {
                    showDialog(content = "No SD card found!")
                    return@async
                }

                val sd = Environment.getExternalStorageDirectory()
                val data = Environment.getDataDirectory()


                //Log.e("Permission", "Check: " + (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)))

                if (sd.canWrite()) {

                    //create backup directory
                    val backupDir = File(sd, Constants.BACKUP_DIR)
                    if (!backupDir.exists())
                        backupDir.mkdir()


                    val baseDir = File(data, "//data//io.github.gmathi.novellibrary")

                    //Restore Databases
                    val currentDBsPath = File(baseDir, databasesDirName)
                    val backupDBsPath = File(backupDir, databasesDirName)

                    if (backupDBsPath.exists() && backupDBsPath.isDirectory) {
                        if (!currentDBsPath.exists()) currentDBsPath.mkdir()
                        backupDBsPath.listFiles().forEach {
                            await { Utils.copyFile(it, File(currentDBsPath, it.name)) }
                        }
                    }

                    //Restore Files
                    val currentFilesDir = File(baseDir, filesDirName)
                    val backupFilesDir = File(backupDir, filesDirName)

                    if (backupFilesDir.exists() && backupFilesDir.isDirectory) {
                        if (!currentFilesDir.exists()) currentFilesDir.mkdir()
                        await { recursiveCopy(backupFilesDir, currentFilesDir) }
                    }

                    //Restore Shared Preferences
                    // --> /data/data/io.github.gmathi.novellibrary/shared_prefs
                    val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)
                    val backupSharedPrefsPath = File(backupDir, sharedPrefsDirName)

                    if (backupSharedPrefsPath.exists() && backupSharedPrefsPath.isDirectory) {
                        if (!currentSharedPrefsPath.exists()) currentSharedPrefsPath.mkdir()
                        backupSharedPrefsPath.listFiles().forEach {
                            await { Utils.copyFile(it, File(currentSharedPrefsPath, it.name)) }
                        }
                    }

                    showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Restore Successful")

                } else showDialog(content = "Cannot read from SD card. Please check your SD card permissions")
            } catch (e: Exception) {
                e.printStackTrace()
                showDialog(content = "Restore Failed!")
            }
        }

    }


//endregion

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

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }


}
