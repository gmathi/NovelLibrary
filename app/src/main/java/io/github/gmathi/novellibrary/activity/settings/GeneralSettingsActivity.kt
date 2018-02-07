package io.github.gmathi.novellibrary.activity.settings

import android.Manifest
import android.accounts.AccountManager
import android.app.PendingIntent.getActivity
import android.content.Intent
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
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.http.GenericUrl
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.ParentReference
import com.thanosfisherman.mayi.Mayi
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.SSLPeerUnverifiedException


class GeneralSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val POSITION_BACKUP = 0
        private const val POSITION_RESTORE = 1
        private const val POSITION_CLEAR = 2
        private const val POSITION_GOOGLE_BACKUP = 3
        private const val POSITION_GOOGLE_RESTORE = 4
        private const val POSITION_LOAD_LIBRARY_SCREEN = 5

        private val SCOPES = arrayOf(DriveScopes.DRIVE)

        const val REQUEST_ACCOUNT_PICKER_BACKUP = 1000
        const val REQUEST_ACCOUNT_PICKER_RESTORE = 2000
        const val REQUEST_AUTHORIZATION_BACKUP = 1001
        const val REQUEST_AUTHORIZATION_RESTORE = 2001
        const val REQUEST_GOOGLE_PLAY_SERVICES_BACKUP = 1002
        const val REQUEST_GOOGLE_PLAY_SERVICES_RESTORE = 2002

        const val BACKUP_FOLDER_NAME = "NovelLibrary-Backup"

    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private var confirmDialog: MaterialDialog? = null
    private lateinit var credential: GoogleAccountCredential

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()

        val scopesList: ArrayList<String> = ArrayList()
        scopesList.add(DriveScopes.DRIVE)

        credential = GoogleAccountCredential.usingOAuth2(applicationContext, scopesList).setBackOff(ExponentialBackOff())
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
            POSITION_BACKUP -> {
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
            POSITION_RESTORE -> {
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
            POSITION_CLEAR -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.clear)
                itemView.widgetButton.setOnClickListener { deleteFilesDialog() }
            }

            POSITION_GOOGLE_BACKUP -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.backup)
                itemView.widgetButton.setOnClickListener {
                    it.isEnabled = false
                    getResultsFromApi_Backup()
                    it.isEnabled = true
                }
            }

            POSITION_GOOGLE_RESTORE -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.restore)
                itemView.widgetButton.setOnClickListener {
                    it.isEnabled = false
                    getResultsFromApi_Restore()
                    it.isEnabled = true
                }
            }

            POSITION_LOAD_LIBRARY_SCREEN -> {
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
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                deleteDir(File(dir, children[i]))
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
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


    //region Google Drive APIs

    @Suppress("FunctionName")
    private fun getResultsFromApi_Backup() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices(REQUEST_GOOGLE_PLAY_SERVICES_BACKUP)
        } else if (credential.selectedAccountName == null) {
            chooseAccount_Backup()
        } else if (!Utils.checkNetwork(this@GeneralSettingsActivity)) {
            showDialog(content = "No Internet Connection!")
        } else {
            backupToGoogleDrive()
        }
    }

    @Suppress("FunctionName")
    private fun getResultsFromApi_Restore() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices(REQUEST_GOOGLE_PLAY_SERVICES_RESTORE)
        } else if (credential.selectedAccountName == null) {
            chooseAccount_Restore()
        } else if (!Utils.checkNetwork(this@GeneralSettingsActivity)) {
            showDialog(content = "No Internet Connection!")
        } else {
            restoreFromGoogleDrive()
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        return connectionStatusCode == ConnectionResult.SUCCESS
    }

    private fun acquireGooglePlayServices(requestCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode, requestCode)
        }
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int, requestCode: Int) {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val dialog = apiAvailability.getErrorDialog(this@GeneralSettingsActivity, connectionStatusCode, requestCode)
        dialog.show()
    }

    @Suppress("FunctionName")
    private fun chooseAccount_Backup() {
        Mayi.withActivity(this@GeneralSettingsActivity)
            .withPermission(Manifest.permission.GET_ACCOUNTS)
            .onResult {
                if (it.isGranted) {

                    val accountName = dataCenter.googleAccountName
                    if (accountName.isNotEmpty()) {
                        credential.selectedAccountName = accountName
                        getResultsFromApi_Backup()
                    } else {
                        // Start a dialog from which the user can choose an account
                        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER_BACKUP)
                    }
                } else
                    showDialog(content = "Enable \"Get Accounts\" permission for Novel Library " +
                        "from your device Settings -> Applications -> Novel Library -> Permissions")
            }.check()

    }

    @Suppress("FunctionName")
    private fun chooseAccount_Restore() {
        Mayi.withActivity(this@GeneralSettingsActivity)
            .withPermission(Manifest.permission.GET_ACCOUNTS)
            .onResult {
                if (it.isGranted) {

                    val accountName = dataCenter.googleAccountName
                    if (accountName.isNotEmpty()) {
                        credential.selectedAccountName = accountName
                        getResultsFromApi_Restore()
                    } else {
                        // Start a dialog from which the user can choose an account
                        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER_RESTORE)
                    }
                } else
                    showDialog(content = "Enable \"Get Accounts\" permission for Novel Library " +
                        "from your device Settings -> Applications -> Novel Library -> Permissions")
            }.check()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GOOGLE_PLAY_SERVICES_BACKUP -> {
                if (resultCode != RESULT_OK) {
                    showDialog(content =
                    "This app requires Google Play Services. Please install " +
                        "Google Play Services on your device and relaunch this app.")
                } else {
                    getResultsFromApi_Backup()
                }
            }

            REQUEST_GOOGLE_PLAY_SERVICES_RESTORE -> {
                if (resultCode != RESULT_OK) {
                    showDialog(content =
                    "This app requires Google Play Services. Please install " +
                        "Google Play Services on your device and relaunch this app.")
                } else {
                    getResultsFromApi_Restore()
                }
            }

            REQUEST_ACCOUNT_PICKER_BACKUP -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        dataCenter.googleAccountName = accountName
                        credential.selectedAccountName = accountName
                        getResultsFromApi_Backup()
                    }
                }
            }

            REQUEST_ACCOUNT_PICKER_RESTORE -> {
                if (resultCode == RESULT_OK && data != null && data.extras != null) {
                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    if (accountName != null) {
                        dataCenter.googleAccountName = accountName
                        credential.selectedAccountName = accountName
                        getResultsFromApi_Restore()
                    }
                }
            }

            REQUEST_AUTHORIZATION_BACKUP -> {
                if (resultCode == RESULT_OK) {
                    getResultsFromApi_Backup()
                }
            }

            REQUEST_AUTHORIZATION_RESTORE -> {
                if (resultCode == RESULT_OK) {
                    getResultsFromApi_Restore()
                }
            }
        }
    }

    private fun backupToGoogleDrive() {
        async {

            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service: Drive = Drive.Builder(transport, jsonFactory, credential).setApplicationName(getString(R.string.app_name)).build()

            showDialog(isProgress = true, content = "Uploading data to Google Drive...")

            try {
                val result: FileList = await {
                    service.files().list()
                        .setQ("title = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder'")
                        .setMaxResults(10)
                        .execute()
                }

                val backupFolder: com.google.api.services.drive.model.File
                if (result.items.isEmpty()) {
                    backupFolder = com.google.api.services.drive.model.File()
                    backupFolder.title = BACKUP_FOLDER_NAME
                    backupFolder.mimeType = "application/vnd.google-apps.folder"

                    val fileInsert = await { service.files().insert(backupFolder).setFields("id").execute() }
                    backupFolder.id = fileInsert.id
                } else {
                    backupFolder = result.items[0]
                }

                val data = Environment.getDataDirectory()
                val baseDir = File(data, "//data//io.github.gmathi.novellibrary")
                val currentDBsPath = File(baseDir, databasesDirName)
                //val currentFilesDir = File(baseDir, filesDirName)
                val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)

                //Backup Databases
                if (currentDBsPath.exists() && currentDBsPath.isDirectory) {

                    val resultDbFolder: FileList = await {
                        service.files().list()
                            .setQ("title = '$databasesDirName' and mimeType = 'application/vnd.google-apps.folder' and parents in '${backupFolder.id}'")
                            .setMaxResults(10).execute()
                    }

                    val dbFolder: com.google.api.services.drive.model.File
                    if (resultDbFolder.items.isEmpty()) {
                        dbFolder = com.google.api.services.drive.model.File()
                        dbFolder.title = databasesDirName
                        dbFolder.mimeType = "application/vnd.google-apps.folder"
                        dbFolder.parents = Collections.singletonList(ParentReference().setId(backupFolder.id))

                        val fileInsert = await { service.files().insert(dbFolder).setFields("id").execute() }
                        dbFolder.id = fileInsert.id
                    } else {
                        dbFolder = resultDbFolder.items[0]
                        val filesToDelete = await { service.files().list().setQ("parents in '${dbFolder.id}'").setMaxResults(30).execute() }
                        filesToDelete.items.forEach {
                            await { service.files().delete(it.id).execute() }
                        }
                    }

                    currentDBsPath.listFiles().forEach {
                        val fileToUpload = com.google.api.services.drive.model.File()
                        fileToUpload.title = it.name
                        fileToUpload.parents = Collections.singletonList(ParentReference().setId(dbFolder.id))
                        val mediaContent = FileContent("application/x-sqlite3", it)
                        await { service.files().insert(fileToUpload, mediaContent).setFields("id, parents").execute() }
                    }
                }

                //Backup Shared Preferences
                // --> /data/data/io.github.gmathi.novellibrary/shared_prefs
                if (currentSharedPrefsPath.exists() && currentSharedPrefsPath.isDirectory) {

                    val resultSharedPrefFolder: FileList = await {
                        service.files().list()
                            .setQ("title = '$sharedPrefsDirName' and mimeType = 'application/vnd.google-apps.folder' and parents in '${backupFolder.id}'")
                            .setMaxResults(10).execute()
                    }

                    val sharedPrefFolder: com.google.api.services.drive.model.File
                    if (resultSharedPrefFolder.items.isEmpty()) {
                        sharedPrefFolder = com.google.api.services.drive.model.File()
                        sharedPrefFolder.title = sharedPrefsDirName
                        sharedPrefFolder.mimeType = "application/vnd.google-apps.folder"
                        sharedPrefFolder.parents = Collections.singletonList(ParentReference().setId(backupFolder.id))

                        val fileInsert = await { service.files().insert(sharedPrefFolder).setFields("id").execute() }
                        sharedPrefFolder.id = fileInsert.id
                    } else {
                        sharedPrefFolder = resultSharedPrefFolder.items[0]
                        val filesToDelete = await { service.files().list().setQ("parents in '${sharedPrefFolder.id}'").setMaxResults(30).execute() }
                        filesToDelete.items.forEach {
                            await { service.files().delete(it.id).execute() }
                        }
                    }

                    currentSharedPrefsPath.listFiles().forEach {
                        val fileToUpload = com.google.api.services.drive.model.File()
                        fileToUpload.title = it.name
                        fileToUpload.parents = Collections.singletonList(ParentReference().setId(sharedPrefFolder.id))
                        val mediaContent = FileContent("application/xml", it)
                        await { service.files().insert(fileToUpload, mediaContent).setFields("id, parents").execute() }
                    }
                }

                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Backup Successful")

            } catch (e: Exception) {
                when (e) {
                    is GooglePlayServicesAvailabilityIOException -> showGooglePlayServicesAvailabilityErrorDialog(e.connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES_BACKUP)
                    is UserRecoverableAuthIOException -> startActivityForResult(e.intent, REQUEST_AUTHORIZATION_BACKUP)
                    else -> e.printStackTrace()
                }
                showDialog(content = "Backup Failed!")
            }

        }
    }

    private fun restoreFromGoogleDrive() {
        async {

            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service: Drive = Drive.Builder(transport, jsonFactory, credential).setApplicationName(getString(R.string.app_name)).build()

            showDialog(isProgress = true, content = "Restoring data from Google Drive...")

            try {
                val result: FileList = await {
                    service.files().list()
                        .setQ("title = '$BACKUP_FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder'")
                        .setMaxResults(10).execute()
                }

                val backupFolder: com.google.api.services.drive.model.File
                if (result.items.isEmpty()) {
                    showDialog(content = "Not backup found!")
                    return@async
                } else {
                    backupFolder = result.items[0]
                }

                val data = Environment.getDataDirectory()
                val baseDir = File(data, "//data//io.github.gmathi.novellibrary")
                val currentDBsPath = File(baseDir, databasesDirName)
                val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)

                //Restore Databases from Google Drive

                val resultDbFolder: FileList = await {
                    service.files().list()
                        .setQ("title = '$databasesDirName' and mimeType = 'application/vnd.google-apps.folder' and parents in '${backupFolder.id}'")
                        .setMaxResults(10).execute()
                }

                val dbFolder: com.google.api.services.drive.model.File
                if (!resultDbFolder.items.isEmpty()) {
                    if (!currentDBsPath.exists()) currentDBsPath.mkdir()

                    dbFolder = resultDbFolder.items[0]
                    val filesToCopy = await { service.files().list().setQ("parents in '${dbFolder.id}'").setMaxResults(30).execute() }
                    filesToCopy.items.forEach {
                        val inputStream = await { downloadFile(service, it) }
                        if (inputStream != null) {
                            val file = File(currentDBsPath, it.title)
                            // if (file.exists()) file.delete()
                            await { Utils.copyFile(inputStream, file) }
                        }
                    }
                }


                //Restore Shared Preferences from Google Drive
                // --> /data/data/io.github.gmathi.novellibrary/shared_prefs
                if (currentSharedPrefsPath.exists() && currentSharedPrefsPath.isDirectory) {

                    val resultSharedPrefFolder: FileList = await {
                        service.files().list()
                            .setQ("title = '$sharedPrefsDirName' and mimeType = 'application/vnd.google-apps.folder' and parents in '${backupFolder.id}'")
                            .setMaxResults(10).execute()
                    }

                    val sharedPrefFolder: com.google.api.services.drive.model.File
                    if (!resultSharedPrefFolder.items.isEmpty()) {
                        if (!currentSharedPrefsPath.exists()) currentSharedPrefsPath.mkdir()

                        sharedPrefFolder = resultSharedPrefFolder.items[0]
                        val filesToCopy = await { service.files().list().setQ("parents in '${sharedPrefFolder.id}'").setMaxResults(30).execute() }
                        filesToCopy.items.forEach {
                            val inputStream = await { downloadFile(service, it) }
                            if (inputStream != null) {
                                val file = File(currentDBsPath, it.title)
                                //     if (file.exists()) file.delete()
                                await { Utils.copyFile(inputStream, file) }
                            }
                        }
                    }
                }

                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Restore Successful")

            } catch (e: Exception) {
                when (e) {
                    is GooglePlayServicesAvailabilityIOException -> showGooglePlayServicesAvailabilityErrorDialog(e.connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES_RESTORE)
                    is UserRecoverableAuthIOException -> startActivityForResult(e.intent, REQUEST_AUTHORIZATION_RESTORE)
                    else -> {
                        showDialog(content = "Restore Failed!")
                        e.printStackTrace()
                    }
                }
            }

        }
    }

    private fun downloadFile(service: Drive, file: com.google.api.services.drive.model.File): InputStream? {
        if (file.downloadUrl != null && file.downloadUrl.isNotEmpty()) {
            try {
                val resp = service.requestFactory.buildGetRequest(GenericUrl(file.downloadUrl)).execute()
                return resp.content
            } catch (e: SSLPeerUnverifiedException) {
                val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
                val m = p.matcher(e.localizedMessage)
                if (m.find()) {
                    val hostName = m.group(1)
                    if (!HostNames.isVerifiedHost(hostName)) {
                        dataCenter.saveVerifiedHost(m.group(1))
                        return downloadFile(service, file)
                    }
                }
            }
            return null
        } else {
            return null
        }
    }

//endregion
}
