package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.ImageLoaderHelper

class ImagePreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val imageUrl = intent.getStringExtra("imageUrl")
        ImageLoaderHelper.loadImageWithPlaceholder(this, imageView, imageUrl, null)
    }
}
