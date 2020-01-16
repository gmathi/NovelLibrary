package io.github.gmathi.novellibrary.activity


import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.folderselector.FileChooserDialog
import com.crashlytics.android.Crashlytics
import com.thanosfisherman.mayi.Mayi
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageFragmentPageListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.alertToast
import io.github.gmathi.novellibrary.extensions.openInBrowser
import io.github.gmathi.novellibrary.extensions.shareUrl
import io.github.gmathi.novellibrary.extensions.startTTSService
import io.github.gmathi.novellibrary.fragment.WebPageDBFragment
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_reader_pager.*
import kotlinx.android.synthetic.main.item_option.view.*
import kotlinx.android.synthetic.main.menu_left_drawer.*
import org.greenrobot.eventbus.EventBus
import java.io.File


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
        private const val READER_MODE = 0
        private const val JAVA_SCRIPT = 1
        private const val FONTS = 2
        private const val FONT_SIZE = 3
        private const val MERGE_PAGES = 4
        private const val REPORT_PAGE = 5
        private const val OPEN_IN_BROWSER = 6
        private const val SHARE_CHAPTER = 7
        private const val READ_ALOUD = 8

        private const val VOLUME_SCROLL_STEP = 500
    }

    private lateinit var screenIcons: Array<Drawable?>
    lateinit var novel: Novel

    private var screenTitles: Array<String>? = null
    private var adapter: GenericFragmentStatePagerAdapter? = null
    private var webPage: WebPage? = null
    private var sourceId: Long = -1L
    private var totalSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)
        sourceId = intent.getLongExtra("sourceId", -1L)

        val tempNovel = intent.getParcelableExtra<Novel?>("novel")
        if (tempNovel == null || tempNovel.chaptersCount.toInt() == 0) {
            finish()
            return
        } else
            novel = tempNovel

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        calculateNovelVariables()

        slideMenuSetup(savedInstanceState)
        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        slideMenuAdapterSetup()
        menuNav.setOnClickListener {
            toggleSlideRootNab()
        }

        if (!dataCenter.isReaderModeButtonVisible)
            menuNav.visibility = View.INVISIBLE

    }

    private fun calculateNovelVariables() {
        dbHelper.updateNewReleasesCount(novel.id, 0L)
        webPage = if (novel.currentWebPageUrl != null)
            dbHelper.getWebPage(novel.currentWebPageUrl!!)
        else
            dbHelper.getWebPage(novel.id, sourceId, 0)
        totalSize = dbHelper.getWebPagesCount(novel.id, sourceId)
        adapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, totalSize, WebPageFragmentPageListener(novel, sourceId))
        viewPager.addOnPageChangeListener(this)
        viewPager.adapter = adapter
        val index = dbHelper.getAllWebPages(novel.id, sourceId).indexOfFirst { it.url == webPage?.url }
        if (webPage != null) {
            updateBookmark(webPage!!)
            viewPager.currentItem =
                    if (dataCenter.japSwipe)
                        totalSize - index - 1
                    else
                        index
        }


    }

    private fun updateBookmark(webPage: WebPage) {
        if (novel.id != -1L) {
            dbHelper.updateBookmarkCurrentWebPageUrl(novel.id, webPage.url)
            val webPageSettings = dbHelper.getWebPageSettings(webPage.url)
            if (webPageSettings != null) {
                webPageSettings.isRead = 1
                dbHelper.updateWebPageSettingsReadStatus(webPageSettings)
            }

        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && dataCenter.enableImmersiveMode) {
            main_content.fitsSystemWindows = false
            val immersiveModeOptions: Int = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = immersiveModeOptions
        }
    }

    override fun onPageSelected(position: Int) {
        val newTotalSize = dbHelper.getWebPagesCount(novel.id, sourceId)
        if (newTotalSize != totalSize) {
            calculateNovelVariables()
        }
        val offset = if (dataCenter.japSwipe) totalSize - position - 1 else position
        val webPage = dbHelper.getWebPage(novel.id, sourceId, offset)
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
        MaterialDialog.Builder(this)
                .content("Please use discord to report a bug.")
                .positiveText("Ok")
                .onPositive { dialog, _ -> dialog.dismiss() }
                .show()
//        val url = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
//        val chapterName = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as WebPageDBFragment?)?.webPage?.chapter
//        if (url != null) {
//            val email = getString(R.string.dev_email)
//            val subject = "[IMPROVEMENT]"
//            val body = StringBuilder()
//            body.append("Please improve the viewing experience of this page.\n")
//            body.append("Novel Name: ${novel?.name} \n")
//            body.append("Novel Url: ${novel?.url} \n")
//            body.append("Chapter Name: $chapterName \n ")
//            body.append("Chapter Url: $url \n ")
//            sendEmail(email, subject, body.toString())
//        }
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
                    val anim = ObjectAnimator.ofInt(webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) - VOLUME_SCROLL_STEP)
                    anim.setDuration(500).start()
                    //webView?.scrollBy(0, -VOLUME_SCROLL_STEP)
                }
                dataCenter.volumeScroll
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    val anim = ObjectAnimator.ofInt(webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) + VOLUME_SCROLL_STEP)
                    anim.setDuration(500).start()
                    //webView?.scrollBy(0, VOLUME_SCROLL_STEP)
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
        val webPageSettings = dbHelper.getWebPageSettingsByRedirectedUrl(url) ?: return false
        val webPage = dbHelper.getWebPage(webPageSettings.url) ?: return false
        val index = dbHelper.getAllWebPages(novel.id, sourceId).indexOf(webPage)
        return if (index == -1)
            false
        else {
            viewPager.currentItem = index
            updateBookmark(webPage)
            true
        }
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
        val adapter = DrawerAdapter(listOf(
                createItemFor(READER_MODE).setSwitchOn(true),
                createItemFor(JAVA_SCRIPT).setSwitchOn(true),
                createItemFor(FONTS),
                createItemFor(FONT_SIZE),
                createItemFor(MERGE_PAGES).setSwitchOn(true),
                createItemFor(REPORT_PAGE),
                createItemFor(OPEN_IN_BROWSER),
                createItemFor(SHARE_CHAPTER),
                createItemFor(READ_ALOUD)
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
        return resources.getStringArray(R.array.reader_mode_menu_titles_list)
    }

    private fun loadScreenIcons(): Array<Drawable?> {
        val ta = resources.obtainTypedArray(R.array.reader_mode_menu_icons_list)
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
                        .onRationale { _, token ->
                            token.continuePermissionRequest()
                        }
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
            READ_ALOUD -> {
                if (dataCenter.readerMode) {
                    val webPageDBFragment = (viewPager.adapter?.instantiateItem(viewPager, viewPager.currentItem) as? WebPageDBFragment)
                    val audioText = webPageDBFragment?.doc?.text() ?: return
                    val title = webPageDBFragment.doc?.title() ?: ""
                    startTTSService(audioText, title, novel.id)
                } else {
                    alertToast(title = "Read Aloud", message = "Only supported in Reader Mode!")
                }
            }
        }
    }

    /**
     *     For Reader Mode & Night Mode toggle
     */
    override fun bind(item: ReaderMenu, itemView: View, position: Int, simpleItem: SimpleItem) {

        itemView.title.text = item.title
        itemView.icon.setImageDrawable(item.icon)
        itemView.switchReaderMode.setOnCheckedChangeListener(null)
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
        } else if (simpleItem.isSwitchOn() && position == MERGE_PAGES) {
            itemView.switchReaderMode.visibility = View.VISIBLE
            itemView.switchReaderMode.isChecked = dataCenter.enableClusterPages
        } else
            itemView.switchReaderMode.visibility = View.GONE


        itemView.switchReaderMode.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            when (position) {
                READER_MODE -> {
                    dataCenter.readerMode = isChecked
                    itemView.linNightMode.visibility = if (isChecked)
                        View.VISIBLE
                    else
                        View.GONE
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
                }
                JAVA_SCRIPT -> {
                    dataCenter.javascriptDisabled = !isChecked
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
                }
                MERGE_PAGES -> {
                    dataCenter.enableClusterPages = isChecked
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
                }
            }
        }

        itemView.switchNightMode.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            dataCenter.isDarkTheme = isChecked
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
        }

    }

    private fun openFontChooserDialog() {
        try {
            val externalDirectory = getExternalFilesDir(null)
            if (externalDirectory != null && externalDirectory.exists())
                FileChooserDialog.Builder(this)
                        .initialPath(externalDirectory.path)  // changes initial path, defaults to external storage directory
                        .extensionsFilter(".ttf") // Optional extension filter, will override mimeType()
                        .tag("optional-identifier")
                        .goUpLabel("Up") // custom go up label, default label is "..."
                        .show(this) // an AppCompatActivity which implements FileCallback
            else
                MaterialDialog.Builder(this)
                        .content("Cannot find the internal storage or sd card. Please check your storage settings.")
                        .positiveText(getString(R.string.okay)).onPositive { dialog, _ -> dialog.dismiss() }
                        .show()
        } catch (e: Exception) {
            Crashlytics.logException(e)
        }
    }

    override fun onFileSelection(dialog: FileChooserDialog, file: File) {
        dialog.dismiss()
        dataCenter.fontPath = file.path
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
    }

    override fun onFileChooserDismissed(dialog: FileChooserDialog) {
        //Do Nothing
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.IWV_ACT_REQ_CODE) {
            Handler(Looper.getMainLooper()).post {
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        novel.metaData[Constants.MetaDataKeys.LAST_READ_DATE] = Utils.getCurrentFormattedDate()
        dbHelper.updateNovelMetaData(novel)
    }

}