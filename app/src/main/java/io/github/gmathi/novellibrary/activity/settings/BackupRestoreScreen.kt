package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter

private const val BACKUP_FILE_NAME = "NovelLibrary.backup.zip"
private val ZIP_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "application/zip"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    dataCenter: DataCenter,
    vm: BackupRestoreViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                result.data?.flags?.let { f -> context.contentResolver.takePersistableUriPermission(uri, f and flags) }
                    ?: context.contentResolver.takePersistableUriPermission(uri, flags)
                vm.startBackup(uri)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> vm.startRestore(uri) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_and_restore)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isOperationRunning && !state.isDeletingFiles) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Progress section
            AnimatedVisibility(
                visible = state.isOperationRunning || state.isDeletingFiles,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ProgressCard(state)
            }

            Spacer(Modifier.height(8.dp))

            // Backup card
            ActionCard(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.backup_data),
                subtitle = stringResource(R.string.backup_data_description),
                enabled = !state.isOperationRunning && !state.isDeletingFiles,
                onClick = { vm.showBackupOptions() }
            )

            // Restore card
            ActionCard(
                icon = Icons.Default.CloudDownload,
                title = stringResource(R.string.restore_data),
                subtitle = stringResource(R.string.restore_data_description),
                enabled = !state.isOperationRunning && !state.isDeletingFiles,
                onClick = { vm.showRestoreOptions() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Backup frequency
            ActionCard(
                icon = Icons.Default.Schedule,
                title = stringResource(R.string.backup_frequency),
                subtitle = when (vm.backupFrequency) {
                    24 -> stringResource(R.string.backup_frequency_Daily)
                    24 * 7 -> stringResource(R.string.backup_frequency_Weekly)
                    else -> stringResource(R.string.backup_frequency_manual)
                },
                enabled = !state.isOperationRunning && !state.isDeletingFiles,
                onClick = { vm.showFrequencyDialog() }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Clear data
            ActionCard(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.internal_clear_data),
                subtitle = stringResource(R.string.internal_clear_data_description),
                enabled = !state.isOperationRunning && !state.isDeletingFiles,
                onClick = { vm.showClearDataDialog() },
                tintColor = MaterialTheme.colorScheme.error
            )
        }
    }

    // Dialogs
    if (state.showBackupOptionsDialog) {
        OptionsDialog(
            title = stringResource(R.string.backup_data),
            state = state,
            onOptionChange = { s, d, p, f -> vm.updateOption(s, d, p, f) },
            onConfirm = {
                vm.dismissDialog()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(ZIP_MIME_TYPE)
                    .putExtra(Intent.EXTRA_TITLE, BACKUP_FILE_NAME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                backupLauncher.launch(intent)
            },
            onDismiss = { vm.dismissDialog() }
        )
    }

    if (state.showRestoreOptionsDialog) {
        OptionsDialog(
            title = stringResource(R.string.restore_data),
            state = state,
            onOptionChange = { s, d, p, f -> vm.updateOption(s, d, p, f) },
            onConfirm = {
                vm.dismissDialog()
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(ZIP_MIME_TYPE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                restoreLauncher.launch(intent)
            },
            onDismiss = { vm.dismissDialog() }
        )
    }

    if (state.showFrequencyDialog) {
        FrequencyDialog(
            currentFrequency = vm.backupFrequency,
            onSelect = { vm.updateBackupFrequency(it) },
            onDismiss = { vm.dismissDialog() }
        )
    }

    if (state.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissDialog() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.clear_data)) },
            text = { Text(stringResource(R.string.clear_data_description)) },
            confirmButton = {
                TextButton(onClick = { vm.clearData() }) {
                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (state.showResultDialog) {
        ResultDialog(
            message = state.resultMessage ?: "",
            isSuccess = state.isSuccess ?: false,
            onDismiss = { vm.dismissResult() }
        )
    }
}


@Composable
private fun ProgressCard(state: BackupRestoreUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isDeletingFiles) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${stringResource(R.string.clearing_data)}…",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = if (state.operationType == OperationType.BACKUP) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (state.operationType == OperationType.BACKUP) stringResource(R.string.backup) else stringResource(R.string.restore),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!state.isDeletingFiles) {
                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    drawStopIndicator = {}
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.currentStep,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tintColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tintColor else tintColor.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun OptionsDialog(
    title: String,
    state: BackupRestoreUiState,
    onOptionChange: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                CheckboxRow(stringResource(R.string.simple_text_backup), state.simpleText) { onOptionChange(it, null, null, null) }
                CheckboxRow(stringResource(R.string.title_library), state.database) { onOptionChange(null, it, null, null) }
                CheckboxRow(stringResource(R.string.preferences), state.preferences) { onOptionChange(null, null, it, null) }
                CheckboxRow(stringResource(R.string.downloaded_files), state.files) { onOptionChange(null, null, null, it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = state.simpleText || state.database || state.preferences || state.files
            ) { Text(stringResource(R.string.okay)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FrequencyDialog(
    currentFrequency: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to stringResource(R.string.backup_frequency_manual),
        24 to stringResource(R.string.backup_frequency_Daily),
        24 * 7 to stringResource(R.string.backup_frequency_Weekly)
    )
    var selected by remember { mutableIntStateOf(currentFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_frequency)) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == value, onClick = { selected = value })
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) { Text(stringResource(R.string.okay)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ResultDialog(message: String, isSuccess: Boolean, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.okay)) }
        }
    )
}
