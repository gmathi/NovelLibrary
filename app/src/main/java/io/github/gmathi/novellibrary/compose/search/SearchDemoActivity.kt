package io.github.gmathi.novellibrary.compose.search

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Enhanced demo activity to showcase the PersistentSearchView component
 * Features: Search, filtering, categories, and Material 3 design
 */
class SearchDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF90CAF9),
                    secondary = Color(0xFFCE93D8),
                    tertiary = Color(0xFFA5D6A7),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onPrimary = Color.Black,
                    onSecondary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                SearchDemoScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDemoScreen() {
    var searchResults by remember { mutableStateOf<List<Novel>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showStats by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Sample data with categories
    val sampleNovels = remember {
        listOf(
            Novel("The Beginning After The End", "Fantasy", 4.8f, "TurtleMe", "Ongoing", "https://example.com/1.jpg"),
            Novel("Solo Leveling", "Action", 4.9f, "Chugong", "Completed", "https://example.com/2.jpg"),
            Novel("Omniscient Reader's Viewpoint", "Fantasy", 4.9f, "Sing Shong", "Ongoing", "https://example.com/3.jpg"),
            Novel("The Lord of the Mysteries", "Mystery", 4.8f, "Cuttlefish", "Completed", "https://example.com/4.jpg"),
            Novel("Reverend Insanity", "Xianxia", 4.7f, "Gu Zhen Ren", "Hiatus", "https://example.com/5.jpg"),
            Novel("Warlock of the Magus World", "Fantasy", 4.6f, "Wen Chao Gong", "Completed", "https://example.com/6.jpg"),
            Novel("Release That Witch", "Fantasy", 4.7f, "Er Mu", "Completed", "https://example.com/7.jpg"),
            Novel("The Legendary Mechanic", "Sci-Fi", 4.8f, "Qi Peijia", "Completed", "https://example.com/8.jpg"),
            Novel("Mother of Learning", "Fantasy", 4.9f, "Domagoj Kurmaic", "Completed", "https://example.com/9.jpg"),
            Novel("Overgeared", "Action", 4.6f, "Park Saenal", "Ongoing", "https://example.com/10.jpg"),
            Novel("Second Life Ranker", "Action", 4.7f, "Sadoyeon", "Ongoing", "https://example.com/11.jpg"),
            Novel("Trash of the Count's Family", "Fantasy", 4.8f, "Yoo Ryeo Han", "Ongoing", "https://example.com/12.jpg")
        )
    }
    
    val searchHistory = remember {
        mutableStateListOf("Solo Leveling", "The Beginning", "Lord of Mysteries", "Fantasy novels")
    }

    val categories = remember {
        listOf("All", "Fantasy", "Action", "Xianxia", "Sci-Fi", "Mystery")
    }

    val searchState = rememberPersistentSearchState(initialLogoText = "Search Novels")
    val suggestionBuilder = remember(searchHistory.toList()) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    // Filter novels based on search and category
    val filteredNovels = remember(searchResults, selectedCategory) {
        if (searchResults.isEmpty()) {
            if (selectedCategory == "All") sampleNovels else sampleNovels.filter { it.category == selectedCategory }
        } else {
            if (selectedCategory == "All") searchResults else searchResults.filter { it.category == selectedCategory }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onShowStats = { showStats = true }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showStats) {
                    StatsTopBar(
                        novelCount = filteredNovels.size,
                        onDismiss = { showStats = false }
                    )
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search View
                        PersistentSearchView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            state = searchState,
                            hint = "Search novels...",
                            homeButtonMode = HomeButtonMode.Burger,
                            onHomeButtonClick = {
                                scope.launch { drawerState.open() }
                            },
                            onSearch = { query ->
                                // Add to history
                                if (query.isNotBlank() && !searchHistory.contains(query)) {
                                    searchHistory.add(0, query)
                                    if (searchHistory.size > 10) {
                                        searchHistory.removeAt(searchHistory.lastIndex)
                                    }
                                }
                                
                                // Perform search
                                searchResults = sampleNovels.filter {
                                    it.title.contains(query, ignoreCase = true) ||
                                    it.author.contains(query, ignoreCase = true) ||
                                    it.category.contains(query, ignoreCase = true)
                                }
                            },
                            onSearchTermChanged = { term ->
                                // Real-time filtering
                                if (term.isNotEmpty()) {
                                    searchResults = sampleNovels.filter {
                                        it.title.contains(term, ignoreCase = true) ||
                                        it.author.contains(term, ignoreCase = true) ||
                                        it.category.contains(term, ignoreCase = true)
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            onSearchCleared = {
                                searchResults = emptyList()
                            },
                            onSearchExit = {
                                searchResults = emptyList()
                            },
                            suggestionBuilder = suggestionBuilder,
                            elevation = 4
                        )

                        // Category Filter
                        CategoryFilter(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )

                        // Results Header
                        ResultsHeader(
                            count = filteredNovels.size,
                            isSearching = searchState.isSearching || searchResults.isNotEmpty()
                        )

                        // Search Results or Default Content
                        if (filteredNovels.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredNovels, key = { it.title }) { novel ->
                                    NovelCard(novel = novel)
                                }
                            }
                        } else if (searchState.isSearching || searchResults.isEmpty() && searchState.searchText.isNotEmpty()) {
                            EmptyState(
                                message = "No novels found",
                                icon = Icons.Filled.SearchOff
                            )
                        } else {
                            WelcomeContent()
                        }
                    }

                    // Background tint overlay
                    SearchBackgroundTint(
                        visible = searchState.isEditing,
                        onClick = { searchState.closeSearch() }
                    )
                }
            }
        }
    }
}

@Composable
fun NovelCard(novel: Novel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle click */ }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Novel Cover Placeholder
            Box(
                modifier = Modifier
                    .size(80.dp, 110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Novel Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "by ${novel.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = novel.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFFB300)
                        )
                        Text(
                            text = novel.rating.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Status Badge
                    StatusBadge(status = novel.status)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Ongoing" -> Color(0xFF4CAF50)
        "Completed" -> Color(0xFF2196F3)
        "Hiatus" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.outline
    }
    
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                leadingIcon = if (category == selectedCategory) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

@Composable
fun ResultsHeader(count: Int, isSearching: Boolean) {
    if (isSearching || count > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count ${if (count == 1) "novel" else "novels"} found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WelcomeContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Search for Novels",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Try searching for your favorite novels\nor browse by category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    onCloseDrawer: () -> Unit,
    onShowStats: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                text = "Novel Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            HorizontalDivider()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                label = { Text("Home") },
                selected = false,
                onClick = onCloseDrawer
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                label = { Text("Favorites") },
                selected = false,
                onClick = onCloseDrawer
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Analytics, contentDescription = null) },
                label = { Text("Statistics") },
                selected = false,
                onClick = {
                    onShowStats()
                    onCloseDrawer()
                }
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = onCloseDrawer
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Compose Search Demo v1.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopBar(novelCount: Int, onDismiss: () -> Unit) {
    TopAppBar(
        title = { Text("$novelCount Novels Available") },
        actions = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// Data class for Novel
data class Novel(
    val title: String,
    val category: String,
    val rating: Float,
    val author: String,
    val status: String,
    val coverUrl: String
)
