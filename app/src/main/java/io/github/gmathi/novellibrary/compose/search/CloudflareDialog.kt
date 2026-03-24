package io.github.gmathi.novellibrary.compose.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Dialog shown when a Cloudflare challenge is detected during an API call.
 * Offers the user two options:
 * - Resolve manually via a WebView (opens CloudflareResolverActivity)
 * - Retry the request directly
 */
@Composable
fun CloudflareDialog(
    onResolveManually: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Cloudflare Verification Required",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "The server requires a Cloudflare verification. " +
                        "You can solve it manually in a browser, and then we'll " +
                        "resume the operation. Or you can retry directly.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onResolveManually) {
                Text("Resolve Manually")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    )
}
