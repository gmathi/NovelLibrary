package io.github.gmathi.novellibrary.compose.searchurl

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.activity.CloudflareResolverActivity
import io.github.gmathi.novellibrary.compose.search.CloudflareDialog
import io.github.gmathi.novellibrary.compose.search.SearchUrlNovelItemWrapper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.viewmodel.SearchUrlUiState
import io.github.gmathi.novellibrary.viewmodel.SearchUrlViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUrlScreen(
    viewModel: SearchUrlViewModel,
    title: String,
    onNovelClick: (Novel) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val novels by viewModel.novels.collectAsState()
    val activity = LocalContext.current as? Activity

    // Cloudflare dialog state
    var showCloudflareDialog by remember { mutableStateOf(false) }
    var cloudflareUrl by remember { mutableStateOf<String?>(null) }

    val cloudflareResolverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.retry()
        }
        showCloudflareDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BrowseContent(
                uiState = uiState,
                novels = novels,
                onRetry = { viewModel.retry() },
                onLoadMore = { viewModel.loadMore() },
                onNovelClick = onNovelClick,
                onResolveCloudflare = { url ->
                    cloudflareUrl = url
                    showCloudflareDialog = true
                }
            )
        }

        if (showCloudflareDialog) {
            CloudflareDialog(
                onResolveManually = {
                    showCloudflareDialog = false
                    val url = cloudflareUrl ?: "https://www.novelupdates.com"
                    val intent = CloudflareResolverActivity.createIntent(activity ?: return@CloudflareDialog, url)
                    cloudflareResolverLauncher.launch(intent)
                },
                onRetry = {
                    showCloudflareDialog = false
                    viewModel.retry()
                },
                onDismiss = {
                    showCloudflareDialog = false
                }
            )
        }
    }
}

@Composable
private fun BrowseContent(
    uiState: SearchUrlUiState,
    novels: List<Novel>,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onResolveCloudflare: (String) -> Unit
) {
    when (uiState) {
        is SearchUrlUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SearchUrlUiState.Error -> {
            ErrorState(
                message = uiState.message,
                isCloudflare = uiState.isCloudflare,
                onRetry = onRetry,
                onResolveCloudflare = if (uiState.isCloudflare) {
                    { onResolveCloudflare(uiState.cloudflareUrl ?: "https://www.novelupdates.com") }
                } else null
            )
        }
        is SearchUrlUiState.NoInternet -> {
            ErrorState(
                message = "No internet connection",
                isCloudflare = false,
                onRetry = onRetry,
                onResolveCloudflare = null
            )
        }
        is SearchUrlUiState.Empty -> {
            EmptyState(message = "No novels found", icon = Icons.Filled.SearchOff)
        }
        is SearchUrlUiState.Success -> {
            if (novels.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(
                        items = novels,
                        key = { index, novel -> "browse_${index}_${novel.id}_${novel.name.hashCode()}" }
                    ) { _, novel ->
                        SearchUrlNovelItemWrapper(novel = novel, onClick = { onNovelClick(novel) })
                    }
                    if (uiState.hasMore) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(onClick = onLoadMore) { Text("Load More") }
                            }
                        }
                    }
                }
            } else {
                EmptyState(message = "No novels found", icon = Icons.Filled.SearchOff)
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    isCloudflare: Boolean,
    onRetry: () -> Unit,
    onResolveCloudflare: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCloudflare) Icons.Filled.Security else Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = if (isCloudflare) "Cloudflare Verification Required" else "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCloudflare && onResolveCloudflare != null) {
                Button(onClick = onResolveCloudflare) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Resolve Cloudflare")
                }
            }
            OutlinedButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
