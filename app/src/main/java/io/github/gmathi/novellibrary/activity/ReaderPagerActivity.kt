package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.MenuItem
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.WebPageAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateCurrentWebPageId
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.WebPageFragment
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import kotlinx.android.synthetic.main.activity_reader_pager.*

class ReaderPagerActivity : BaseActivity(), ViewPager.OnPageChangeListener, FloatingToolbar.ItemClickListener, SeekBar.OnSeekBarChangeListener {

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

        floatingToolbar.attachFab(fab)
        floatingToolbar.setClickListener(this)

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


    override fun onItemLongClick(item: MenuItem?) {

    }

    override fun onItemClick(item: MenuItem?) {
        when (item?.itemId) {
            R.id.action_dark_theme -> toggleDarkTheme()
            R.id.action_font_size -> changeTextSize()
        }
    }

    private fun toggleDarkTheme() {
        dataCenter.isDarkTheme = !dataCenter.isDarkTheme
        (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.reloadPage()
    }

    fun changeTextSize() {
        val dialog = MaterialDialog.Builder(this)
            .title(R.string.text_size)
            .customView(R.layout.dialog_text_slider, true)
            .build()
        dialog.show()
        dialog.customView?.findViewById<SeekBar>(R.id.fontSeekBar)?.setOnSeekBarChangeListener(this)
        dialog.customView?.findViewById<SeekBar>(R.id.fontSeekBar)?.progress = dataCenter.textSize
    }

    //region SeekBar Progress Listener
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        dataCenter.textSize = progress
        (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.changeTextSize(progress)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }
    //endregion


}