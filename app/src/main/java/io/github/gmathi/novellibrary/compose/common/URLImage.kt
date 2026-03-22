package io.github.gmathi.novellibrary.compose.common

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.github.gmathi.novellibrary.network.HostNames
import java.net.URL

/**
 * A reusable composable for loading images from URLs with cookie and header support.
 * Cookie/header resolution is memoized per URL to avoid main-thread IPC on every recomposition.
 */
@Composable
fun URLImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    width: Dp? = null,
    height: Dp? = null,
    shape: Shape? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showLoadingIndicator: Boolean = true,
    loadingIndicatorSize: Dp = 24.dp,
    errorContent: @Composable (() -> Unit)? = null
) {
    val actualWidth = width ?: size
    val actualHeight = height ?: size

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    // Memoize cookie + host resolution per imageUrl so CookieManager IPC
    // doesn't run on every recomposition / frame during scrolling.
    val imageModel = remember(imageUrl) {
        if (imageUrl.isNullOrBlank()) null
        else {
            val hostName = try {
                URL(imageUrl).host.replace("www.", "").replace("m.", "").trim()
            } catch (_: Exception) { "" }

            val cookies = CookieManager.getInstance().getCookie(imageUrl)
                ?: CookieManager.getInstance().getCookie(".$hostName")
                ?: ""

            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(actualWidth.value.toInt(), actualHeight.value.toInt())
                .crossfade(true)
                .apply {
                    setHeader("User-Agent", HostNames.USER_AGENT)
                    if (cookies.isNotEmpty()) {
                        setHeader("Cookie", cookies)
                    }
                }
                .build()
        }
    }

    Box(
        modifier = modifier
            .size(actualWidth, actualHeight)
            .then(
                if (shape != null) Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = shape
                ) else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isPreview && imageModel != null) {
            SubcomposeAsyncImage(
                model = imageModel,
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                loading = {
                    if (showLoadingIndicator) {
                        Box(
                            modifier = Modifier.matchParentSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(loadingIndicatorSize),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                error = {
                    errorContent?.invoke()
                }
            )
        }
    }
}
