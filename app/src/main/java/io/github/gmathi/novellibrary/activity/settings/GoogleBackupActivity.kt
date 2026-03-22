package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.common.R as CommonR
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.common.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.util.R as UtilR
import io.github.gmathi.novellibrary.common.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.network.GoogleDriveHelper
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.lang.launchIO
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.worker.oneTimeGoogleDriveBackupWorkRequest
import io.github.gmathi.novellibrary.worker.oneTimeGoogleDriveRestoreWorkRequest

class GoogleBackupActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "GoogleBackupActivity"
    }

    private lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var driveHelper: GoogleDriveHelper

    private var pendingAction: PendingAction? = null

    private enum class PendingAction { BACKUP, RESTORE }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                onSignInSuccess(account)
            } catch (e: ApiException) {
                Logs.error(TAG, "Sign-in failed: ${e.statusCode}", e)
                showDialog(content = getString(R.string.google_sign_in_failed))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        driveHelper = GoogleDriveHelper(this)
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        refreshDescriptions()
    }

    private fun setRecyclerView() {
        settingsItems = arrayListOf(
            getString(R.string.google_account),
            getString(R.string.google_drive_backup),
            getString(R.string.google_drive_restore),
            getString(R.string.google_drive_backup_info),
            getString(R.string.google_sign_out)
        )
        settingsItemsDescription = arrayListOf(
            getAccountDescription(),
            getString(R.string.google_drive_backup_description),
            getString(R.string.google_drive_restore_description),
            getString(R.string.google_drive_backup_info_description),
            getString(R.string.google_sign_out_description)
        )
        adapter = GenericAdapter(
            items = settingsItems,
            layoutResId = CommonR.layout.listitem_title_subtitle_widget,
            listener = this
        )
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(
            CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    private fun getAccountDescription(): String {
        val account = driveHelper.getSignedInAccount()
        return account?.email ?: getString(R.string.google_not_signed_in)
    }

    private fun refreshDescriptions() {
        settingsItemsDescription[0] = getAccountDescription()
        if (driveHelper.isSignedIn()) {
            launchIO {
                val infoResult = driveHelper.getBackupInfo()
                launchUI {
                    val info = infoResult.getOrNull()
                    settingsItemsDescription[3] = if (info != null) {
                        getString(R.string.google_drive_last_backup_info, info.getFormattedTime(), info.getFormattedSize())
                    } else {
                        getString(R.string.google_drive_no_backup_found)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
        } else {
            settingsItemsDescription[3] = getString(R.string.google_drive_backup_info_description)
            adapter.notifyDataSetChanged()
        }
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
            getString(R.string.google_account) -> {
                if (driveHelper.isSignedIn()) {
                    showDialog(content = getString(R.string.google_already_signed_in, driveHelper.getSignedInAccount()?.email ?: ""))
                } else {
                    signIn()
                }
            }
            getString(R.string.google_drive_backup) -> {
                if (!driveHelper.isSignedIn()) {
                    pendingAction = PendingAction.BACKUP
                    signIn()
                    return
                }
                showBackupOptionsDialog()
            }
            getString(R.string.google_drive_restore) -> {
                if (!driveHelper.isSignedIn()) {
                    pendingAction = PendingAction.RESTORE
                    signIn()
                    return
                }
                showRestoreOptionsDialog()
            }
            getString(R.string.google_drive_backup_info) -> {
                refreshDescriptions()
            }
            getString(R.string.google_sign_out) -> {
                if (driveHelper.isSignedIn()) {
                    MaterialDialog(this).show {
                        title(R.string.confirm_action)
                        message(R.string.google_sign_out_confirm)
                        positiveButton(R.string.okay) {
                            driveHelper.getSignInClient().signOut().addOnCompleteListener {
                                dataCenter.gdAccountEmail = "-"
                                refreshDescriptions()
                            }
                        }
                        negativeButton(R.string.cancel)
                    }
                } else {
                    showDialog(content = getString(R.string.google_not_signed_in))
                }
            }
        }
    }

    private fun signIn() {
        signInLauncher.launch(driveHelper.getSignInClient().signInIntent)
    }

    private fun onSignInSuccess(account: GoogleSignInAccount) {
        dataCenter.gdAccountEmail = account.email ?: "-"
        refreshDescriptions()
        when (pendingAction) {
            PendingAction.BACKUP -> showBackupOptionsDialog()
            PendingAction.RESTORE -> showRestoreOptionsDialog()
            null -> {}
        }
        pendingAction = null
    }

    private fun showBackupOptionsDialog() {
        MaterialDialog(this).show {
            title(R.string.google_drive_backup)
            listItemsMultiChoice(
                R.array.backup_and_restore_options,
                initialSelection = intArrayOf(0, 1, 2, 3)
            ) { _, which, _ ->
                if (which.isNotEmpty()) {
                    val workRequest = oneTimeGoogleDriveBackupWorkRequest(
                        shouldBackupSimpleText = which.contains(0),
                        shouldBackupDatabase = which.contains(1),
                        shouldBackupPreferences = which.contains(2),
                        shouldBackupFiles = which.contains(3)
                    )
                    executeWorkRequest(workRequest)
                }
            }
            positiveButton(R.string.okay)
        }
    }

    private fun showRestoreOptionsDialog() {
        MaterialDialog(this).show {
            title(R.string.google_drive_restore)
            message(R.string.google_drive_restore_warning)
            listItemsMultiChoice(
                R.array.backup_and_restore_options,
                initialSelection = intArrayOf(0, 1, 2, 3)
            ) { _, which, _ ->
                if (which.isNotEmpty()) {
                    val workRequest = oneTimeGoogleDriveRestoreWorkRequest(
                        shouldRestoreSimpleText = which.contains(0),
                        shouldRestoreDatabase = which.contains(1),
                        shouldRestorePreferences = which.contains(2),
                        shouldRestoreFiles = which.contains(3)
                    )
                    executeWorkRequest(workRequest)
                }
            }
            positiveButton(R.string.okay)
        }
    }

    private fun executeWorkRequest(workRequest: WorkRequest) {
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(workRequest)
        val observable = workManager.getWorkInfoByIdLiveData(workRequest.id)
        observable.observe(this) { info ->
            if (info != null && arrayOf(State.SUCCEEDED, State.FAILED, State.CANCELLED).contains(info.state)) {
                when (info.state) {
                    State.SUCCEEDED -> {
                        showDialog(
                            iconRes = R.drawable.ic_check_circle_white_vector,
                            content = info.outputData.getString(WORK_KEY_RESULT)
                        )
                        refreshDescriptions()
                    }
                    State.FAILED, State.CANCELLED -> showDialog(
                        iconRes = R.drawable.ic_close_white_vector,
                        content = info.outputData.getString(WORK_KEY_RESULT)
                    )
                    else -> {}
                }
                observable.removeObservers(this)
            }
        }
    }

    private fun showDialog(
        title: String? = null,
        content: String? = null,
        iconRes: Int = UtilR.drawable.ic_warning_white_vector
    ) {
        MaterialDialog(this).show {
            if (title != null) title(text = title)
            else title(R.string.confirm_action)
            if (content != null) message(text = content)
            icon(iconRes)
            positiveButton(R.string.okay) { it.dismiss() }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
