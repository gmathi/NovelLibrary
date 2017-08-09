package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.KeyEvent
import android.view.MenuItem
import android.webkit.WebView
import android.widget.SeekBar
import com.afollestad.materialdialogs.MaterialDialog
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.WebPageAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateCurrentWebPageId
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.WebPageFragment
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.android.synthetic.main.activity_reader_pager.*


class ReaderPagerActivity : BaseActivity(), ViewPager.OnPageChangeListener, FloatingToolbar.ItemClickListener, SeekBar.OnSeekBarChangeListener {

    var novel: Novel? = null
    var webPage: WebPage? = null

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)
        //window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        novel = intent.getSerializableExtra("novel") as Novel?
        webPage = intent.getSerializableExtra("webPage") as WebPage?

        @Suppress("UNCHECKED_CAST")
        chapters = intent.getSerializableExtra("chapters") as ArrayList<WebPage>

        if (novel == null || webPage == null) finish()

        adapter = WebPageAdapter(supportFragmentManager, chapters, object : WebPageAdapter.Listener {
            override fun checkUrl(url: String?): Boolean {
                if (url != null) {
                    val currentWebPage = (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.webPage
                    if (currentWebPage?.metaData?.containsKey(Constants.MD_OTHER_LINKED_WEB_PAGES) ?: false) {
                        val links: ArrayList<WebPage> = Gson().fromJson(currentWebPage!!.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES], object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
                        links.forEach {
                            if (it.url == url || (it.redirectedUrl != null && it.redirectedUrl == url)) {
                                (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.loadNewWebPage(it)
                                return@checkUrl true
                            }
                        }
                    }

                    val index = chapters.indexOfFirst { it.redirectedUrl != null && it.redirectedUrl!!.contains(url) }
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
                        return true
                    } else return false
                } else return false
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

        fabClean.setOnClickListener {
            (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).cleanPage()
            fabClean.hide()
        }

    }

    override fun onPageSelected(position: Int) {
        val webPage = chapters[position]
        if (webPage.novelId != -1L && webPage.id != -1L)
            dbHelper.updateCurrentWebPageId(webPage.novelId, webPage.id)
        if (webPage.id != -1L) {
            webPage.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage)
        }
        fabClean.show()
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
            R.id.action_report_page -> reportPage()
            R.id.action_open_in_browser -> inBrowser()
            R.id.action_share -> share()
        }
    }

    private fun toggleDarkTheme() {
        dataCenter.isDarkTheme = !dataCenter.isDarkTheme
        (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.applyTheme()
        (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.loadDocument()
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

    private fun reportPage() {
        val url = (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        if (url != null) {
            val email = getString(R.string.dev_email)
            val subject = "[IMPROVEMENT]"
            val body = "Url: $url \n Please improve the viewing experience of this page."
            sendEmail(email, subject, body)
        }
    }

    private fun inBrowser() {
        val url = (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        if (url != null)
            openInBrowser(url)
    }

    private fun share() {
        val url = (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        if (url != null) {
            shareUrl(url)
        }
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


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        val webView = (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.view?.findViewById<WebView>(R.id.readerWebView)
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    webView?.pageUp(false)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    webView?.pageDown(false)
                }
                return true
            }
            else -> return super.dispatchKeyEvent(event)
        }
    }

//    override fun onSaveInstanceState(outState: Bundle?) {
//        super.onSaveInstanceState(outState)
//        outState?.putSerializable("novel", novel)
//        outState?.putSerializable("webPage", webPage)
//        outState?.putSerializable("chapters", chapters)
//    }
//
//    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
//        super.onRestoreInstanceState(savedInstanceState)
//        if (savedInstanceState != null) {
//            novel = savedInstanceState.getSerializable("novel") as Novel?
//            webPage = savedInstanceState.getSerializable("webPage") as WebPage?
//            @Suppress("UNCHECKED_CAST")
//            chapters = savedInstanceState.getSerializable("chapters") as ArrayList<WebPage>
//        }
//    }

    override fun onBackPressed() {
        if ((viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).history.isNotEmpty())
            (viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).goBack()
        else
            super.onBackPressed()
    }

}