package io.github.gmathi.novellibrary.activity.downloads

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.github.gmathi.novellibrary.R
import kotlinx.android.synthetic.main.activity_chapter_downloads.*

class ChapterDownloadsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter_downloads)
        setSupportActionBar(toolbar)

        fab.hide()
    }

}
