package io.github.gmathi.novellibrary.compose.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MergeType
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.viewmodel.ReaderUiState
import io.github.gmathi.novellibrary.viewmodel.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsPanel(
    uiState: ReaderUiState,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
    onMoreSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Reader Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Text Size Section
            TextSizeSection(
                textSize = uiState.textSize,
                onTextSizeChange = { viewModel.setTextSize(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Theme Section
            ThemeSection(
                isDarkTheme = uiState.isDarkTheme,
                dayBgColor = uiState.dayBackgroundColor,
                dayTextColor = uiState.dayTextColor,
                nightBgColor = uiState.nightBackgroundColor,
                nightTextColor = uiState.nightTextColor,
                onToggleTheme = { viewModel.toggleDarkTheme() }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Quick Toggles
            SectionHeader("Reading")

            SettingToggle(
                icon = Icons.Outlined.AutoStories,
                title = "Reader Mode",
                subtitle = "Clean page for better reading",
                checked = uiState.isReaderMode,
                onCheckedChange = { viewModel.setReaderMode(it) }
            )

            SettingToggle(
                icon = Icons.Outlined.Code,
                title = "JavaScript",
                subtitle = "Enable page scripts",
                checked = uiState.isJavascriptEnabled,
                onCheckedChange = { viewModel.setJavascriptEnabled(it) }
            )

            SettingToggle(
                icon = Icons.Outlined.PhotoSizeSelectLarge,
                title = "Limit Image Width",
                subtitle = "Fit images to screen width",
                checked = uiState.limitImageWidth,
                onCheckedChange = { viewModel.setLimitImageWidth(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            SectionHeader("Display")

            SettingToggle(
                icon = Icons.Outlined.ScreenLockPortrait,
                title = "Keep Screen On",
                subtitle = "Prevent screen from turning off",
                checked = uiState.keepScreenOn,
                onCheckedChange = { viewModel.setKeepScreenOn(it) }
            )

            SettingToggle(
                icon = Icons.Outlined.Fullscreen,
                title = "Immersive Mode",
                subtitle = "Hide system bars while reading",
                checked = uiState.immersiveMode,
                onCheckedChange = { viewModel.setImmersiveMode(it) }
            )

            SettingToggle(
                icon = Icons.Outlined.VerticalAlignBottom,
                title = "Show Navbar at Chapter End",
                subtitle = "Show navigation at bottom of chapter",
                checked = uiState.showNavbarAtChapterEnd,
                onCheckedChange = { viewModel.setShowNavbarAtChapterEnd(it) }
            )

            SettingToggle(
                icon = Icons.AutoMirrored.Outlined.MergeType,
                title = "Merge Pages",
                subtitle = "Combine multi-page chapters",
                checked = uiState.enableClusterPages,
                onCheckedChange = { viewModel.setEnableClusterPages(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // More Settings — navigates to the full ReaderSettingsActivity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onMoreSettings()
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "More Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TextSizeSection(
    textSize: Int,
    onTextSizeChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Outlined.TextFields,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Text Size",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${(textSize + 50) * 2}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "A",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = textSize.toFloat(),
                onValueChange = { onTextSizeChange(it.toInt()) },
                valueRange = -25f..50f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "A",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeSection(
    isDarkTheme: Boolean,
    dayBgColor: Int,
    dayTextColor: Int,
    nightBgColor: Int,
    nightTextColor: Int,
    onToggleTheme: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Light theme preview
            ThemePreviewCard(
                label = "Light",
                bgColor = Color(dayBgColor),
                textColor = Color(dayTextColor),
                isSelected = !isDarkTheme,
                onClick = { if (isDarkTheme) onToggleTheme() },
                modifier = Modifier.weight(1f)
            )
            // Dark theme preview
            ThemePreviewCard(
                label = "Dark",
                bgColor = Color(nightBgColor),
                textColor = Color(nightTextColor),
                isSelected = isDarkTheme,
                onClick = { if (!isDarkTheme) onToggleTheme() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    label: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Aa",
            style = MaterialTheme.typography.headlineSmall,
            color = textColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
