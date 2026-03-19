package io.github.gmathi.novellibrary.compose.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * A persistent search view component built with Jetpack Compose.
 * Provides search functionality with suggestions, history, and customizable callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistentSearchView(
    modifier: Modifier = Modifier,
    state: PersistentSearchState = rememberPersistentSearchState(),
    hint: String = "Search",
    homeButtonMode: HomeButtonMode = HomeButtonMode.Burger,
    onHomeButtonClick: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    onSearchTermChanged: (String) -> Unit = {},
    onSearchEditOpened: () -> Unit = {},
    onSearchEditClosed: () -> Unit = {},
    onSearchExit: () -> Unit = {},
    onSearchCleared: () -> Unit = {},
    onSearchReset: () -> Unit = {},
    onSuggestionClick: (SearchSuggestion) -> Boolean = { true },
    suggestionBuilder: SearchSuggestionsBuilder? = null,
    elevation: Int = 4,
    colors: PersistentSearchColors = PersistentSearchDefaults.colors()
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.isEditing) {
        if (state.isEditing) {
            onSearchEditOpened()
            focusRequester.requestFocus()
        } else {
            onSearchEditClosed()
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
            colors = CardDefaults.cardColors(containerColor = colors.backgroundColor)
        ) {
            Column {
                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home/Back Button
                    IconButton(onClick = {
                        if (state.isEditing) {
                            // Editing -> close the edit, keep search results if any
                            state.closeSearch()
                            if (!state.isSearching) {
                                onSearchExit()
                            }
                        } else if (state.isSearching) {
                            // Showing search results -> reset to browse
                            state.resetSearch()
                            onSearchReset()
                        } else {
                            onHomeButtonClick()
                        }
                    }) {
                        val showBackArrow = state.isEditing || state.isSearching
                        Icon(
                            imageVector = if (showBackArrow) Icons.AutoMirrored.Filled.ArrowBack
                                         else if (homeButtonMode == HomeButtonMode.Arrow) Icons.AutoMirrored.Filled.ArrowBack
                                         else Icons.Filled.Menu,
                            contentDescription = if (showBackArrow) "Back" else "Menu",
                            tint = colors.iconColor
                        )
                    }

                    // Search Input
                    if (state.isEditing) {
                        TextField(
                            value = state.textFieldValue,
                            onValueChange = { 
                                state.updateTextFieldValue(it)
                                onSearchTermChanged(it.text)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = { Text(hint, color = colors.hintColor) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedTextColor = colors.textColor,
                                unfocusedTextColor = colors.textColor
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (state.searchText.isNotBlank()) {
                                        state.performSearch()
                                        onSearch(state.searchText)
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        )
                    } else if (state.isSearching && state.searchText.isNotBlank()) {
                        // Show the searched term in the bar, clickable to re-edit
                        Text(
                            text = state.searchText,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { state.openSearch() },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textColor
                        )
                    } else {
                        // Logo/Title
                        Text(
                            text = state.logoText.ifEmpty { hint },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { state.openSearch() },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textColor
                        )
                    }

                    // Clear/Search Button
                    if (state.isEditing) {
                        if (state.searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                state.clearSearch()
                                onSearchCleared()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.iconColor
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { state.openSearch() }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = colors.iconColor
                            )
                        }
                    }
                }

                // Suggestions List
                AnimatedVisibility(
                    visible = state.isEditing && state.suggestions.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(
                            items = state.suggestions,
                            key = { suggestion -> "${suggestion.type.name}_${suggestion.text}" }
                        ) { suggestion ->
                            SuggestionItem(
                                suggestion = suggestion,
                                colors = colors,
                                onClick = {
                                    if (onSuggestionClick(suggestion)) {
                                        state.updateSearchText(suggestion.text)
                                        state.performSearch()
                                        onSearch(suggestion.text)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Update suggestions when search text changes
    LaunchedEffect(state.searchText, state.isEditing) {
        if (state.isEditing) {
            val newSuggestions = if (state.searchText.isEmpty()) {
                suggestionBuilder?.buildEmptySearchSuggestion(5) ?: emptyList()
            } else {
                suggestionBuilder?.buildSearchSuggestion(5, state.searchText) ?: emptyList()
            }
            state.updateSuggestions(newSuggestions)
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: SearchSuggestion,
    colors: PersistentSearchColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (suggestion.type == SearchSuggestionType.History) 
                Icons.Filled.History else Icons.Filled.Search,
            contentDescription = null,
            tint = colors.iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = suggestion.text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textColor
        )
    }
}

/**
 * Background tint overlay that appears when search is active
 */
@Composable
fun SearchBackgroundTint(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        label = "background_tint_alpha"
    )

    if (visible || alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onClick)
        )
    }
}

enum class HomeButtonMode {
    Burger,
    Arrow
}

object PersistentSearchDefaults {
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surface,
        textColor: Color = MaterialTheme.colorScheme.onSurface,
        hintColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = PersistentSearchColors(
        backgroundColor = backgroundColor,
        textColor = textColor,
        hintColor = hintColor,
        iconColor = iconColor
    )
}

data class PersistentSearchColors(
    val backgroundColor: Color,
    val textColor: Color,
    val hintColor: Color,
    val iconColor: Color
)
