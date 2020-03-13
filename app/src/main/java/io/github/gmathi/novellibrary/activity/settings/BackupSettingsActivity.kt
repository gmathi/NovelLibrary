package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.github.johnpersano.supertoasts.library.Style
import com.github.johnpersano.supertoasts.library.SuperActivityToast
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.service.backup.BackupWorker
import io.github.gmathi.novellibrary.service.oneTimeBackupWorkRequest
import io.github.gmathi.novellibrary.service.oneTimeRestoreWorkRequest
import io.github.gmathi.novellibrary.service.periodicBackupWorkRequest
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import java.io.File
import java.util.*


class BackupSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "BackupSettingsActivity"

        private const val BACKUP_FILE_NAME = "NovelLibrary.backup.zip"
        private val ZIP_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "application/zip"

        private const val CREATE_BACKUP_REQUEST_CODE = 1121
        private const val RESTORE_BACKUP_REQUEST_CODE = 1122
    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private var confirmDialog: MaterialDialog? = null

    private var simpleText: Boolean = false
    private var database: Boolean = false
    private var preferences: Boolean = false
    private var files: Boolean = false

    private var workRequestId: UUID? = null

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
                        .title(item)
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
                        .title(item)
                        .items(R.array.backup_and_restore_options)
                        .itemsCallbackMultiChoice(arrayOf(0, 1, 2, 3)) { _, which, _ ->
                            if (which.isNotEmpty())
                                restoreData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
                            true
                        }
                        .positiveText(R.string.okay)
                        .show()
            }

            getString(R.string.backup_frequency) -> {
                val selected =
                    when (dataCenter.backupFrequency) {
                        24 -> 1
                        24 * 7 -> 2
                        else -> 0
                    }
                MaterialDialog.Builder(this)
                        .theme(Theme.DARK)
                        .title(item)
                        .items(R.array.backup_frequency_options)
                        .itemsCallbackSingleChoice(selected) { _, _, which, _ ->
                            val backupFrequency =
                                when (which) {
                                    1 -> 24
                                    2 -> 24 * 7
                                    else -> 0
                                }
                            if (dataCenter.backupFrequency != backupFrequency) {
                                dataCenter.backupFrequency = backupFrequency

                                WorkManager.getInstance(applicationContext).apply {
                                    if (backupFrequency == 0) {
                                        cancelUniqueWork(BackupWorker.UNIQUE_WORK_NAME)
                                    } else {
                                        enqueueUniquePeriodicWork(
                                            BackupWorker.UNIQUE_WORK_NAME,
                                            ExistingPeriodicWorkPolicy.REPLACE,
                                            periodicBackupWorkRequest()
                                        )
                                    }
                                }

                            }
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
        if (workRequestId != null)
            WorkManager.getInstance(applicationContext)
                .getWorkInfoByIdLiveData(workRequestId!!)
                .removeObservers(this)
        super.onDestroy()
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
        }

    private fun backupData(shouldBackupSimpleText: Boolean = true, shouldBackupDatabase: Boolean = true, shouldBackupPreferences: Boolean = true, shouldBackupFiles: Boolean = true) {
        simpleText = shouldBackupSimpleText
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
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
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
                var workRequest: OneTimeWorkRequest? = null

                when (requestCode) {
                    CREATE_BACKUP_REQUEST_CODE -> {
                        val readWriteFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                        val oldUri = dataCenter.backupUri
                        if (uri != oldUri) {
                            // Release old permissions
                            if (oldUri != null)
                                contentResolver.releasePersistableUriPermission(oldUri, readWriteFlags)

                            // Request for the new permissions to persist
                            contentResolver.takePersistableUriPermission(uri, data.flags and readWriteFlags)

                            dataCenter.backupUri = uri
                        }

                        workRequest = oneTimeBackupWorkRequest(simpleText, database, preferences, files)
                    }
                    RESTORE_BACKUP_REQUEST_CODE -> {
                        workRequest = oneTimeRestoreWorkRequest(simpleText, database, preferences, files)
                    }
                }

                if (workRequest != null) {
                    val workManager = WorkManager.getInstance(applicationContext)
                    workManager.enqueue(workRequest)
                    val observable = workManager.getWorkInfoByIdLiveData(workRequest.id)
                    observable.observe(this, Observer { info ->
                        if (info != null && arrayOf(State.SUCCEEDED, State.FAILED, State.CANCELLED).contains(info.state)) {
                            @Suppress("NON_EXHAUSTIVE_WHEN")
                            when (info.state) {
                                State.SUCCEEDED -> showDialog(iconRes = R.drawable.ic_check_circle_white_vector, content = info.outputData.getString(WORK_KEY_RESULT))
                                State.FAILED, State.CANCELLED -> showDialog(iconRes = R.drawable.ic_close_white_vector, content = info.outputData.getString(WORK_KEY_RESULT))
                            }
                            observable.removeObservers(this)
                        }
                    })
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
