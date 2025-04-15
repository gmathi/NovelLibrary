package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.lang.launchIO
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.worker.BackupWorker
import io.github.gmathi.novellibrary.worker.oneTimeBackupWorkRequest
import io.github.gmathi.novellibrary.worker.oneTimeRestoreWorkRequest
import io.github.gmathi.novellibrary.worker.periodicBackupWorkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean


class BackupSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "BackupSettingsActivity"

        private const val BACKUP_FILE_NAME = "NovelLibrary.backup.zip"
        private val ZIP_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "application/zip"

        private const val CREATE_BACKUP_REQUEST_CODE = 1121
        private const val RESTORE_BACKUP_REQUEST_CODE = 1122

        private var BACKUP_FREQUENCY_LIST_INDEX = -1
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

    private val isDeletingFiles = AtomicBoolean(false)

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isDeletingFiles.get()) {
                    return
                }
                finish()
            }

        })
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.backup_and_restore_titles_list).asList())
        settingsItemsDescription = ArrayList(resources.getStringArray(R.array.backup_and_restore_subtitles_list).asList())
        setBackupFrequencyDescription()
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    private fun setBackupFrequencyDescription(backupFrequency: Int = dataCenter.backupFrequency) {
        if (BACKUP_FREQUENCY_LIST_INDEX == -1) BACKUP_FREQUENCY_LIST_INDEX = settingsItemsDescription.indexOf(getString(R.string.backup_frequency_description))
        settingsItemsDescription[BACKUP_FREQUENCY_LIST_INDEX] = getString(
            when (backupFrequency) {
                24 -> R.string.backup_frequency_Daily
                24 * 7 -> R.string.backup_frequency_Weekly
                else -> R.string.backup_frequency_manual
            }
        )
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.widgetChevron.visibility = View.GONE
        itemBinding.widgetSwitch.visibility = View.GONE
        itemBinding.currentValue.visibility = View.GONE
        itemBinding.widget.visibility = View.GONE
        itemBinding.blackOverlay.visibility = View.GONE

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: String, position: Int) {
        when (item) {
            getString(R.string.internal_clear_data) -> deleteFilesDialog()

            getString(R.string.backup_data) -> {
                MaterialDialog(this).show {
                    title(text = item).listItemsMultiChoice(R.array.backup_and_restore_options, initialSelection = intArrayOf(0, 1, 2, 3)) { _, which, _ ->
                        if (which.isNotEmpty()) backupData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
                    }.positiveButton(R.string.okay)
                }
            }

            getString(R.string.restore_data) -> {
                MaterialDialog(this).show {
                    title(text = item).listItemsMultiChoice(R.array.backup_and_restore_options, initialSelection = intArrayOf(0, 1, 2, 3)) { _, which, _ ->
                        if (which.isNotEmpty()) restoreData(which.contains(0), which.contains(1), which.contains(2), which.contains(3))
                    }.positiveButton(R.string.okay)
                }
            }

            getString(R.string.backup_frequency) -> {
                val selected = when (dataCenter.backupFrequency) {
                    24 -> 1
                    24 * 7 -> 2
                    else -> 0
                }
                MaterialDialog(this).show {
                    title(text = item).listItemsSingleChoice(R.array.backup_frequency_options, initialSelection = selected) { _, which, _ ->
                        var backupFrequency = when (which) {
                            1 -> 24
                            2 -> 24 * 7
                            else -> 0
                        }
                        if (dataCenter.backupFrequency != backupFrequency) {
                            val workRequest = if (backupFrequency != 0) periodicBackupWorkRequest(backupFrequency) else null
                            WorkManager.getInstance(applicationContext).apply {
                                if (workRequest == null) {
                                    backupFrequency = 0
                                    cancelUniqueWork(BackupWorker.UNIQUE_WORK_NAME)
                                } else {
                                    enqueueUniquePeriodicWork(
                                        BackupWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest
                                    )
                                }
                            }
                            dataCenter.backupFrequency = backupFrequency
                            setBackupFrequencyDescription(backupFrequency)
                            adapter.notifyItemChanged(BACKUP_FREQUENCY_LIST_INDEX)
                        }
                    }.positiveButton(R.string.okay)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (workRequestId != null) WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(workRequestId!!).removeObservers(this)
        super.onDestroy()
    }

    private fun showDialog(title: String? = null, content: String? = null, @DrawableRes iconRes: Int = R.drawable.ic_warning_white_vector) {
        if (confirmDialog != null && confirmDialog!!.isShowing) confirmDialog!!.dismiss()

        MaterialDialog(this).show {
            if (title != null) title(text = title)
            else title(R.string.confirm_action)

            if (content != null) message(text = content)

            icon(iconRes)

            positiveButton(R.string.okay) {
                it.dismiss()
            }
        }
    }

    private val zipIntent: Intent
        get() {
            return Intent().addCategory(Intent.CATEGORY_OPENABLE).setType(ZIP_MIME_TYPE).putExtra(Intent.EXTRA_TITLE, BACKUP_FILE_NAME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

    private fun backupData(
        shouldBackupSimpleText: Boolean = true, shouldBackupDatabase: Boolean = true, shouldBackupPreferences: Boolean = true, shouldBackupFiles: Boolean = true
    ) {
        simpleText = shouldBackupSimpleText
        database = shouldBackupDatabase
        preferences = shouldBackupPreferences
        files = shouldBackupFiles

        if (dataCenter.showBackupHint) {
            Snackbar.make(binding.root, R.string.backup_hint, Snackbar.LENGTH_LONG).setAction("Don't show again") {
                dataCenter.showBackupHint = false
            }.show()
        }
        doBackup()
    }

    private val doBackupContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
            val uri = it.data!!.data
            if (uri != null) {
                val readWriteFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                val array = dataCenter.backupData
                val oldUri = if (array != null) Uri.parse(Data.fromByteArray(array).getString(BackupWorker.KEY_URI))
                else null

                //Backup File location is not the same
                if (uri != oldUri) {
                    // Release old permissions
                    try {
                        if (oldUri != null) contentResolver.releasePersistableUriPermission(oldUri, readWriteFlags)
                    } catch (e: Exception) {
                        //Don't do anything
                    }

                    // Request for the new permissions to persist
                    it.data?.flags?.let { flags -> contentResolver.takePersistableUriPermission(uri, flags and readWriteFlags) }
                        ?: contentResolver.takePersistableUriPermission(uri, readWriteFlags)

                }

                val workRequest = oneTimeBackupWorkRequest(
                    uri, simpleText, database, preferences, files
                )
                executeWorkRequest(workRequest)
            }
        }
    }

    private fun doBackup() {
        val intent = zipIntent.setAction(Intent.ACTION_CREATE_DOCUMENT).addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        doBackupContract.launch(intent)
    }

    private fun restoreData(
        shouldRestoreSimpleText: Boolean = true,
        shouldRestoreDatabase: Boolean = true,
        shouldRestorePreferences: Boolean = true,
        shouldRestoreFiles: Boolean = true
    ) {
        simpleText = shouldRestoreSimpleText
        database = shouldRestoreDatabase
        preferences = shouldRestorePreferences
        files = shouldRestoreFiles

        if (dataCenter.showRestoreHint) {
            Snackbar.make(binding.root, R.string.restore_hint, Snackbar.LENGTH_LONG).setAction("Don't show again") {
                dataCenter.showRestoreHint = false
            }.show()
        }
        doRestore()
    }

    private val doRestoreContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                val workRequest: OneTimeWorkRequest = oneTimeRestoreWorkRequest(
                    uri, simpleText, database, preferences, files
                )
                executeWorkRequest(workRequest)
            }
        }
    }

    private fun executeWorkRequest(workRequest: WorkRequest?) {
        if (workRequest != null) {
            val workManager = WorkManager.getInstance(applicationContext)
            workManager.enqueue(workRequest)
            val observable = workManager.getWorkInfoByIdLiveData(workRequest.id)
            observable.observe(this) { info ->
                if (info != null && arrayOf(State.SUCCEEDED, State.FAILED, State.CANCELLED).contains(info.state)) {
                    when (info.state) {
                        State.SUCCEEDED -> showDialog(
                            iconRes = R.drawable.ic_check_circle_white_vector, content = info.outputData.getString(WORK_KEY_RESULT)
                        )

                        State.FAILED, State.CANCELLED -> showDialog(
                            iconRes = R.drawable.ic_close_white_vector, content = info.outputData.getString(WORK_KEY_RESULT)
                        )

                        else -> {

                        }
                    }
                    observable.removeObservers(this)
                }
            }
        }
    }

    private fun doRestore() {
        val intent = zipIntent.setAction(Intent.ACTION_OPEN_DOCUMENT)
        doRestoreContract.launch(intent)
    }

    private fun deleteFilesDialog() {
        MaterialDialog(this).show {
            title(R.string.clear_data)
            message(R.string.clear_data_description)
            positiveButton(R.string.clear) {
                val snackProgressBarManager = Utils.createSnackProgressBarManager(findViewById(android.R.id.content), this@BackupSettingsActivity)
                val snackProgressBar = SnackProgressBar(
                    SnackProgressBar.TYPE_NORMAL, getString(R.string.clearing_data) + " - " + getString(R.string.please_wait)
                )
                launchUI {
                    snackProgressBarManager.show(
                        snackProgressBar, SnackProgressBarManager.LENGTH_INDEFINITE
                    )
                }
                deleteFiles(snackProgressBarManager)
            }
            negativeButton(R.string.cancel) {
                it.dismiss()
            }
        }
    }

    private fun deleteFiles(dialog: SnackProgressBarManager) {
        launchIO {
            isDeletingFiles.set(true)
            try {
                deleteDir(cacheDir)
                deleteDir(filesDir)
                dbHelper.removeAll()
                dataCenter.saveNovelSearchHistory(ArrayList())

                @Suppress("DeferredResultUnused")
                async(Dispatchers.Main) {
                    dialog.disable()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isDeletingFiles.set(false)
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) for (i in children.indices) deleteDir(File(dir, children[i]))
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }

}
