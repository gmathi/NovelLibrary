package io.github.gmathi.novellibrary.compose.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.viewmodel.SortDirection
import io.github.gmathi.novellibrary.viewmodel.SortKey
import io.github.gmathi.novellibrary.viewmodel.LibraryTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySortSheet(
    currentKey: SortKey,
    currentDirection: SortDirection,
    onSelectKey: (SortKey) -> Unit,
    onSelectDirection: (SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            SortSectionHeader("Sort by")
            SortKey.values().forEach { option ->
                SortOptionRow(
                    label = option.label,
                    selected = option == currentKey,
                    onClick = { onSelectKey(option) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SortSectionHeader("Order")
            SortDirection.values().forEach { option ->
                SortOptionRow(
                    label = option.label,
                    selected = option == currentDirection,
                    onClick = { onSelectDirection(option) }
                )
            }
        }
    }
}

@Composable
private fun SortSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTagsSheet(
    novel: Novel,
    allTags: List<LibraryTag>,
    appliedTagIds: Set<Long>,
    onToggle: (tagId: Long, apply: Boolean) -> Unit,
    onCreateAndApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Tags for ${novel.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (allTags.isEmpty()) {
                Text(
                    text = "No tags yet. Create one below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(allTags, key = { it.tag.id }) { libraryTag ->
                        val applied = libraryTag.tag.id in appliedTagIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(libraryTag.tag.id, !applied) }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Checkbox(
                                checked = applied,
                                onCheckedChange = { onToggle(libraryTag.tag.id, it) }
                            )
                            Spacer(Modifier.width(12.dp))
                            TagChip(tag = libraryTag.tag)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        if (newTag.isNotBlank()) {
                            onCreateAndApply(newTag)
                            newTag = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create and apply tag")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagerSheet(
    tags: List<LibraryTag>,
    onCreate: (String, (Long) -> Unit) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<LibraryTag?>(null) }
    var deleteTarget by remember { mutableStateOf<LibraryTag?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Manage tags",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledIconButton(
                    onClick = {
                        if (newTag.isNotBlank()) {
                            onCreate(newTag) {}
                            newTag = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create tag")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (tags.isEmpty()) {
                Text(
                    text = "No tags yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(tags, key = { it.tag.id }) { libraryTag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = libraryTag.tag.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${libraryTag.novelCount} novel(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { renameTarget = libraryTag }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Rename tag")
                            }
                            IconButton(onClick = { deleteTarget = libraryTag }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete tag")
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        var name by remember(target.tag.id) { mutableStateOf(target.tag.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename tag") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Tag name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(target.tag.id, name)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
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
