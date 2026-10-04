package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Storage & Backup — merges the download-location and backup/restore entries
 * that previously lived separately under the legacy General screen. Both rows
 * launch the EXISTING sub-screens; no backup logic is reimplemented here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBackupSettingsScreen(
    onOpenDownloadStorage: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Backup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                ChevronSettingRow(
                    title = "Download storage location",
                    subtitle = "Where downloaded chapters are stored",
                    onClick = onOpenDownloadStorage
                )
            }
            item {
                ChevronSettingRow(
                    title = "Backup & Restore",
                    subtitle = "Backup or restore your library and settings",
                    onClick = onOpenBackupRestore
                )
            }
        }
    }
}
