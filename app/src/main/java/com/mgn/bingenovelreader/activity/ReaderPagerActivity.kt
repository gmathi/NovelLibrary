package com.mgn.bingenovelreader.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.WebPageAdapter
import com.mgn.bingenovelreader.database.updateCurrentWebPageId
import com.mgn.bingenovelreader.database.updateWebPageReadStatus
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.model.WebPage
import kotlinx.android.synthetic.main.activity_reader_pager.*

class ReaderPagerActivity : BaseActivity(), ViewPager.OnPageChangeListener {

    var novel: Novel? = null
    var webPage: WebPage? = null

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)

        novel = intent.getSerializableExtra("novel") as Novel?
        webPage = intent.getSerializableExtra("webPage") as WebPage?

        @Suppress("UNCHECKED_CAST")
        chapters = intent.getSerializableExtra("chapters") as ArrayList<WebPage>

        if (novel == null || webPage == null) finish()

        adapter = WebPageAdapter(supportFragmentManager, chapters, object : WebPageAdapter.Listener {
            override fun checkUrl(url: String?) {
                if (url != null) {
                    val index = chapters.indexOfFirst { it.redirectedUrl!!.contains(url) }
                    if (index != -1) {
                        viewPager.currentItem = index
                        val webPage = chapters[index]
                        if (webPage.novelId != -1L && webPage.id != -1L) {
                            dbHelper.updateCurrentWebPageId(webPage.novelId, webPage.id)
                        }
                        if (webPage.id != -1L) {
                            webPage.isRead = 1
                            dbHelper.updateWebPageReadStatus(webPage)
                        }
                    }
                }
            }
        })

        viewPager.addOnPageChangeListener(this)
        viewPager.adapter = adapter

        viewPager.currentItem = chapters.indexOf(webPage)
        if (webPage?.novelId != -1L && webPage?.id != -1L)
            dbHelper.updateCurrentWebPageId(webPage!!.novelId, webPage!!.id)
        if (webPage?.id != -1L) {
            webPage!!.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage!!)
        }


//        val novelId = intent.getLongExtra(Constants.NOVEL_ID, -1L)
//        if (novelId == -1L) finish()
//
//        chapters = ArrayList<WebPage>(dbHelper.getAllReadableWebPages(novelId))
//        adapter = WebPageAdapter(supportFragmentManager, chapters, object : WebPageAdapter.Listener {
//            override fun checkUrl(url: String?) {
//                if (url != null) {
//                    val index = chapters.indexOfFirst { it.redirectedUrl!!.contains(url) }
//                    if (index != -1) {
//                        viewPager.currentItem = index
//                        dbHelper.updateCurrentWebPageId(chapters[index].novelId, chapters[index].id)
//                    }
//                }
//            }
//        })
//
//        viewPager.addOnPageChangeListener(this)
//        viewPager.adapter = adapter
//
//        val webPageId = intent.getLongExtra(Constants.WEB_PAGE_ID, -1L)
//        if (webPageId != -1L) {
//            viewPager.currentItem = chapters.indexOfFirst { it.id == webPageId }
//            dbHelper.updateCurrentWebPageId(novelId, webPageId)
//        }
    }

    override fun onPageSelected(position: Int) {
        val webPage = chapters[position]
        if (webPage.novelId != -1L && webPage.id != -1L)
            dbHelper.updateCurrentWebPageId(webPage.novelId, webPage.id)
        if (webPage.id != -1L) {
            webPage.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage)
        }
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }


}