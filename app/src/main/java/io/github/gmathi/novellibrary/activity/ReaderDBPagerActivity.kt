package io.github.gmathi.novellibrary.activity


import android.Manifest
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.thanosfisherman.mayi.Mayi
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageFragmentPageListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.getWebPageByRedirectedUrl
import io.github.gmathi.novellibrary.database.updateBookmarkCurrentWebPageId
import io.github.gmathi.novellibrary.database.updateWebPageReadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.WebPageDBFragment
import io.github.gmathi.novellibrary.model.*
import kotlinx.android.synthetic.main.activity_new_reader_pager.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.android.synthetic.main.menu_left_drawer.*
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*


class ReaderDBPagerActivity :
    BaseActivity(),
    ViewPager.OnPageChangeListener,
    DrawerAdapter.OnItemSelectedListener,
    SimpleItem.Listener<ReaderMenu>,
    SeekBar.OnSeekBarChangeListener,
    FileChooserDialog.FileCallback {

    private var slidingRootNav: SlidingRootNav? = null
    lateinit var recyclerView: RecyclerView

    companion object {
        private val READER_MODE = 0
        private val JAVA_SCRIPT = 1
        private val FONTS = 2
        private val FONT_SIZE = 3
        private val REPORT_PAGE = 4
        private val OPEN_IN_BROWSER = 5
        private val SHARE_CHAPTER = 6
    }

    private var screenTitles: Array<String>? = null
    private lateinit var screenIcons: Array<Drawable?>

    var novel: Novel? = null
    var webPage: WebPage? = null

    private var adapter: GenericFragmentStatePagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reader_pager)
        novel = intent.getSerializableExtra("novel") as Novel?

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (novel == null || novel?.chapterCount?.toInt() == 0) finish()
        adapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, novel!!.chapterCount.toInt(), WebPageFragmentPageListener(novel!!))
        viewPager.addOnPageChangeListener(this)
        viewPager.adapter = adapter

        webPage = if (novel!!.currentWebPageId != -1L)
            dbHelper.getWebPage(novel!!.currentWebPageId)
        else
            dbHelper.getWebPage(novel!!.id, 0)

        if (webPage != null) {
            updateBookmark(webPage!!)
            viewPager.currentItem =
                if (dataCenter.japSwipe)
                    novel!!.chapterCount.toInt() - webPage!!.orderId.toInt() - 1
                else
                    webPage!!.orderId.toInt()
        }

        slideMenuSetup(savedInstanceState)

        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        slideMenuAdapterSetup()
        menuNav.setOnClickListener {
            toggleSlideRootNab()
        }
    }

    private fun updateBookmark(webPage: WebPage) {
        if (webPage.novelId != -1L && webPage.id != -1L)
            dbHelper.updateBookmarkCurrentWebPageId(webPage.novelId, webPage.id)
        if (webPage.id != -1L) {
            webPage.isRead = 1
            dbHelper.updateWebPageReadStatus(webPage)
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
        val orderId = if (dataCenter.japSwipe) novel!!.chapterCount.toInt() - position - 1 else position
        val webPage = dbHelper.getWebPage(novel!!.id, orderId.toLong())
        if (webPage != null) updateBookmark(webPage)
        //fabClean.visibility = View.VISIBLE
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }

    private fun changeTextSize() {
        val dialog = MaterialDialog.Builder(this)
            .title(R.string.text_size)
            .customView(R.layout.dialog_text_slider, true)
            .build()
        dialog.show()
        dialog.customView?.findViewById<SeekBar>(R.id.fontSeekBar)?.setOnSeekBarChangeListener(this)
        dialog.customView?.findViewById<SeekBar>(R.id.fontSeekBar)?.progress = dataCenter.textSize
    }

    private fun reportPage() {
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        val chapterName = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.webPage?.chapter
        if (url != null) {
            val email = getString(R.string.dev_email)
            val subject = "[IMPROVEMENT]"
            val body = StringBuilder()
            body.append("Please improve the viewing experience of this page.\n")
            body.append("Novel Name: ${novel?.name} \n")
            body.append("Novel Url: ${novel?.url} \n")
            body.append("Chapter Name: $chapterName \n ")
            body.append("Chapter Url: $url \n ")
            sendEmail(email, subject, body.toString())
        }
    }

    private fun inBrowser() {
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        if (url != null)
            openInBrowser(url)
    }

    private fun share() {
        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        if (url != null) {
            shareUrl(url)
        }
    }


    //region SeekBar Progress Listener
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        dataCenter.textSize = progress
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.TEXT_SIZE))
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
    }
    //endregion


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        val webView = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.view?.findViewById<WebView>(R.id.readerWebView)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    webView?.pageUp(false)
                }
                dataCenter.volumeScroll
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    webView?.pageDown(false)
                }
                dataCenter.volumeScroll
            }
            else -> super.dispatchKeyEvent(event)
        }
    }


    override fun onBackPressed() {
        val currentFrag = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment)
        when {
            currentFrag.history.isNotEmpty() -> currentFrag.goBack()
        //currentFrag.readerWebView.canGoBack() -> currentFrag.readerWebView.goBack()
            else -> super.onBackPressed()
        }
    }

    fun checkUrl(url: String): Boolean {
        val webPage = dbHelper.getWebPageByRedirectedUrl(novel!!.id, url) ?: return false

        viewPager.currentItem = webPage.orderId.toInt()
        updateBookmark(webPage)
        return true
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
        @Suppress("UNCHECKED_CAST")
        val adapter = DrawerAdapter(Arrays.asList(
            createItemFor(READER_MODE).setSwitchOn(true),
            createItemFor(JAVA_SCRIPT).setSwitchOn(true),
            createItemFor(FONTS),
            createItemFor(FONT_SIZE),
            createItemFor(REPORT_PAGE),
            createItemFor(OPEN_IN_BROWSER),
            createItemFor(SHARE_CHAPTER)
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

    /**
     *     Handle Slide Menu Nav Options
     */
    override fun onItemSelected(position: Int) {
        slidingRootNav!!.closeMenu()
        when (position) {
            FONTS -> {
                Mayi.withActivity(this@ReaderDBPagerActivity)
                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .onResult {
                        if (it.isGranted)
                            openFontChooserDialog()
                        else
                            MaterialDialog.Builder(this)
                                .content("Enable \"Write External Storage\" permission for Novel Library " +
                                    "from your device Settings -> Applications -> Novel Library -> Permissions")
                                .positiveText(getString(R.string.okay)).onPositive { dialog, _ -> dialog.dismiss() }
                                .show()
                    }.check()
            }
            FONT_SIZE -> changeTextSize()
            REPORT_PAGE -> reportPage()
            OPEN_IN_BROWSER -> inBrowser()
            SHARE_CHAPTER -> share()
        }
    }

    /**
     *     For Reader Mode & Night Mode toggle
     */
    override fun bind(item: ReaderMenu, itemView: View, position: Int, simpleItem: SimpleItem) {

        itemView.title.text = item.title
        itemView.icon.setImageDrawable(item.icon)
        if (simpleItem.isSwitchOn() && position == READER_MODE) {
            itemView.titleNightMode.text = getString(R.string.title_night)
            itemView.switchReaderMode.visibility = View.VISIBLE
            itemView.switchReaderMode.isChecked = dataCenter.readerMode
            itemView.switchNightMode.isChecked = dataCenter.isDarkTheme
            if (itemView.switchReaderMode.isChecked)
                itemView.linNightMode.visibility = View.VISIBLE
        } else if (simpleItem.isSwitchOn() && position == JAVA_SCRIPT) {
            itemView.switchReaderMode.visibility = View.VISIBLE
            itemView.switchReaderMode.isChecked = !dataCenter.javascriptDisabled
        } else
            itemView.switchReaderMode.visibility = View.GONE


        itemView.switchReaderMode.setOnCheckedChangeListener({ _: CompoundButton, isChecked: Boolean ->
            if (position == READER_MODE) {
                dataCenter.readerMode = isChecked
                itemView.linNightMode.visibility = if (isChecked)
                    View.VISIBLE
                else
                    View.GONE
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
            } else if (position == JAVA_SCRIPT) {
                dataCenter.javascriptDisabled = !isChecked
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
            }
        })

        itemView.switchNightMode.setOnCheckedChangeListener({ _: CompoundButton, isChecked: Boolean ->
            dataCenter.isDarkTheme = isChecked
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
        })

    }

    private fun openFontChooserDialog() {
        FileChooserDialog.Builder(this)
            .initialPath(Environment.getExternalStorageDirectory().path)  // changes initial path, defaults to external storage directory
            .extensionsFilter(".ttf") // Optional extension filter, will override mimeType()
            .tag("optional-identifier")
            .goUpLabel("Up") // custom go up label, default label is "..."
            .show(this) // an AppCompatActivity which implements FileCallback
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        dialog.dismiss()
        dataCenter.fontPath = file.path
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
    }

    override fun onFileChooserDismissed(dialog: FileChooserDialog) {
        //Do Nothing
    }

}