package io.github.gmathi.novellibrary.activity

import android.os.Build
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.SeekBar
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.WebPageAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateBookmarkCurrentWebPageId
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.WebPageFragment
import io.github.gmathi.novellibrary.model.NightModeChangeEvent
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import kotlinx.android.synthetic.main.activity_reader_pager.*
import org.greenrobot.eventbus.EventBus


class ReaderPagerActivity : BaseActivity(), ViewPager.OnPageChangeListener, FloatingToolbar.ItemClickListener, SeekBar.OnSeekBarChangeListener {

    lateinit var novel: Novel
    lateinit var webPage: WebPage

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val i_novel = intent.getSerializableExtra("novel") as Novel?
        val i_webPage = intent.getSerializableExtra("webPage") as WebPage?

        if (i_novel == null || i_webPage == null) finish()

        novel = i_novel!!
        webPage = i_webPage!!

        loadData()
    }

    private fun loadData() {

        async {

            chapters = await { ArrayList(NovelApi().getChapterUrls(novel)) }
            adapter = WebPageAdapter(supportFragmentManager, chapters)
            viewPager.addOnPageChangeListener(this@ReaderPagerActivity)
            viewPager.adapter = adapter

            viewPager.currentItem = chapters.indexOf(webPage)
            if (webPage.novelId != -1L && webPage.id != -1L)
                dbHelper.updateBookmarkCurrentWebPageId(webPage.novelId, webPage.id)
            if (webPage.id != -1L) {
                webPage.isRead = 1
                dbHelper.updateWebPageReadStatus(webPage)
            }

            floatingToolbar.attachFab(fab)
            floatingToolbar.setClickListener(this@ReaderPagerActivity)

            fabClean.setOnClickListener {
                (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).cleanPage()
                fabClean.visibility = View.INVISIBLE
            }

        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && dataCenter.enableImmersiveMode) {
            val immersiveModeOptions: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                main_content.fitsSystemWindows = false

                immersiveModeOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } else {
                immersiveModeOptions = (View.SYSTEM_UI_FLAG_LOW_PROFILE)
            }

            window.decorView.systemUiVisibility = immersiveModeOptions
        }
    }

    override fun onPageSelected(position: Int) {
        val webPage = chapters[position]
        if (webPage.novelId != -1L && webPage.id != -1L)
            dbHelper.updateBookmarkCurrentWebPageId(webPage.novelId, webPage.id)
        if (webPage.id != -1L) {
            webPage.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage)
        }
        //fabClean.visibility = View.VISIBLE
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
        //(viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.applyTheme()
        //(viewPager.adapter.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.loadDocument()
        EventBus.getDefault().post(NightModeChangeEvent())
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
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        val chapterName = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.webPage?.chapter
        if (url != null) {
            val email = getString(R.string.dev_email)
            val subject = "[IMPROVEMENT]"
            val body = StringBuilder()
            body.append("Please improve the viewing experience of this page.\n")
            body.append("Novel Name: ${novel.name} \n")
            body.append("Novel Url: ${novel.url} \n")
            body.append("Chapter Name: $chapterName \n ")
            body.append("Chapter Url: $url \n ")
            sendEmail(email, subject, body.toString())
        }
    }

    private fun inBrowser() {
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        if (url != null)
            openInBrowser(url)
    }

    private fun share() {
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.getUrl()
        if (url != null) {
            shareUrl(url)
        }
    }


    //region SeekBar Progress Listener
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        dataCenter.textSize = progress
        (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.changeTextSize(progress)
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }
    //endregion


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        val webView = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment?)?.view?.findViewById<WebView>(R.id.readerWebView)
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    webView?.pageUp(false)
                }
                return dataCenter.volumeScroll
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    webView?.pageDown(false)
                }
                return dataCenter.volumeScroll
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
        if ((viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).history.isNotEmpty())
            (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).goBack()
        else
            super.onBackPressed()
    }

    fun checkUrl(url: String): Boolean {
        val index = chapters.indexOfFirst { it.redirectedUrl != null && it.redirectedUrl!!.contains(url) }
        if (index == -1)
            return false

        viewPager.currentItem = index
        val webPage = chapters[index]
        if (webPage.novelId != -1L && webPage.id != -1L) {
            dbHelper.updateBookmarkCurrentWebPageId(webPage.novelId, webPage.id)
        }
        if (webPage.id != -1L) {
            webPage.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage)
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }
}