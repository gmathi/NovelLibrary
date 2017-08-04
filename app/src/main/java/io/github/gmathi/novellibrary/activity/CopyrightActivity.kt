package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.applyFont
import kotlinx.android.synthetic.main.activity_copyright.*
import kotlinx.android.synthetic.main.content_copyright.*

class CopyrightActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_copyright)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        copyrightContentTextView.applyFont(assets)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}
