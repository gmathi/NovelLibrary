package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Network & Privacy — consolidates the networking items that were stranded in
 * the legacy General screen: DNS over HTTPS, Forced WebView Domains, and the
 * Clear Cloudflare Cookies action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkPrivacySettingsScreen(
    dohProviderIndex: Int,
    dohProviderNames: List<String>,
    onDohProviderChange: (Int) -> Unit,
    onOpenForcedWebViewDomains: () -> Unit,
    onClearCloudflareCookies: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    var showDnsDialog by remember { mutableStateOf(false) }
    var showClearCookiesDialog by remember { mutableStateOf(false) }

    val currentDoh = dohProviderNames.getOrNull(dohProviderIndex) ?: dohProviderNames.firstOrNull() ?: ""

    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("DNS over HTTPS provider") },
            text = {
                LazyColumn {
                    items(dohProviderNames) { name ->
                        val index = dohProviderNames.indexOf(name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDohProviderChange(index)
                                    showDnsDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == dohProviderIndex,
                                onClick = {
                                    onDohProviderChange(index)
                                    showDnsDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDnsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearCookiesDialog) {
        AlertDialog(
            onDismissRequest = { showClearCookiesDialog = false },
            title = { Text("Clear Cloudflare cookies") },
            text = { Text("This removes stored Cloudflare clearance cookies for all known hosts. You may need to pass the Cloudflare check again. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    onClearCloudflareCookies()
                    showClearCookiesDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCookiesDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network & Privacy") },
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
            item { SettingsSectionHeader(title = "Network") }
            item {
                ChevronSettingRow(
                    title = "DNS over HTTPS provider",
                    subtitle = currentDoh,
                    onClick = { showDnsDialog = true }
                )
            }
            item {
                ChevronSettingRow(
                    title = "Forced WebView Domains",
                    subtitle = "Domains always loaded via WebView",
                    onClick = onOpenForcedWebViewDomains
                )
            }

            item { SettingsSectionHeader(title = "Privacy") }
            item {
                Column {
                    ActionSettingRow(
                        title = "Clear Cloudflare cookies",
                        subtitle = "Remove stored Cloudflare clearance cookies",
                        isDestructive = true,
                        onClick = { showClearCookiesDialog = true }
                    )
                }
            }
        }
    }
}
