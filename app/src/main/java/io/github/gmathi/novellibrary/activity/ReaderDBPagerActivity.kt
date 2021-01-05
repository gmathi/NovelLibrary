package io.github.gmathi.novellibrary.activity


import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView.LayoutParams.WRAP_CONTENT
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.internal.main.DialogTitleLayout
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.logEvent
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageFragmentPageListener
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityReaderPagerBinding
import io.github.gmathi.novellibrary.databinding.ItemOptionBinding
import io.github.gmathi.novellibrary.databinding.MenuLeftDrawerBinding
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.fragment.WebPageDBFragment
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import io.github.gmathi.novellibrary.model.ui.DrawerItem
import io.github.gmathi.novellibrary.model.ui.ReaderMenu
import io.github.gmathi.novellibrary.model.ui.SimpleItem
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_STEP
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class ReaderDBPagerActivity :
    BaseActivity(),
    ViewPager.OnPageChangeListener,
    DrawerAdapter.OnItemSelectedListener,
    SimpleItem.Listener<ReaderMenu> {


    companion object {
        private const val TAG = "ReaderDBPagerActivity"

        private const val READER_MODE = 0
        private const val NIGHT_MODE = 1
        private const val JAVA_SCRIPT = 2
        private const val FONTS = 3
        private const val FONT_SIZE = 4
        private const val REPORT_PAGE = 5
        private const val OPEN_IN_BROWSER = 6
        private const val SHARE_CHAPTER = 7
        private const val MORE_SETTINGS = 8
        private const val READ_ALOUD = 9


        private val FONT_MIME_TYPES = arrayOf(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("ttf") ?: "application/x-font-ttf",
            "fonts/ttf",
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("otf") ?: "application/x-font-opentype",
            "fonts/otf",
            "application/octet-stream"
        )

        private val AVAILABLE_FONTS = linkedMapOf<String, String>()
    }

    private lateinit var slidingRootNav: SlidingRootNav
    private lateinit var screenTitles: Array<String>
    private lateinit var screenIcons: Array<Drawable?>

    private lateinit var novel: Novel
    private lateinit var adapter: GenericFragmentStatePagerAdapter

    private var sourceId: Long = -1L
    private var webPages: List<WebPage> = ArrayList()
    
    lateinit var binding: ActivityReaderPagerBinding
    private lateinit var bindingList: MenuLeftDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReaderPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //Read Intent Extras
        sourceId = intent.getLongExtra("sourceId", -1L)
        val tempNovel = intent.getSerializableExtra("novel") as Novel?

        //Check if it is Valid Novel
        if (tempNovel == null || tempNovel.chaptersCount.toInt() == 0) {
            finish()
            return
        } else
            novel = tempNovel

        //Get all WebPages & set view pager
        webPages = db.webPageDao().findByNovelAndSourceId(novel.id, sourceId)
        if (dataCenter.japSwipe)
            webPages = webPages.reversed()

        adapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, webPages.size, WebPageFragmentPageListener(novel, webPages))
        binding.viewPager.addOnPageChangeListener(this)
        binding.viewPager.adapter = adapter

        //Set the current page to the bookmarked webPage
        novel.currentChapterUrl?.let { bookmarkUrl ->
            val index = webPages.indexOfFirst { it.url == bookmarkUrl }
            if (index != -1) binding.viewPager.currentItem = index
            if (index == 0) updateBookmark(webPages[0])
        }

        //Set up the Slide-Out Reader Menu.
        slideMenuSetup(savedInstanceState)
        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        slideMenuAdapterSetup()
        binding.menuNav.setOnClickListener {
            toggleSlideRootNab()
        }

        // Easy access to open reader menu button
        if (!dataCenter.isReaderModeButtonVisible)
            binding.menuNav.visibility = INVISIBLE
    }

    private fun updateBookmark(webPage: WebPage) {
        firebaseAnalytics.logEvent(FAC.Event.READ_NOVEL) {
            param(FAC.Param.NOVEL_NAME, novel.name)
            param(FAC.Param.NOVEL_URL, novel.url)
        }
        novel.currentChapterUrl = webPage.url
        db.novelDao().update(novel)
        NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncBookmarks(it.host)) it.setBookmark(novel, webPage) }
        val webPageSettings = db.webPageSettingsDao().findOneByUrl(webPage.url)
        if (webPageSettings != null) {
            webPageSettings.isRead = 1
            db.updateWebPageSettingsReadStatus(webPageSettings)
        }
    }

    @Suppress("DEPRECATION")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus && dataCenter.enableImmersiveMode) {
            binding.mainContent.fitsSystemWindows = false
            window.decorView.systemUiVisibility = Constants.IMMERSIVE_MODE_FLAGS
        }
    }

    override fun onPageSelected(position: Int) {
        updateBookmark(webPage = webPages[position])
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }

    private fun changeTextSize() {
        val dialog = MaterialDialog(this).show {
            title(R.string.text_size)
            customView(R.layout.dialog_slider, scrollable = true)

            getCustomView()?.findViewById<TwoWaySeekBar>(R.id.seekBar)?.setOnSeekBarChangedListener { _, progress ->
                dataCenter.textSize = progress.toInt()
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.TEXT_SIZE))
            }
            getCustomView()?.findViewById<TwoWaySeekBar>(R.id.seekBar)?.setProgress(dataCenter.textSize.toDouble())
        }

    }

    private fun reportPage() {
        MaterialDialog(this).show {
            message(text = "Please use discord to report a bug.")
            positiveButton(text = "Ok") {
                it.dismiss()
            }
        }
    }

    private fun inBrowser() {
        val url = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        if (url != null)
            openInBrowser(url)
    }

    private fun share() {
        val url = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        if (url != null) {
            shareUrl(url)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        val webView = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as WebPageDBFragment?)?.view?.findViewById<WebView>(R.id.readerWebView)
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    val anim = ObjectAnimator.ofInt(
                        webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) - dataCenter.scrollLength * VOLUME_SCROLL_LENGTH_STEP
                    )
                    anim.setDuration(500).start()
                }
                dataCenter.volumeScroll
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.volumeScroll) {
                    val anim = ObjectAnimator.ofInt(
                        webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) + dataCenter.scrollLength * VOLUME_SCROLL_LENGTH_STEP
                    )
                    anim.setDuration(500).start()
                }
                dataCenter.volumeScroll
            }
            else -> super.dispatchKeyEvent(event)
        }
    }


    override fun onBackPressed() {
        val currentFrag = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as WebPageDBFragment)
        when {
            currentFrag.history.isNotEmpty() -> currentFrag.goBack()
            //currentFrag.readerWebView.canGoBack() -> currentFrag.readerWebView.goBack()
            else -> super.onBackPressed()
        }
    }

    fun checkUrl(url: String): Boolean {
        val webPageSettings = db.webPageSettingsDao().findOneByRedirectUrl(url) ?: return false
        val webPage = db.webPageDao().findOneByUrl(webPageSettings.url) ?: return false
        val index = db.webPageDao().findByNovelAndSourceId(novel.id, sourceId).indexOf(webPage)
        return if (index == -1)
            false
        else {
            binding.viewPager.currentItem = index
            updateBookmark(webPage)
            true
        }
    }

    private fun slideMenuSetup(savedInstanceState: Bundle?) {
        val slidingRootNavBuilder = SlidingRootNavBuilder(this)
            .withMenuOpened(false)
            .withContentClickableWhenMenuOpened(true)
            .withSavedState(savedInstanceState)
            .withGravity(SlideGravity.RIGHT)
            .withMenuLayout(R.layout.menu_left_drawer)
        slidingRootNav = slidingRootNavBuilder.inject()

        var view = slidingRootNav.layout.getChildAt(0)
        bindingList = MenuLeftDrawerBinding.bind(view)
    }

    private fun slideMenuAdapterSetup() {
        @Suppress("UNCHECKED_CAST")
        val adapter = DrawerAdapter(
            listOf(
                createItemFor(READER_MODE).setSwitchOn(true),
                createItemFor(NIGHT_MODE).setSwitchOn(true),
                createItemFor(JAVA_SCRIPT).setSwitchOn(true),
                createItemFor(FONTS),
                createItemFor(FONT_SIZE),
                createItemFor(REPORT_PAGE),
                createItemFor(OPEN_IN_BROWSER),
                createItemFor(SHARE_CHAPTER),
                createItemFor(MORE_SETTINGS),
                createItemFor(READ_ALOUD)

            ) as List<DrawerItem<DrawerAdapter.ViewHolder>>
        )
        adapter.setListener(this)
        
        bindingList.list.isNestedScrollingEnabled = false
        bindingList.list.layoutManager = LinearLayoutManager(this)
        bindingList.list.adapter = adapter
    }

    private fun createItemFor(position: Int): DrawerItem<SimpleItem.ViewHolder> {
        return SimpleItem(ReaderMenu(screenIcons[position]!!, screenTitles[position]), this)
    }

    private fun toggleSlideRootNab() {
        if (slidingRootNav.isMenuOpened)
            slidingRootNav.closeMenu()
        else
            slidingRootNav.openMenu()
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

    private fun createTypeface(path: String = dataCenter.fontPath): Typeface {
        return if (path.startsWith("/android_asset"))
            Typeface.createFromAsset(assets, path.substringAfter('/').substringAfter('/'))
        else
            Typeface.createFromFile(path)
    }

    /**
     *     Handle Slide Menu Nav Options
     */
    override fun onItemSelected(position: Int) {
        slidingRootNav.closeMenu()
        when (position) {
            FONTS -> changeFontStyle()
            FONT_SIZE -> changeTextSize()
            REPORT_PAGE -> reportPage()
            OPEN_IN_BROWSER -> inBrowser()
            SHARE_CHAPTER -> share()
            MORE_SETTINGS -> startReaderSettingsActivity()
            READ_ALOUD -> {
                if (dataCenter.readerMode) {
                    val webPageDBFragment = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as? WebPageDBFragment)
                    val audioText = webPageDBFragment?.doc?.getFormattedText() ?: return
                    val title = webPageDBFragment.doc?.title() ?: ""
                    val chapterIndex = (if (dataCenter.japSwipe) webPages.reversed() else webPages).indexOf(webPages[binding.viewPager.currentItem])

                    startTTSService(audioText, title, novel.id, sourceId, chapterIndex)
                    firebaseAnalytics.logEvent(FAC.Event.LISTEN_NOVEL) {
                        param(FAC.Param.NOVEL_NAME, novel.name)
                        param(FAC.Param.NOVEL_URL, novel.url)
                    }
                } else {
                    showAlertDialog(title = "Read Aloud", message = "Only supported in Reader Mode!")
                }
            }
        }
    }

    /**
     *     For Reader Mode & Night Mode toggle
     */
    override fun bind(item: ReaderMenu, itemView: View, position: Int, simpleItem: SimpleItem) {
        if (itemView.visibility == GONE) {
            itemView.visibility = VISIBLE
            itemView.layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        }
        
        val itemBinding = ItemOptionBinding.bind(itemView)

        itemBinding.title.text = item.title
        itemBinding.icon.setImageDrawable(item.icon)
        itemBinding.itemSwitch.setOnCheckedChangeListener(null)

        if (simpleItem.isSwitchOn() && position == READER_MODE) {
            itemBinding.itemSwitch.visibility = VISIBLE
            itemBinding.itemSwitch.isChecked = dataCenter.readerMode
        } else if (simpleItem.isSwitchOn() && position == NIGHT_MODE) {
            if (!dataCenter.readerMode) {
                itemView.visibility = GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                itemBinding.itemSwitch.visibility = VISIBLE
                itemBinding.itemSwitch.isChecked = dataCenter.isDarkTheme
            }
        } else if (simpleItem.isSwitchOn() && position == JAVA_SCRIPT) {
            if (dataCenter.readerMode) {
                itemView.visibility = GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                itemBinding.itemSwitch.visibility = VISIBLE
                itemBinding.itemSwitch.isChecked = !dataCenter.javascriptDisabled
            }
        } else
            itemBinding.itemSwitch.visibility = GONE

        itemBinding.itemSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            when (position) {
                READER_MODE -> {
                    dataCenter.readerMode = isChecked
                    if (isChecked && !dataCenter.javascriptDisabled)
                        dataCenter.javascriptDisabled = true
                    bindingList.list.adapter?.notifyItemRangeChanged(NIGHT_MODE, 2)
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
                }
                NIGHT_MODE -> {
                    dataCenter.isDarkTheme = isChecked
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
                }
                JAVA_SCRIPT -> {
                    dataCenter.javascriptDisabled = !isChecked
                    if (!dataCenter.javascriptDisabled && dataCenter.readerMode)
                        dataCenter.readerMode = false
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
                }
            }
        }
    }
    
    private fun changeFontStyle() {
        if (AVAILABLE_FONTS.isEmpty())
            getAvailableFonts()

        var selectedFont = dataCenter.fontPath.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')

        var typeFace = createTypeface()
        
        MaterialDialog(this).show {
            title(R.string.title_fonts)

            val exampleText = TextView(this@ReaderDBPagerActivity)
            exampleText.textAlignment = TEXT_ALIGNMENT_CENTER
            exampleText.text = getString(R.string.title_fonts)
            exampleText.textSize = 24F
            exampleText.setTypeface(typeFace, Typeface.NORMAL)
            customView(view = exampleText)

            listItemsSingleChoice(items = AVAILABLE_FONTS.keys.toMutableList(), waitForPositiveButton = false) { dialog, which, font ->
                if (which == 0) {
                    addFont()
                    dialog.dismiss()
                } else {
                    Logs.debug("ReaderDBPagerActivity", "font $which $font")
                    val fontPath = AVAILABLE_FONTS[font.toString()]
                    if (fontPath != null) {
                        selectedFont = font.toString()
                        typeFace = createTypeface(fontPath)
                        // Currently doesn't work
                        //dialog.setTypeface(dialog.titleView, typeFace)
                        getCustomView()?.let { 
                            launchUI {
                                val it = it as TextView
                                it.setTypeface(typeFace, Typeface.NORMAL)
                            }
                        }

                        setActionButtonEnabled(WhichButton.POSITIVE, true)
                    } else {
                        dialog.checkItem(AVAILABLE_FONTS.keys.indexOf(selectedFont))
                    }
                }
            }
            positiveButton(R.string.okay) { _ ->
                dataCenter.fontPath = AVAILABLE_FONTS[selectedFont] ?: ""
                EventBus.getDefault()
                    .post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
            }
            negativeButton(R.string.cancel)
        }
    }

    @Synchronized
    private fun getAvailableFonts() {
        if (AVAILABLE_FONTS.isNotEmpty()) return

        AVAILABLE_FONTS[getString(R.string.add_font)] = ""

        assets.list("fonts")?.filter {
            it.endsWith(".ttf") || it.endsWith(".otf")
        }?.forEach {
            AVAILABLE_FONTS[it.substringBeforeLast('.').replace('_', ' ')] = "/android_asset/fonts/$it"
        }

        val appFontsDir = File(getExternalFilesDir(null) ?: filesDir, "Fonts")
        if (!appFontsDir.exists()) appFontsDir.mkdir()
        appFontsDir.listFiles()?.forEach {
            AVAILABLE_FONTS[it.nameWithoutExtension.replace('_', ' ')] = it.path
        }
    }

    private fun addFont() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, FONT_MIME_TYPES)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
//                        .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, Constants.ADD_FONT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            Constants.IWV_ACT_REQ_CODE -> {
                Handler(Looper.getMainLooper()).post {
                    EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
                }
            }

            Constants.ADD_FONT_REQUEST_CODE -> try {
                // Check for different conditions / edge-cases.
                if (resultCode != Activity.RESULT_OK) return
                val uri = data?.data ?: return
                val document = DocumentFile.fromSingleUri(baseContext, uri) ?: return
                if (!document.isFile) return

                // After the checks are complete, we copy the font for the reader to use
                val fontsDir = File(getExternalFilesDir(null) ?: filesDir, "Fonts/")
                if (!fontsDir.exists()) fontsDir.mkdir()
                val file = File(fontsDir, document.name ?: "RandomFontName${Random().nextInt()}")
                Utils.copyFile(contentResolver, document, file)
                AVAILABLE_FONTS[file.nameWithoutExtension.replace('_', ' ')] = file.path
                dataCenter.fontPath = file.path
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
            } catch (e: Exception) {
                Logs.error(TAG, "Unable to copy font", e)
            }

            Constants.READER_SETTINGS_ACT_REQ_CODE -> {
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE] = Utils.getCurrentFormattedDate()
        db.novelDao().update(novel)
        firebaseAnalytics.logEvent(FAC.Event.OPEN_NOVEL) {
            param(FAC.Param.NOVEL_NAME, novel.name)
            param(FAC.Param.NOVEL_URL, novel.url)
        }
    }


}