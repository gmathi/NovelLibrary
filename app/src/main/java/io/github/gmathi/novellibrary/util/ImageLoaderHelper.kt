package io.github.gmathi.novellibrary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.target.Target
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation

/**
 * Utility class to help with image loading using Coil
 * Provides consistent image loading patterns across the app
 */
object ImageLoaderHelper {
    
    /**
     * Load image into ImageView with circle crop transformation
     * Handles null/blank URLs by setting transparent placeholder
     */
    fun loadCircleImage(context: Context, imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(android.R.color.transparent)
            return
        }
        
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(imageView)
            .transformations(CircleCropTransformation())
            .build()
        
        ImageLoader(context).enqueue(request)
    }
    
    /**
     * Load image into ImageView with rounded corners
     * Handles null/blank URLs by setting transparent placeholder
     */
    fun loadRoundedImage(context: Context, imageView: ImageView, url: String?, cornerRadius: Float = 8f) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(android.R.color.transparent)
            return
        }
        
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(imageView)
            .transformations(RoundedCornersTransformation(cornerRadius))
            .build()
        
        ImageLoader(context).enqueue(request)
    }
    
    /**
     * Load image into ImageView with custom target
     */
    fun loadImageWithTarget(context: Context, url: String?, target: Target) {
        if (url.isNullOrBlank()) return
        
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(target)
            .build()
        
        ImageLoader(context).enqueue(request)
    }
    
    /**
     * Load image into ImageView with placeholder
     * Handles null/blank URLs by setting the provided placeholder
     */
    fun loadImageWithPlaceholder(context: Context, imageView: ImageView, url: String?, placeholder: Drawable?) {
        if (url.isNullOrBlank()) {
            imageView.setImageDrawable(placeholder)
            return
        }
        
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(imageView)
            .placeholder(placeholder)
            .error(placeholder)
            .build()
        
        ImageLoader(context).enqueue(request)
    }
    
    /**
     * Load image into ImageView with custom placeholder resource
     * Handles null/blank URLs by setting the provided placeholder resource
     */
    fun loadImageWithPlaceholderResource(context: Context, imageView: ImageView, url: String?, placeholderResId: Int) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(placeholderResId)
            return
        }
        
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(imageView)
            .placeholder(placeholderResId)
            .error(placeholderResId)
            .build()
        
        ImageLoader(context).enqueue(request)
    }
    
    /**
     * Convert Drawable to Bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }
} 