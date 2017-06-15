package com.mgn.bingenovelreader.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mgn.bingenovelreader.R
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        setSupportActionBar(toolbar)

        fabSearch.setOnClickListener { startSearchActivity() }

    }

    private fun startSearchActivity() {
        startActivity(Intent(this, SearchActivity::class.java))
    }

}
