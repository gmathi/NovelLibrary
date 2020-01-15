package io.github.gmathi.novellibrary.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.io.File


class ImagePreviewActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            val decorView = window.decorView
            val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = option
            getWindow().statusBarColor = ContextCompat.getColor(this, R.color.colorBackground)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_preview)

        val url = intent.getStringExtra("url")
        val filePath = intent.getStringExtra("filePath")



        if (Utils.isConnectedToNetwork(this) && url != null)
            Glide.with(this).load(url).into(previewImageView)
        else if (filePath != null)
            Glide.with(this).load(File(filePath.replace(FILE_PROTOCOL, ""))).into(previewImageView)
    }

}
