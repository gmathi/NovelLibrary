package io.github.gmathi.novellibrary.compose.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.R

enum class SortOption {
    ALPHABETICALLY_ASC,
    ALPHABETICALLY_DESC,
    LAST_READ_NEWEST,
    LAST_READ_OLDEST,
    LAST_UPDATED_NEWEST,
    LAST_UPDATED_OLDEST,
    RECENTLY_ADDED_NEWEST,
    RECENTLY_ADDED_OLDEST
}

@Composable
fun SortBottomSheetContent(
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.sort),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Alphabetical section
        SortSectionHeader(title = stringResource(R.string.sort_section_alphabetical))
        SortOptionItem(
            label = stringResource(R.string.sort_alphabetically_asc),
            icon = Icons.Outlined.SortByAlpha,
            onClick = { onSortSelected(SortOption.ALPHABETICALLY_ASC) }
        )
        SortOptionItem(
            label = stringResource(R.string.sort_alphabetically_desc),
            icon = Icons.Outlined.SortByAlpha,
            onClick = { onSortSelected(SortOption.ALPHABETICALLY_DESC) }
        )

        SortDivider()

        // Last Read section
        SortSectionHeader(title = stringResource(R.string.sort_section_last_read))
        SortOptionItem(
            label = stringResource(R.string.sort_by_last_read_newest),
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            onClick = { onSortSelected(SortOption.LAST_READ_NEWEST) }
        )
        SortOptionItem(
            label = stringResource(R.string.sort_by_last_read_oldest),
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            onClick = { onSortSelected(SortOption.LAST_READ_OLDEST) }
        )

        SortDivider()

        // Last Updated section
        SortSectionHeader(title = stringResource(R.string.sort_section_last_updated))
        SortOptionItem(
            label = stringResource(R.string.sort_by_last_updated_newest),
            icon = Icons.Outlined.Update,
            onClick = { onSortSelected(SortOption.LAST_UPDATED_NEWEST) }
        )
        SortOptionItem(
            label = stringResource(R.string.sort_by_last_updated_oldest),
            icon = Icons.Outlined.Update,
            onClick = { onSortSelected(SortOption.LAST_UPDATED_OLDEST) }
        )

        SortDivider()

        // Recently Added section
        SortSectionHeader(title = stringResource(R.string.sort_section_recently_added))
        SortOptionItem(
            label = stringResource(R.string.sort_by_recently_added_newest),
            icon = Icons.Outlined.NewReleases,
            onClick = { onSortSelected(SortOption.RECENTLY_ADDED_NEWEST) }
        )
        SortOptionItem(
            label = stringResource(R.string.sort_by_recently_added_oldest),
            icon = Icons.Outlined.NewReleases,
            onClick = { onSortSelected(SortOption.RECENTLY_ADDED_OLDEST) }
        )
    }
}

@Composable
private fun SortSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun SortOptionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SortDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
