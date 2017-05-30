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

class CacheListActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    var adapter: CardListItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cache_list)
        setSupportActionBar(toolbar)
        setup()
    }

    private fun setup() {
        dataCenter.loadCacheMap()
        fab.setOnClickListener { _ ->
            startOfflineDownloaderActivity()
        }
        adapter = CardListItemAdapter(this, ArrayList(dataCenter.cacheMap.keys))
        cacheListView.adapter = adapter
        cacheListView.onItemClickListener = this
    }

    override fun onResume() {
        super.onResume()
        adapter?.updateData(ArrayList(dataCenter.cacheMap.keys))
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d("listItemClick", adapter!!.getItem(position))
        startCacheDetailsActivity(adapter!!.getItem(position))
    }

    private fun startCacheDetailsActivity(title: String) {
        val cacheDetailsActivity: Intent = Intent(this, CacheDetailsActivity::class.java)
        cacheDetailsActivity.putExtra("title", title)
        startActivity(cacheDetailsActivity)
    }

    fun startOfflineDownloaderActivity() {
        val downloaderIntent: Intent = Intent(this, DownloaderActivity::class.java)
        startActivity(downloaderIntent)
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
        return super.onOptionsItemSelected(item)
    }
    //endregion
}
