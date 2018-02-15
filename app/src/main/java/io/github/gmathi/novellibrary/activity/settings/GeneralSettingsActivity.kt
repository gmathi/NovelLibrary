package io.github.gmathi.novellibrary.activity.settings

import android.Manifest
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.drive.*
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
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
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class GeneralSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {

        private const val TAG = "GeneralSettingsActivity"

        private const val POSITION_BACKUP = 0
        private const val POSITION_RESTORE = 1
        private const val POSITION_CLEAR = 2
        private const val POSITION_GOOGLE_BACKUP = 3
        private const val POSITION_GOOGLE_RESTORE = 4
        private const val POSITION_LOAD_LIBRARY_SCREEN = 5

//        private val SCOPES = arrayOf(DriveScopes.DRIVE)

        const val REQUEST_ACCOUNT_PICKER_BACKUP = 1000
        const val REQUEST_ACCOUNT_PICKER_RESTORE = 2000
        const val REQUEST_AUTHORIZATION_BACKUP = 1001
        const val REQUEST_AUTHORIZATION_RESTORE = 2001
        const val REQUEST_GOOGLE_PLAY_SERVICES_BACKUP = 1002
        const val REQUEST_GOOGLE_PLAY_SERVICES_RESTORE = 2002

        private const val REQUEST_CODE_SIGN_IN_BACKUP = 1003
        private const val REQUEST_CODE_SIGN_IN_RESTORE = 2003
        private const val REQUEST_CODE_CREATOR = 2

        const val BACKUP_FOLDER_NAME = "NovelLibrary-Backup"

    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private var confirmDialog: MaterialDialog? = null
//    private lateinit var credential: GoogleAccountCredential

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mDriveClient: DriveClient? = null
    private var mDriveResourceClient: DriveResourceClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()

//        val scopesList: ArrayList<String> = ArrayList()
//        scopesList.add(DriveScopes.DRIVE)

        //credential = GoogleAccountCredential.usingOAuth2(applicationContext, scopesList).setBackOff(ExponentialBackOff())
        //signIn()
        //startMainActivity()
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
                    showDialog(content = "Still being developed!")
//                    it.isEnabled = false
//                    signInBackUp()
//                    it.isEnabled = true
                }
            }

            POSITION_GOOGLE_RESTORE -> {
                itemView.widgetButton.visibility = View.VISIBLE
                itemView.widgetButton.text = getString(R.string.restore)
                itemView.widgetButton.setOnClickListener {
                    showDialog(content = "Still being developed!")
//                    it.isEnabled = false
//                    signInRestore()
//                    it.isEnabled = true
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

    private fun signInBackUp() {
        val requiredScopes = HashSet<Scope>(2)
        requiredScopes.add(Drive.SCOPE_FILE)
        requiredScopes.add(Drive.SCOPE_APPFOLDER)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (signInAccount != null && signInAccount.grantedScopes.containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount)
            backupToGoogleDrive()
        } else {
            val signInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(Drive.SCOPE_FILE)
                    .requestScopes(Drive.SCOPE_APPFOLDER)
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN_BACKUP)
        }
    }

    private fun signInRestore() {
        val requiredScopes = HashSet<Scope>(2)
        requiredScopes.add(Drive.SCOPE_FILE)
        requiredScopes.add(Drive.SCOPE_APPFOLDER)
        val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (signInAccount != null && signInAccount.grantedScopes.containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount)
            restoreFromGoogleDrive()
        } else {
            val signInOptions =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(Drive.SCOPE_FILE)
                    .requestScopes(Drive.SCOPE_APPFOLDER)
                    .build()
            val googleSignInClient = GoogleSignIn.getClient(this, signInOptions)
            startActivityForResult(googleSignInClient.signInIntent, REQUEST_CODE_SIGN_IN_RESTORE)
        }
    }

    /**
     * Continues the sign-in process, initializing the Drive clients with the current
     * user's account.
     */
    private fun initializeDriveClient(signInAccount: GoogleSignInAccount) {
        mDriveClient = Drive.getDriveClient(applicationContext, signInAccount)
        mDriveResourceClient = Drive.getDriveResourceClient(applicationContext, signInAccount)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN_BACKUP -> {
                Utils.info(TAG, "Sign in request code")
                if (resultCode != RESULT_OK) {
                    Utils.error(TAG, "Sign-in failed. Phase 1")
                    showDialog(content = "Backup Failed! (sign-on fail)")
                    return
                }

                val getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data) as Task<GoogleSignInAccount>
                if (getAccountTask.isSuccessful) {
                    initializeDriveClient(getAccountTask.result)
                    backupToGoogleDrive()
                } else {
                    Utils.error(TAG, "Sign-in failed. Phase 2");
                    showDialog(content = "Backup Failed! (sign-on2 fail)")
                }
            }

            REQUEST_CODE_SIGN_IN_RESTORE -> {
                Utils.info(TAG, "Sign in request code")
                if (resultCode != RESULT_OK) {
                    Utils.error(TAG, "Sign-in failed. Phase 1")
                    showDialog(content = "Restore Failed! (sign-on fail)")
                    return
                }

                val getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(data) as Task<GoogleSignInAccount>
                if (getAccountTask.isSuccessful) {
                    initializeDriveClient(getAccountTask.result)
                    restoreFromGoogleDrive()
                } else {
                    Utils.error(TAG, "Sign-in failed. Phase 2");
                    showDialog(content = "Restore Failed! (sign-on2 fail)")
                }
            }

//            REQUEST_GOOGLE_PLAY_SERVICES_BACKUP -> {
//                if (resultCode != RESULT_OK) {
//                    showDialog(content =
//                    "This app requires Google Play Services. Please install " +
//                        "Google Play Services on your device and relaunch this app.")
//                } else {
//                    getResultsFromApi_Backup()
//                }
//            }
//
//            REQUEST_GOOGLE_PLAY_SERVICES_RESTORE -> {
//                if (resultCode != RESULT_OK) {
//                    showDialog(content =
//                    "This app requires Google Play Services. Please install " +
//                        "Google Play Services on your device and relaunch this app.")
//                } else {
//                    getResultsFromApi_Restore()
//                }
//            }
//
//            REQUEST_ACCOUNT_PICKER_BACKUP -> {
//                if (resultCode == RESULT_OK && data != null && data.extras != null) {
//                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
//                    if (accountName != null) {
//                        dataCenter.googleAccountName = accountName
//                        credential.selectedAccountName = accountName
//                        getResultsFromApi_Backup()
//                    }
//                }
//            }
//
//            REQUEST_ACCOUNT_PICKER_RESTORE -> {
//                if (resultCode == RESULT_OK && data != null && data.extras != null) {
//                    val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
//                    if (accountName != null) {
//                        dataCenter.googleAccountName = accountName
//                        credential.selectedAccountName = accountName
//                        getResultsFromApi_Restore()
//                    }
//                }
//            }
//
//            REQUEST_AUTHORIZATION_BACKUP -> {
//                if (resultCode == RESULT_OK) {
//                    getResultsFromApi_Backup()
//                }
//            }
//
//            REQUEST_AUTHORIZATION_RESTORE -> {
//                if (resultCode == RESULT_OK) {
//                    getResultsFromApi_Restore()
//                }
//            }
        }
    }

    private fun backupToGoogleDrive() {
        async {

            showDialog(isProgress = true, content = "Uploading data to Google Drive...")

            try {

                val backupFolderQuery = Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE), Filters.eq(SearchableField.TITLE, BACKUP_FOLDER_NAME))).build()
                val backupFolderQueryTask = mDriveResourceClient?.query(backupFolderQuery) as Task<MetadataBuffer>
                val backupFolderQueryResult = await { Tasks.await(backupFolderQueryTask, 10000, TimeUnit.MILLISECONDS) }

                val backupFolder: DriveFolder
                if (backupFolderQueryResult != null && backupFolderQueryResult.count > 0) {
                    backupFolder = backupFolderQueryResult.get(0).driveId.asDriveFolder()
                    val backupFolderChildrenQuery = Query.Builder().addFilter(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE)).build()
                    val backupFolderChildrenQueryTask = mDriveResourceClient?.queryChildren(backupFolder, backupFolderChildrenQuery) as Task<MetadataBuffer>
                    val backupFolderChildrenQueryResult = await { Tasks.await(backupFolderChildrenQueryTask, 10000, TimeUnit.MILLISECONDS) }

                    backupFolderChildrenQueryResult?.forEach {
                        if (it.isTrashable && !it.isTrashed) {
                            val trashQuery = mDriveResourceClient?.trash(it.driveId.asDriveResource()) as Task<Void>
                            await { Tasks.await(trashQuery) }
                        }
                    }

                } else {
                    val rootFolderTask = mDriveResourceClient?.rootFolder as Task<DriveFolder>
                    val rootFolder = await { Tasks.await(rootFolderTask) }
                    val newFolderMetadata = MetadataChangeSet.Builder()
                        .setTitle(BACKUP_FOLDER_NAME)
                        .setMimeType(DriveFolder.MIME_TYPE)
                        .setStarred(true)
                        .build()
                    val createFolderTask = mDriveResourceClient?.createFolder(rootFolder, newFolderMetadata) as Task<DriveFolder>
                    backupFolder = await { Tasks.await(createFolderTask) }
                }


                val data = Environment.getDataDirectory()
                val baseDir = File(data, "//data//io.github.gmathi.novellibrary")
                val currentDBsPath = File(baseDir, databasesDirName)
                val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)

                //Backup Databases
                if (currentDBsPath.exists() && currentDBsPath.isDirectory) {

                    val newFolderMetadata = MetadataChangeSet.Builder()
                        .setTitle(databasesDirName)
                        .setMimeType(DriveFolder.MIME_TYPE)
                        .setStarred(true)
                        .build()
                    val createFolderTask = mDriveResourceClient?.createFolder(backupFolder, newFolderMetadata) as Task<DriveFolder>
                    val dbFolder = await { Tasks.await(createFolderTask) }

                    currentDBsPath.listFiles().forEach {

                        val createContentsTask = mDriveResourceClient?.createContents() as Task<DriveContents>
                        val driveContents = await { Tasks.await(createContentsTask) }
                        val outputStream = driveContents.outputStream

                        val fileInputStream = FileInputStream(it)
                        val buffer = ByteArray(1024)
                        var bytesRead: Int = fileInputStream.read(buffer)
                        while (bytesRead != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesRead = fileInputStream.read(buffer)
                        }

                        val changeSet = MetadataChangeSet.Builder()
                            .setTitle(it.name)
                            .setMimeType("application/x-sqlite3")
                            .build()
                        val createFileTask = mDriveResourceClient?.createFile(dbFolder, changeSet, driveContents) as Task<DriveFile>
                        val createFileTaskResult = await { Tasks.await(createFileTask) }
                        Utils.info(TAG, createFileTaskResult.toString())
                    }
                }

                //Backup Shared Preferences
                // --> /data/data/io.github.gmathi.novellibrary/shared_prefs
                if (currentSharedPrefsPath.exists() && currentSharedPrefsPath.isDirectory) {

                    val newFolderMetadata = MetadataChangeSet.Builder()
                        .setTitle(sharedPrefsDirName)
                        .setMimeType(DriveFolder.MIME_TYPE)
                        .setStarred(true)
                        .build()
                    val createFolderTask = mDriveResourceClient?.createFolder(backupFolder, newFolderMetadata) as Task<DriveFolder>
                    val sharedPrefFolder = await { Tasks.await(createFolderTask) }

                    currentSharedPrefsPath.listFiles().forEach {

                        val createContentsTask = mDriveResourceClient?.createContents() as Task<DriveContents>
                        val driveContents = await { Tasks.await(createContentsTask) }
                        val outputStream = driveContents.outputStream

                        val fileInputStream = FileInputStream(it)
                        val buffer = ByteArray(1024)
                        var bytesRead: Int = fileInputStream.read(buffer)
                        while (bytesRead != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            bytesRead = fileInputStream.read(buffer)
                        }

                        val changeSet = MetadataChangeSet.Builder()
                            .setTitle(it.name)
                            .setMimeType("application/xml")
                            .build()
                        val createFileTask = mDriveResourceClient?.createFile(sharedPrefFolder, changeSet, driveContents) as Task<DriveFile>
                        val createFileTaskResult = await { Tasks.await(createFileTask) }
                        Utils.info(TAG, createFileTaskResult.toString())
                    }
                }

                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Backup Successful")

            } catch (e: Exception) {
                e.printStackTrace()
                showDialog(content = "Backup Failed!")

            }

        }
    }

    private fun restoreFromGoogleDrive() {
        async {

            showDialog(isProgress = true, content = "Restoring data from Google Drive...")

            try {
                val backupFolderQuery = Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE), Filters.eq(SearchableField.TITLE, BACKUP_FOLDER_NAME))).build()
                val backupFolderQueryTask = mDriveResourceClient?.query(backupFolderQuery) as Task<MetadataBuffer>
                val backupFolderQueryResult = await { Tasks.await(backupFolderQueryTask, 10000, TimeUnit.MILLISECONDS) }

                val backupFolder: DriveFolder
                if (backupFolderQueryResult != null && backupFolderQueryResult.count > 0) {
                    backupFolder = backupFolderQueryResult.get(0).driveId.asDriveFolder()
                } else {
                    showDialog(content = "Not backup found!")
                    return@async
                }

                val data = Environment.getDataDirectory()
                val baseDir = File(data, "//data//io.github.gmathi.novellibrary")
                val currentDBsPath = File(baseDir, databasesDirName)
                val currentSharedPrefsPath = File(baseDir, sharedPrefsDirName)

                //Restore Databases from Google Drive

                val dbFolderQuery = Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE), Filters.eq(SearchableField.TITLE, databasesDirName))).build()
                val dbFolderQueryTask = mDriveResourceClient?.queryChildren(backupFolder, dbFolderQuery) as Task<MetadataBuffer>
                val dbFolderQueryResult = await { Tasks.await(dbFolderQueryTask, 10000, TimeUnit.MILLISECONDS) }

                val dbFolder: DriveFolder
                if (dbFolderQueryResult != null && dbFolderQueryResult.count > 0) {

                    dbFolder = dbFolderQueryResult.get(0).driveId.asDriveFolder()
                    if (!currentDBsPath.exists()) currentDBsPath.mkdir()

                    val dbFolderChildrenQuery = Query.Builder().build()
                    val dbFolderChildrenQueryTask = mDriveResourceClient?.queryChildren(dbFolder, dbFolderChildrenQuery) as Task<MetadataBuffer>
                    val dbFolderChildrenQueryResult = await { Tasks.await(dbFolderChildrenQueryTask, 10000, TimeUnit.MILLISECONDS) }

                    dbFolderChildrenQueryResult?.forEach {
                        if (!it.isTrashed && !it.isFolder) {

                            val openFileTask = mDriveResourceClient?.openFile(it.driveId.asDriveFile(), DriveFile.MODE_READ_ONLY) as Task<DriveContents>
                            val fileContents = await { Tasks.await(openFileTask) }

                            val file = File(currentDBsPath, it.title)
                            await { Utils.copyFile(fileContents.inputStream, file) }
                        }
                    }
                }

                //Restore Shared Preferences from Google Drive
                // --> /data/data/io.github.gmathi.novellibrary/shared_prefs

                val sharedPrefFolderQuery = Query.Builder().addFilter(Filters.and(Filters.eq(SearchableField.MIME_TYPE, DriveFolder.MIME_TYPE), Filters.eq(SearchableField.TITLE, sharedPrefsDirName))).build()
                val sharedPrefFolderQueryTask = mDriveResourceClient?.queryChildren(backupFolder, sharedPrefFolderQuery) as Task<MetadataBuffer>
                val sharedPrefFolderQueryResult = await { Tasks.await(sharedPrefFolderQueryTask, 10000, TimeUnit.MILLISECONDS) }

                val sharedPrefFolder: DriveFolder
                if (sharedPrefFolderQueryResult != null && sharedPrefFolderQueryResult.count > 0) {

                    sharedPrefFolder = sharedPrefFolderQueryResult.get(0).driveId.asDriveFolder()
                    if (!currentSharedPrefsPath.exists()) currentSharedPrefsPath.mkdir()

                    val sharedPrefFolderChildrenQuery = Query.Builder().build()
                    val sharedPrefFolderChildrenQueryTask = mDriveResourceClient?.queryChildren(sharedPrefFolder, sharedPrefFolderChildrenQuery) as Task<MetadataBuffer>
                    val sharedPrefFolderChildrenQueryResult = await { Tasks.await(sharedPrefFolderChildrenQueryTask, 10000, TimeUnit.MILLISECONDS) }

                    sharedPrefFolderChildrenQueryResult?.forEach {
                        if (!it.isTrashed && !it.isFolder) {

                            val openFileTask = mDriveResourceClient?.openFile(it.driveId.asDriveFile(), DriveFile.MODE_READ_ONLY) as Task<DriveContents>
                            val fileContents = await { Tasks.await(openFileTask) }

                            val file = File(currentSharedPrefsPath, it.title)
                            await { Utils.copyFile(fileContents.inputStream, file) }
                        }
                    }
                }

                showDialog(iconRes = R.drawable.ic_info_white_vector, content = "Restore Successful")

            } catch (e: Exception) {
                showDialog(content = "Restore Failed!")
                e.printStackTrace()
            }

        }
    }

//    private fun downloadFile(service: Drive, file: com.google.api.services.drive.model.File): InputStream? {
//        if (file.downloadUrl != null && file.downloadUrl.isNotEmpty()) {
//            try {
//                val resp = service.requestFactory.buildGetRequest(GenericUrl(file.downloadUrl)).execute()
//                return resp.content
//            } catch (e: SSLPeerUnverifiedException) {
//                val p = Pattern.compile("Hostname\\s(.*?)\\snot", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
//                val m = p.matcher(e.localizedMessage)
//                if (m.find()) {
//                    val hostName = m.group(1)
//                    if (!HostNames.isVerifiedHost(hostName)) {
//                        dataCenter.saveVerifiedHost(m.group(1))
//                        return downloadFile(service, file)
//                    }
//                }
//            }
//            return null
//        } else {
//            return null
//        }
//    }

//endregion
}
