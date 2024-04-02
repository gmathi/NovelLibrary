package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.ActivityImagePreviewBinding
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import java.io.File


class ImagePreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val window = window
        val decorView = window.decorView
        val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        decorView.systemUiVisibility = option
        getWindow().statusBarColor = ContextCompat.getColor(this, R.color.colorBackground)
        super.onCreate(savedInstanceState)

        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.getStringExtra("url")
        val filePath = intent.getStringExtra("filePath")

        if (networkHelper.isConnectedToNetwork() && url != null)
            Glide.with(this).load(url).into(binding.previewImageView)
        else if (filePath != null)
            Glide.with(this).load(File(filePath.replace(FILE_PROTOCOL, ""))).into(binding.previewImageView)
    }

}
