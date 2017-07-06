package com.mgn.bingenovelreader.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.WebPageAdapter
import com.mgn.bingenovelreader.database.getAllReadableWebPages
import com.mgn.bingenovelreader.database.updateCurrentWebPageId
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.model.WebPage
import com.mgn.bingenovelreader.util.Constants
import kotlinx.android.synthetic.main.activity_reader_pager.*

class ReaderPagerActivity : AppCompatActivity(), ViewPager.OnPageChangeListener {

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)

        val novelId = intent.getLongExtra(Constants.NOVEL_ID, -1L)
        if (novelId == -1L) finish()

        chapters = ArrayList<WebPage>(dbHelper.getAllReadableWebPages(novelId))
        adapter = WebPageAdapter(supportFragmentManager, chapters, object : WebPageAdapter.Listener {
            override fun checkUrl(url: String?) {
                if (url != null) {
                    val index = chapters.indexOfFirst { it.redirectedUrl!!.contains(url) }
                    if (index != -1) {
                        viewPager.currentItem = index
                        dbHelper.updateCurrentWebPageId(chapters[index].novelId, chapters[index].id)
                    }
                }
            }
        })

        viewPager.addOnPageChangeListener(this)
        viewPager.adapter = adapter

        val webPageId = intent.getLongExtra(Constants.WEB_PAGE_ID, -1L)
        if (webPageId != -1L) {
            viewPager.currentItem = chapters.indexOfFirst { it.id == webPageId }
            dbHelper.updateCurrentWebPageId(novelId, webPageId)
        }
    }

    override fun onPageSelected(position: Int) {
        dbHelper.updateCurrentWebPageId(chapters[position].novelId, chapters[position].id)
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }


}