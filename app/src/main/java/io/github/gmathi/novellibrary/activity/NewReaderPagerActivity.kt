package io.github.gmathi.novellibrary.activity

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.CompoundButton
import android.widget.SeekBar
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateBookmarkCurrentWebPageId
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.WebPageDBFragment
import io.github.gmathi.novellibrary.fragment.WebPageFragment
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import kotlinx.android.synthetic.main.activity_new_reader_pager.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.android.synthetic.main.menu_left_drawer.*
import org.greenrobot.eventbus.EventBus
import java.util.*

class NewReaderPagerActivity : DrawerAdapter.OnItemSelectedListener, SimpleItem.Listener<ReaderMenu>, BaseActivity(), ViewPager.OnPageChangeListener, SeekBar.OnSeekBarChangeListener {

    private var slidingRootNav: SlidingRootNav? = null
    lateinit var recyclerView: RecyclerView

    private val posReaderMode = 0
    private val posFonts = 1
    private val posFontSize = 2
    private val posReportPage = 3
    private val posOpenInBrowser = 4
    private val posShareChapter = 5

    private var screenTitles: Array<String>? = null
    lateinit var screenIcons: Array<Drawable?>
    lateinit var novel: Novel
    lateinit var webPage: WebPage

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reader_pager)
        slideMenuSetup(savedInstanceState)

        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        slideMenuAdapterSetup()

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val i_novel = intent.getSerializableExtra("novel") as Novel?
        val i_webPage = intent.getSerializableExtra("webPage") as WebPage?

        if (i_novel == null || i_webPage == null) finish()

        novel = i_novel!!
        webPage = i_webPage!!
        loadData()

        menuNav.setOnClickListener({toggleSlideRootNab()})
        applyMenuTint()
    }

    fun applyMenuTint() {
        if (dataCenter.isDarkTheme)
            menuNav.setColorFilter(getResources().getColor(R.color.white), android.graphics.PorterDuff.Mode.MULTIPLY);
        else
            menuNav.setColorFilter(getResources().getColor(R.color.black), android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    private fun loadData() {

        async {

            chapters = await { ArrayList(NovelApi().getChapterUrls(novel)) }
            adapter = WebPageAdapter(supportFragmentManager, chapters)
            viewPager.addOnPageChangeListener(this@NewReaderPagerActivity)
            viewPager.adapter = adapter

            viewPager.currentItem = chapters.indexOf(webPage)
            if (webPage.novelId != -1L && webPage.id != -1L)
                dbHelper.updateBookmarkCurrentWebPageId(webPage.novelId, webPage.id)
            if (webPage.id != -1L) {
                webPage.isRead = 1
                dbHelper.updateWebPageReadStatus(webPage)
            }
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
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }


    private fun toggleDarkTheme() {
        applyMenuTint()
        dataCenter.isDarkTheme = !dataCenter.isDarkTheme
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

    override fun onBackPressed() {
        if ((viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).history.isNotEmpty())
            (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageFragment).goBack()
        else
            super.onBackPressed()
    }

    fun checkUrl(url: String): Boolean {
        val index = chapters.indexOfFirst { it.redirectedUrl != null && it.redirectedUrl!!.contains(url) }
        if (index != -1) {
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
        } else return false
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

    private fun slideMenuSetup(savedInstanceState: Bundle?) {
        slidingRootNav = SlidingRootNavBuilder(this)
                .withMenuOpened(false)
                .withContentClickableWhenMenuOpened(true)
                .withSavedState(savedInstanceState)
                .withGravity(SlideGravity.RIGHT)
                .withMenuLayout(R.layout.menu_left_drawer)
                .inject()
    }

    private fun slideMenuAdapterSetup() {
        val adapter = DrawerAdapter(Arrays.asList(
                createItemFor(posReaderMode).setSwitchOn(true),
                createItemFor(posFonts),
                createItemFor(posFontSize),
                createItemFor(posReportPage),
                createItemFor(posOpenInBrowser),
                createItemFor(posShareChapter)
        ) as List<DrawerItem<DrawerAdapter.ViewHolder>>)
        adapter.setListener(this)

        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

    }

    private fun createItemFor(position: Int): DrawerItem<SimpleItem.ViewHolder> {
        return SimpleItem(ReaderMenu(screenIcons[position]!!, screenTitles!![position]), this)


    }

    private fun toggleSlideRootNab() {
        if (slidingRootNav!!.isMenuOpened)
            slidingRootNav!!.closeMenu()
        else
            slidingRootNav!!.openMenu()
    }

    private fun loadScreenTitles(): Array<String> {
        return resources.getStringArray(R.array.ld_activityScreenTitles)
    }

    private fun loadScreenIcons(): Array<Drawable?> {
        val ta = resources.obtainTypedArray(R.array.ld_activityScreenIcons)
        val icons = arrayOfNulls<Drawable>(ta.length())
        for (i in 0 until ta.length()) {
            val id = ta.getResourceId(i, 0)
            if (id != 0) {
                icons[i] = ContextCompat.getDrawable(this, id)
            }
        }
        ta.recycle()
        return icons
    }

    override fun onItemSelected(position: Int) {

        slidingRootNav!!.closeMenu()
        when (position) {
            posReaderMode -> {
            }
            posFonts -> {
                toast("Font Locked")
            }
            posFontSize -> {
                changeTextSize()
            }
            posReportPage -> {
                reportPage()
            }
            posOpenInBrowser -> {
                inBrowser()
            }
            posShareChapter -> {
                share()
            }
        }
    }

    override fun bind(item: ReaderMenu, itemView: View, position: Int, simpleItem: SimpleItem) {

        itemView.title.text = item.title
        itemView.icon.setImageDrawable(item.icon)
        if (simpleItem.isSwitchOn()) {
            itemView.titleNightMode.text = getString(R.string.title_night)
            itemView.switchReaderMode.visibility = View.VISIBLE
            itemView.switchReaderMode.isChecked = dataCenter.cleanChapters
            itemView.switchNightMode.isChecked = dataCenter.isDarkTheme
            if (itemView.switchReaderMode.isChecked)
                itemView.linNightMode.visibility = View.VISIBLE
        } else
            itemView.switchReaderMode.visibility = View.GONE


        itemView.switchReaderMode.setOnCheckedChangeListener({ _: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                dataCenter.cleanChapters = true
                (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment).cleanPage()
                itemView.linNightMode.visibility = View.VISIBLE
            } else
                itemView.linNightMode.visibility = View.GONE
        })
        itemView.switchNightMode.setOnCheckedChangeListener({ _: CompoundButton, _: Boolean ->
            toggleDarkTheme()
        })
    }

}
