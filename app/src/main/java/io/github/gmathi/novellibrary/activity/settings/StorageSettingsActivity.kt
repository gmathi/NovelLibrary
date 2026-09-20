package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.DialogStorageMigrationProgressBinding
import io.github.gmathi.novellibrary.databinding.ListitemStorageLocationBinding
import io.github.gmathi.novellibrary.util.storage.StorageLocation
import io.github.gmathi.novellibrary.util.storage.StorageLocationResolver
import io.github.gmathi.novellibrary.util.storage.StorageMigrator
import io.github.gmathi.novellibrary.util.storage.StorageOption
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaults
import kotlinx.coroutines.launch
import java.util.ArrayList

/**
 * Settings screen for choosing the download storage location (internal vs. an available SD
 * card volume). Mirrors the classic `BaseActivity` + `GenericAdapter` pattern used by
 * [GeneralSettingsActivity].
 */
class StorageSettingsActivity : BaseActivity(), GenericAdapter.Listener<StorageOption> {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var adapter: GenericAdapter<StorageOption>

    /** True while a migration triggered from this screen is in flight, to ignore stray taps. */
    private var isMigrating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.download_storage_location)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        val options = ArrayList(StorageLocationResolver.listOptions(this, dataCenter))
        adapter = GenericAdapter(items = options, layoutResId = R.layout.listitem_storage_location, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    private fun refreshOptions() {
        adapter.updateData(ArrayList(StorageLocationResolver.listOptions(this, dataCenter)))
    }

    override fun bind(item: StorageOption, itemView: View, position: Int) {
        val itemBinding = ListitemStorageLocationBinding.bind(itemView)

        itemBinding.title.text = item.label
        itemBinding.activeMarker.visibility = if (item.isActive) View.VISIBLE else View.INVISIBLE
        itemBinding.unavailableMarker.visibility = if (!item.isAvailable) View.VISIBLE else View.GONE

        if (item.isAvailable) {
            itemBinding.subtitle.visibility = View.VISIBLE
            itemBinding.subtitle.text = getString(
                R.string.storage_free_of_total,
                Formatter.formatShortFileSize(this, item.freeBytes),
                Formatter.formatShortFileSize(this, item.totalBytes)
            )
        } else {
            itemBinding.subtitle.visibility = View.VISIBLE
            itemBinding.subtitle.text = getString(R.string.storage_location_unavailable_label)
        }

        // "No removable storage detected" note: only Internal is present (no SD options at all).
        val hasAnySdOption = adapter.items.any { it.location is StorageLocation.SdCard }
        if (item.location is StorageLocation.Internal && !hasAnySdOption) {
            itemBinding.note.visibility = View.VISIBLE
            itemBinding.note.text = getString(R.string.storage_no_removable_storage_detected)
        } else {
            itemBinding.note.visibility = View.GONE
        }
    }

    override fun onItemClick(item: StorageOption, position: Int) {
        if (isMigrating) return

        // Selecting the already-active location is a no-op (Req 2.6).
        if (item.isActive) return

        confirmLocationChange(item)
    }

    private fun confirmLocationChange(target: StorageOption) {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_change_confirm_title)
            .setMessage(getString(R.string.storage_change_confirm_message, target.label))
            .setPositiveButton(R.string.okay) { dialog, _ ->
                dialog.dismiss()
                startMigration(target)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startMigration(target: StorageOption) {
        val fromResolved = StorageLocationResolver.resolveWritableRoot(this, dataCenter)
        val toResolved = StorageLocationResolver.resolveWritableRoot(
            configured = target.location,
            internalRoot = filesDir,
            candidateDirs = ContextCompat.getExternalFilesDirs(this, null).filterNotNull()
        )

        isMigrating = true

        val progressBinding = DialogStorageMigrationProgressBinding.inflate(layoutInflater)
        progressBinding.migrationProgressText.text = getString(R.string.storage_migration_progress_message, 0, 0)
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.storage_migration_progress_title)
            .setView(progressBinding.root)
            .setCancelable(false)
            .show()

        val migrator = StorageMigrator(applicationContext, dbHelper, dataCenter)

        lifecycleScope.launch {
            val result = migrator.migrate(fromResolved, toResolved) { done, total ->
                runOnUiThread {
                    updateProgress(progressBinding.migrationProgressBar, progressBinding.migrationProgressText, done, total)
                }
            }

            progressDialog.dismiss()
            isMigrating = false
            handleMigrationResult(result, target)
        }
    }

    private fun updateProgress(progressBar: ProgressBar, progressText: TextView, done: Int, total: Int) {
        val safeTotal = if (total <= 0) 1 else total
        progressBar.max = safeTotal
        progressBar.progress = done
        progressText.text = getString(R.string.storage_migration_progress_message, done, total)
    }

    private fun handleMigrationResult(result: StorageMigrator.MigrationResult, target: StorageOption) {
        when (result) {
            is StorageMigrator.MigrationResult.Success -> {
                refreshOptions()
                Toast.makeText(this, getString(R.string.storage_migration_success, target.label), Toast.LENGTH_LONG).show()
            }

            is StorageMigrator.MigrationResult.InsufficientSpace -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.alert)
                    .setMessage(
                        getString(
                            R.string.storage_migration_insufficient_space,
                            Formatter.formatShortFileSize(this, result.requiredBytes),
                            Formatter.formatShortFileSize(this, result.availableBytes)
                        )
                    )
                    .setPositiveButton(R.string.okay, null)
                    .show()
            }

            is StorageMigrator.MigrationResult.PartialFailure -> {
                refreshOptions()
                AlertDialog.Builder(this)
                    .setTitle(R.string.alert)
                    .setMessage(getString(R.string.storage_migration_partial_failure, result.movedFiles, result.failedFiles))
                    .setPositiveButton(R.string.okay, null)
                    .show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}
