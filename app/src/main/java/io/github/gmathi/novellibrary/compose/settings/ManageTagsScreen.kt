package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.library.ColorSwatch
import io.github.gmathi.novellibrary.compose.library.TagChip
import io.github.gmathi.novellibrary.compose.library.TagColors
import io.github.gmathi.novellibrary.viewmodel.LibraryTag

/**
 * Settings screen to manage library tags: create, rename, recolor and delete. Reads/writes the same
 * tag model the new library uses, so changes are shared. Follows the restructured-settings pattern
 * (Scaffold + TopAppBar + LazyColumn, SettingsSectionHeader) used by the sibling settings screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTagsScreen(
    tags: List<LibraryTag>,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onSetColor: (Long, Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<LibraryTag?>(null) }
    var deleteTarget by remember { mutableStateOf<LibraryTag?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library Tags") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsSectionHeader(title = "Add a tag") }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("New tag name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick = {
                            if (newTag.isNotBlank()) {
                                onCreate(newTag)
                                newTag = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create tag")
                    }
                }
            }

            item { SettingsSectionHeader(title = "Tags (${tags.size})") }

            if (tags.isEmpty()) {
                item {
                    Text(
                        text = "No tags yet. Tags carry over from your library sections and you can add more above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(tags, key = { it.tag.id }) { libraryTag ->
                    TagManagerRow(
                        libraryTag = libraryTag,
                        onEdit = { editTarget = libraryTag },
                        onDelete = { deleteTarget = libraryTag }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editTarget?.let { target ->
        TagEditDialog(
            tag = target,
            onSave = { name, color ->
                if (name.isNotBlank() && name != target.tag.name) onRename(target.tag.id, name)
                if (color != target.tag.color) onSetColor(target.tag.id, color)
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete tag?") },
            text = { Text("\"${target.tag.name}\" will be removed from ${target.novelCount} novel(s). The novels themselves are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.tag.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TagManagerRow(
    libraryTag: LibraryTag,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ColorSwatch(
            color = TagColors.resolveTagColor(libraryTag.tag.color, libraryTag.tag.name),
            selected = false,
            modifier = Modifier.size(28.dp),
            onClick = onEdit
        )
        Column(modifier = Modifier.weight(1f)) {
            TagChip(tag = libraryTag.tag)
            Text(
                text = "${libraryTag.novelCount} novel(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, start = 2.dp)
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit tag")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete tag")
        }
    }
}

/** Combined edit dialog: rename and recolor a tag in one place. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditDialog(
    tag: LibraryTag,
    onSave: (String, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(tag.tag.id) { mutableStateOf(tag.tag.name) }
    var color by remember(tag.tag.id) { mutableStateOf(tag.tag.color) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit tag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Tag name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TagColors.palette.forEach { argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = color == argb,
                            modifier = Modifier.size(36.dp),
                            onClick = { color = argb }
                        )
                    }
                }
                TextButton(
                    onClick = { color = null },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("Reset color to default") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

