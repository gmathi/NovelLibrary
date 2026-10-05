package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.util.changelog.Changelog
import io.github.gmathi.novellibrary.util.changelog.ChangelogChange
import io.github.gmathi.novellibrary.util.changelog.ChangelogVersion

/**
 * Material 3 "What's Changed" screen. Renders [Changelog.versions] as a list of
 * expandable version cards, newest first, with the latest release expanded and
 * badged. Replaces the old XML RecyclerView + alternating-row-colour screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsChangedScreen(
    onNavigateBack: () -> Unit,
) {
    val versions = remember { Changelog.versions }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("What's Changed") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        // Flexible, inset-aware horizontal margins: a base 16dp gutter PLUS any
        // horizontal window inset (display cutout / system gesture area) so the
        // cards never clip on the right under edge-to-edge or in landscape.
        val layoutDirection = LocalLayoutDirection.current
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
        val startPadding = 16.dp + safeInsets.calculateStartPadding(layoutDirection)
        val endPadding = 16.dp + safeInsets.calculateEndPadding(layoutDirection)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start = startPadding,
                end = endPadding,
                top = 8.dp,
                bottom = 24.dp + safeInsets.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(versions, key = { it.version }) { version ->
                VersionCard(
                    version = version,
                    isLatest = version === versions.first(),
                    initiallyExpanded = version === versions.first(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VersionCard(
    version: ChangelogVersion,
    isLatest: Boolean,
    initiallyExpanded: Boolean,
) {
    var expanded by rememberSaveable(version.version) { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLatest) 2.dp else 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Header (tap to expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Version ${version.version}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (isLatest) {
                            LatestBadge()
                        }
                    }
                    version.date?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    version.headline?.let { headline ->
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLatest) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    version.changes.forEach { change ->
                        ChangeRow(change = change, onContainer = isLatest)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChangeRow(change: ChangelogChange, onContainer: Boolean) {
    val titleColor = if (onContainer) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val detailColor = if (onContainer) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .padding(top = 2.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(text = change.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = change.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (change.major) FontWeight.Bold else FontWeight.SemiBold,
                    color = titleColor,
                )
                if (change.major) {
                    MajorBadge()
                }
            }
            change.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = detailColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun LatestBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = "LATEST",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MajorBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "NEW",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
}
