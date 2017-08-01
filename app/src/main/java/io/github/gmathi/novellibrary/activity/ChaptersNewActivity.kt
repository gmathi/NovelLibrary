package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.github.gmathi.novellibrary.R
import kotlinx.android.synthetic.main.activity_chapters_new.*

class ChaptersNewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters_new)
        setSupportActionBar(toolbar)
    }

}
