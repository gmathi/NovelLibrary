package com.mgn.bingenovelreader.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.CardListItemAdapter
import com.mgn.bingenovelreader.dataCenter
import kotlinx.android.synthetic.main.activity_cache_list.*
import kotlinx.android.synthetic.main.content_cache_list.*

class CacheDetailsActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    var adapter: CardListItemAdapter? = null
    var title: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache_list)
        setSupportActionBar(toolbar)

        title = intent.getStringExtra("title")
        if (title == null)
            finish()

        setup()
    }

    private fun setup() {
        supportActionBar!!.title = title
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        fab.hide()

        val chapters = ArrayList<String>()
        dataCenter.cacheMap[title]!!.mapTo(chapters) { it.title!! }
        adapter = CardListItemAdapter(this, chapters)
        cacheListView.adapter = adapter
        cacheListView.onItemClickListener = this

    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d("listItemClick", adapter!!.getItem(position))
        startReaderActivity(position, title!!)
    }

    fun startReaderActivity(position: Int, title: String) {
        val readerIntent: Intent = Intent(this, ReaderActivity::class.java)
        readerIntent.putExtra("position", position)
        readerIntent.putExtra("title", title)
        startActivity(readerIntent)
    }


    //region Not Used For Now OptionsMenu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_cache_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_settings) {
            return true
        }
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    //endregion
}
