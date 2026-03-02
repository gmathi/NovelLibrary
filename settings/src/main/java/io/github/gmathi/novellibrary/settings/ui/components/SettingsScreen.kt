package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview

/**
 * Base layout for all settings screens.
 * 
 * Provides a consistent scaffold with collapsing large top app bar and scrollable content.
 * Follows Android System Settings design with Material 3.
 * The large title collapses to a smaller size when scrolling down.
 * 
 * @param title The screen title shown in the top app bar (large when not scrolled)
 * @param onNavigateBack Callback invoked when the back button is pressed
 * @param modifier Modifier for the scaffold
 * @param actions Optional actions to display in the top app bar
 * @param content The settings content to display in the scrollable area
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Empty Screen", showBackground = true)
@Composable
private fun PreviewSettingsScreenEmpty() {
    MaterialTheme {
        SettingsScreen(
            title = "Settings",
            onNavigateBack = {}
        ) {
            // Empty content
        }
    }
}

@Preview(name = "Basic Content", showBackground = true)
@Composable
private fun PreviewSettingsScreenBasic() {
    MaterialTheme {
        SettingsScreen(
            title = "Reader Settings",
            onNavigateBack = {}
        ) {
            SettingsItem(
                title = "Theme",
                description = "Choose reader theme",
                onClick = {}
            )
            SettingsItem(
                title = "Text Size",
                description = "Adjust reading text size",
                onClick = {}
            )
        }
    }
}

@Preview(name = "With Sections", showBackground = true)
@Composable
private fun PreviewSettingsScreenWithSections() {
    MaterialTheme {
        SettingsScreen(
            title = "Reader Settings",
            onNavigateBack = {}
        ) {
            SettingsSection(title = "Display") {
                SettingsDropdown(
                    title = "Theme",
                    description = "Choose reader theme",
                    icon = Icons.Default.Palette,
                    selectedValue = "Sepia",
                    options = listOf("Light", "Dark", "Sepia"),
                    onOptionSelected = {}
                )
                SettingsSlider(
                    title = "Text Size",
                    description = "Adjust reading text size",
                    value = 16f,
                    onValueChange = {},
                    valueRange = 12f..24f
                )
            }
            
            SettingsSection(title = "Behavior") {
                SettingsSwitch(
                    title = "Keep Screen On",
                    description = "Prevent screen from sleeping",
                    checked = true,
                    onCheckedChange = {}
                )
                SettingsSwitch(
                    title = "Volume Key Navigation",
                    description = "Use volume keys to turn pages",
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }
    }
}

@Preview(name = "With Actions", showBackground = true)
@Composable
private fun PreviewSettingsScreenWithActions() {
    MaterialTheme {
        SettingsScreen(
            title = "Settings",
            onNavigateBack = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
            }
        ) {
            SettingsItem(
                title = "General",
                onClick = {}
            )
            SettingsItem(
                title = "Notifications",
                onClick = {}
            )
        }
    }
}

@Preview(name = "Long Content", showBackground = true, heightDp = 400)
@Composable
private fun PreviewSettingsScreenLongContent() {
    MaterialTheme {
        SettingsScreen(
            title = "All Settings",
            onNavigateBack = {}
        ) {
            repeat(10) { index ->
                SettingsItem(
                    title = "Setting ${index + 1}",
                    description = "Description for setting ${index + 1}",
                    icon = Icons.Default.Notifications,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsScreenDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {}
            ) {
                SettingsSection(title = "Display") {
                    SettingsItem(
                        title = "Theme",
                        description = "Choose reader theme",
                        icon = Icons.Default.Palette,
                        onClick = {}
                    )
                    SettingsSwitch(
                        title = "Keep Screen On",
                        description = "Prevent screen from sleeping",
                        checked = true,
                        onCheckedChange = {}
                    )
                }
            }
        }
    }
}
