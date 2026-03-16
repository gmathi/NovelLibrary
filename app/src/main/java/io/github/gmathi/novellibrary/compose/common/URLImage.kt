package io.github.gmathi.novellibrary.compose.common

import android.webkit.CookieManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
 * 
 * @param imageUrl The URL of the image to load
 * @param contentDescription Description for accessibility
 * @param modifier Modifier for the image container
 * @param size Size of the image (width and height) when using uniform dimensions
 * @param width Width of the image (overrides size for width when specified)
 * @param height Height of the image (overrides size for height when specified)
 * @param shape Shape to clip the image (optional)
 * @param contentScale How to scale the image content
 * @param showLoadingIndicator Whether to show a loading indicator while loading
 * @param loadingIndicatorSize Size of the loading indicator
 * @param errorContent Optional composable to show on error
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
        if (!isPreview && !imageUrl.isNullOrBlank()) {
            // Get cookies and headers
            val hostName = try {
                val url = URL(imageUrl)
                url.host.replace("www.", "").replace("m.", "").trim()
            } catch (_: Exception) {
                ""
            }
            
            val cookies = CookieManager.getInstance().getCookie(imageUrl) 
                ?: CookieManager.getInstance().getCookie(".$hostName") 
                ?: ""
            
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(actualWidth.value.toInt(), actualHeight.value.toInt())
                    .crossfade(true)
                    .apply {
                        setHeader("User-Agent", HostNames.USER_AGENT)
                        if (cookies.isNotEmpty()) {
                            setHeader("Cookie", cookies)
                        }
                    }
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                loading = {
                    if (showLoadingIndicator) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(loadingIndicatorSize),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                error = {
                    errorContent?.invoke()
                }
            )
        }
    }
}
