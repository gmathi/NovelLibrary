package com.mgn.bingenovelreader.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.extension.snackBar
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.model.WebPage
import kotlinx.android.synthetic.main.activity_chapters.*

class ChaptersActivity : AppCompatActivity() {

    lateinit var novel: Novel
    lateinit var chapters: List<WebPage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        fab.setOnClickListener { view ->
            snackBar(view, "Fab pressed!")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}
