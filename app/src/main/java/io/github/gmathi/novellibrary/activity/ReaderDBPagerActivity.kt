package io.github.gmathi.novellibrary.activity


import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.reader.ReaderSettingsActivity
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageFragmentPageListener
import io.github.gmathi.novellibrary.compose.reader.ReaderOverlay
import io.github.gmathi.novellibrary.database.getAllWebPages
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.getWebPageSettingsByRedirectedUrl
import io.github.gmathi.novellibrary.databinding.ActivityReaderPagerBinding
import io.github.gmathi.novellibrary.fragment.WebPageDBFragment
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_STEP
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.analytics.FAC
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.system.getParcelableExtraCompat
import io.github.gmathi.novellibrary.util.system.intentOf
import io.github.gmathi.novellibrary.util.system.logNovelEvent
import io.github.gmathi.novellibrary.util.system.markChapterRead
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.util.system.showAlertDialog
import io.github.gmathi.novellibrary.util.system.startAiTtsActivity
import io.github.gmathi.novellibrary.util.system.startAiTtsService
import io.github.gmathi.novellibrary.util.system.startTTSActivity
import io.github.gmathi.novellibrary.util.system.startTTSService
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.system.updateNovelLastRead
import io.github.gmathi.novellibrary.viewmodel.ReaderViewModel
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Random


class ReaderDBPagerActivity :
    BaseActivity(),
    ViewPager.OnPageChangeListener {

    override val skipWindowInsets: Boolean = true

    companion object {
        private const val TAG = "ReaderDBPagerActivity"

        private val FONT_MIME_TYPES = arrayOf(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("ttf") ?: "application/x-font-ttf",
            "fonts/ttf",
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("otf") ?: "application/x-font-opentype",
            "fonts/otf",
            "application/octet-stream"
        )

        private val AVAILABLE_FONTS = linkedMapOf<String, String>()
    }

    private lateinit var novel: Novel
    private lateinit var adapter: GenericFragmentStatePagerAdapter
    private lateinit var readerViewModel: ReaderViewModel

    private var translatorSourceName: String? = null
    private var webPages: List<WebPage> = ArrayList()

    /** Compose-observable overlay visibility state */
    private val overlayVisible = mutableStateOf(false)

    /** Compose-observable menu icon visibility state (auto-hides on scroll down, shows on scroll up / tap) */
    private val menuIconVisible = mutableStateOf(true)

    /** In page mode the floating menu icon is redundant (center tap opens the menu) and covers text. */
    private val pageModeActive = mutableStateOf(false)

    /** Page mode: chapters already marked read in this session, so each is written once. */
    private val finishedChapters = HashSet<String>()

    /**
     * Whether the chapter pager runs in reverse (next chapter at a lower index). That is what
     * "swipe right for next chapter" means in scroll mode. Page mode locks chapter swiping and
     * turns pages by sliding content left, so its chapter transitions must slide the same way:
     * the pager keeps natural order there.
     */
    private val pagerReversed: Boolean
        get() = dataCenter.japSwipe && !isPageModeActive

    /**
     * Page Mode paginates the cleaned chapter, so it only applies while this novel is in Reader
     * Mode. The preference itself is app-wide; whether it applies depends on the novel.
     */
    private val isPageModeActive: Boolean
        get() = dataCenter.isPageModeActiveForNovel(novel.id)

    lateinit var binding: ActivityReaderPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReaderPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readerViewModel = ViewModelProvider(this)[ReaderViewModel::class.java]

        if (dataCenter.keepScreenOn)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Read Intent Extras
        translatorSourceName = intent.getStringExtra("translatorSourceName")
        if (translatorSourceName == Constants.ALL_TRANSLATOR_SOURCES) translatorSourceName = null

        val tempNovel = intent.getParcelableExtraCompat<Novel>("novel")

        if (tempNovel == null || tempNovel.chaptersCount.toInt() == 0) {
            finish()
            return
        } else
            novel = tempNovel

        readerViewModel.initialize(novel.id)

        // Get all WebPages & set view pager
        webPages = dbHelper.getAllWebPages(novel.id, translatorSourceName)
        if (pagerReversed)
            webPages = webPages.reversed()

        adapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, webPages.size, WebPageFragmentPageListener(novel, webPages))
        binding.viewPager.addOnPageChangeListener(this)
        binding.viewPager.adapter = adapter

        // Set the current page to the bookmarked webPage
        novel.currentChapterUrl?.let { bookmarkUrl ->
            val index = webPages.indexOfFirst { it.url == bookmarkUrl }
            if (index != -1) binding.viewPager.currentItem = index
            if (index == 0) updateBookmark(webPages[0])
        }

        // Update chapter info in ViewModel
        val initialPosition = binding.viewPager.currentItem
        val initialDisplayIndex = if (pagerReversed) (webPages.size - 1 - initialPosition) else initialPosition
        readerViewModel.updateChapterInfo(
            index = initialDisplayIndex,
            total = webPages.size,
            title = webPages.getOrNull(initialPosition)?.chapterName ?: ""
        )

        // Setup Compose overlay (once — state changes drive recomposition)
        setupComposeOverlay()

        // Chapter swiping follows the user's setting and is always off in page mode, where
        // horizontal gestures turn pages inside the chapter.
        var appliedReversed = pagerReversed
        lifecycleScope.launch {
            readerViewModel.uiState.collect { state ->
                binding.viewPager.isSwipeEnabled = state.chapterSwipeEnabled && !state.isPageMode
                pageModeActive.value = state.isPageMode
                val reversed = state.japSwipe && !state.isPageMode
                if (reversed != appliedReversed) {
                    appliedReversed = reversed
                    reversePagerOrder()
                }
            }
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFrag = binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as? WebPageDBFragment
                when {
                    currentFrag == null -> finish()
                    currentFrag.history.isNotEmpty() -> currentFrag.goBack()
                    else -> finish()
                }
            }
        })
    }

    private fun setupComposeOverlay() {
        binding.readerOverlayCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val isVisible = overlayVisible.value

                ReaderOverlay(
                    viewModel = readerViewModel,
                    isVisible = isVisible,
                    isMenuIconVisible = menuIconVisible.value && !pageModeActive.value,
                    novelName = novel.name ?: "",
                    onBackPress = { finish() },
                    onPreviousChapter = {
                        val state = readerViewModel.uiState.value
                        when {
                            // Page mode: first return to page 1 of this chapter; from page 1 go to
                            // page 1 of the previous chapter.
                            state.isPageMode && state.currentPage > 0 -> jumpWithinChapter(toEnd = false)
                            !goToPreviousChapter() -> jumpWithinChapter(toEnd = false)
                        }
                    },
                    onNextChapter = { if (!goToNextChapter()) jumpWithinChapter(toEnd = true) },
                    onPageSelected = { page -> showPage(page) },
                    onFontClick = { changeFontStyle() },
                    onReadAloudClick = { handleReadAloud() },
                    onBrowserClick = { inBrowser() },
                    onMoreSettingsClick = {
                        readerSettingsActivityContract.launch(intentOf<ReaderSettingsActivity>())
                    },
                    onCenterTap = { toggleOverlay() }
                )
            }
        }
    }

    /** Toggle the top/bottom bar visibility. Called from the Compose tap target
     *  and from the WebPageDBFragment scroll listener. */
    fun toggleOverlay() {
        overlayVisible.value = !overlayVisible.value
        // Hide the icon while the full bars are open; reveal it again when they close (screen tapped).
        menuIconVisible.value = !overlayVisible.value
    }

    fun showOverlay() {
        overlayVisible.value = true
        menuIconVisible.value = false // Hide icon when overlay is shown
    }

    fun hideOverlay() {
        overlayVisible.value = false
        menuIconVisible.value = true // Reveal icon when bars close
    }

    /** Show the floating menu icon (called from scroll listener on scroll down). */
    fun showMenuIcon() {
        if (!overlayVisible.value) {
            menuIconVisible.value = true
        }
    }

    /** Hide the floating menu icon (called from scroll listener on scroll up). */
    fun hideMenuIcon() {
        menuIconVisible.value = false
    }

    /**
     * Moves to the next chapter in reading order. The pager list is reversed when
     * "swipe right for next chapter" is on, so "next" is a lower pager index in that case.
     * Returns false when already at the last chapter.
     */
    fun goToNextChapter(): Boolean {
        val target = binding.viewPager.currentItem + if (pagerReversed) -1 else 1
        if (target !in webPages.indices) return false
        // Entering a chapter from the previous one always starts at its first page.
        fragmentAt(target)?.startAtPage(0)
        binding.viewPager.currentItem = target
        return true
    }

    private fun fragmentAt(position: Int): WebPageDBFragment? =
        binding.viewPager.adapter?.instantiateItem(binding.viewPager, position) as? WebPageDBFragment

    private fun currentFragment(): WebPageDBFragment? = fragmentAt(binding.viewPager.currentItem)

    private fun currentWebView(): WebView? = currentFragment()?.view?.findViewById(R.id.readerWebView)

    /** Whether [fragment] shows the chapter the pager is on, rather than an off-screen neighbour. */
    fun isCurrentChapter(fragment: WebPageDBFragment): Boolean {
        val url = fragment.chapterUrl ?: return false
        return webPages.getOrNull(binding.viewPager.currentItem)?.url == url
    }

    /** Called by chapter fragments when their page-mode position changes; only the visible one is shown. */
    fun onFragmentPageChanged(fragment: WebPageDBFragment, page: Int, total: Int) {
        runOnUiThread {
            if (isCurrentChapter(fragment)) readerViewModel.updatePageInfo(page, total)
        }
    }

    /**
     * Page mode: the reader turned past the last page of [fragment]'s chapter ([forward]) or back
     * past its first page. Only the chapter on screen may move the reader: a neighbour still sliding
     * out of view, or a second quick swipe landing on it, would otherwise skip a chapter.
     */
    fun onChapterBoundary(fragment: WebPageDBFragment, forward: Boolean) {
        if (!isCurrentChapter(fragment)) return
        if (forward) {
            markChapterFinished(fragment)
            goToNextChapter()
        } else {
            goToPreviousChapter(startAtEnd = true)
        }
    }

    /** Page mode: the reader turned onto the last page of [fragment]'s chapter. */
    fun onChapterFinished(fragment: WebPageDBFragment) {
        if (isCurrentChapter(fragment)) markChapterFinished(fragment)
    }

    /**
     * In page mode a chapter counts as read once its last page is reached, not when it is opened;
     * opening one only moves the bookmark (see [updateBookmark]).
     */
    private fun markChapterFinished(fragment: WebPageDBFragment) {
        val url = fragment.chapterUrl ?: return
        if (!finishedChapters.add(url)) return
        webPages.firstOrNull { it.url == url }?.let { markChapterRead(it, true) }
    }

    /** Page-mode scrubber: position the current chapter on [page] without the turn animation. */
    private fun showPage(page: Int) {
        currentWebView()?.evaluateJavascript("window.__nlPager && window.__nlPager.goTo($page, true);", null)
    }

    /**
     * Used by the chapter chevrons when there is no further chapter: jump to the end (last page,
     * or bottom in scroll mode) or to the start of the current chapter instead of doing nothing.
     */
    private fun jumpWithinChapter(toEnd: Boolean) {
        val webView = currentWebView() ?: return
        if (isPageModeActive) {
            webView.evaluateJavascript("window.__nlPager && window.__nlPager.goTo(${if (toEnd) -1 else 0});", null)
        } else {
            val target = if (toEnd) (webView.contentHeight * webView.scale - webView.height).toInt().coerceAtLeast(0) else 0
            ObjectAnimator.ofInt(webView, "scrollY", webView.scrollY, target).setDuration(300).start()
        }
    }

    /**
     * Re-orders the pager when the swipe-direction setting changes while the reader is open,
     * keeping the current chapter in place.
     */
    private fun reversePagerOrder() {
        if (webPages.isEmpty()) return
        val current = webPages[binding.viewPager.currentItem]
        webPages = webPages.reversed()
        adapter = GenericFragmentStatePagerAdapter(supportFragmentManager, null, webPages.size, WebPageFragmentPageListener(novel, webPages))
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(webPages.indexOf(current), false)
    }

    /** Moves to the previous chapter in reading order; see [goToNextChapter]. */
    /**
     * @param startAtEnd open the previous chapter at its last page (backing across a chapter
     * boundary in page mode) instead of its first page (chevron).
     */
    fun goToPreviousChapter(startAtEnd: Boolean = false): Boolean {
        val target = binding.viewPager.currentItem + if (pagerReversed) 1 else -1
        if (target !in webPages.indices) return false
        fragmentAt(target)?.startAtPage(if (startAtEnd) -1 else 0)
        binding.viewPager.currentItem = target
        return true
    }

    private fun updateBookmark(webPage: WebPage) {
        // Scroll mode keeps the long-standing behaviour of marking a chapter read when it is
        // opened. Page mode marks it read when its last page is reached (markChapterFinished).
        updateNovelBookmark(novel, webPage, markRead = !isPageModeActive)
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
        // Show the newly visible chapter's page position in the menu.
        fragmentAt(position)?.publishPageInfo()
        val displayIndex = if (pagerReversed) (webPages.size - 1 - position) else position
        readerViewModel.updateChapterInfo(
            index = displayIndex,
            total = webPages.size,
            title = webPages.getOrNull(position)?.chapterName ?: ""
        )
    }

    override fun onPageScrollStateChanged(position: Int) {
        // Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        // Do Nothing
    }

    private fun handleReadAloud() {
        if (dataCenter.getReaderModeForNovel(novel.id)) {
            val webPageDBFragment = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as? WebPageDBFragment)
            val audioText = webPageDBFragment?.doc?.getFormattedText() ?: return
            val title = webPageDBFragment.doc?.title() ?: ""
            val chapterIndex = (if (pagerReversed) webPages.reversed() else webPages).indexOf(webPages[binding.viewPager.currentItem])

            if (dataCenter.useAiTts) {
                val linkedPageUrls = ArrayList(webPageDBFragment.linkedPages.map { it.href })
                startAiTtsService(audioText, linkedPageUrls, title, novel.id, translatorSourceName ?: "", chapterIndex)
                startAiTtsActivity(novel.name ?: "", title)
            } else {
                startTTSService(audioText, webPageDBFragment.linkedPages, title, novel.id, translatorSourceName, chapterIndex)
                firebaseAnalytics.logNovelEvent(FAC.Event.LISTEN_NOVEL, novel)
                startTTSActivity()
            }
        } else {
            showAlertDialog(title = "Read Aloud", message = "Only supported in Reader Mode!")
        }
    }

    private fun inBrowser() {
        val url = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as WebPageDBFragment?)?.getUrl()
        if (url != null)
            openInBrowser(url)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        if (webPages.isEmpty()) return super.dispatchKeyEvent(event)
        val webView = (binding.viewPager.adapter?.instantiateItem(
            binding.viewPager,
            binding.viewPager.currentItem
        ) as WebPageDBFragment?)?.view?.findViewById<WebView>(R.id.readerWebView)
        if (isPageModeActive && dataCenter.enableVolumeScroll &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            // In page mode the volume keys turn pages instead of scrolling.
            if (action == KeyEvent.ACTION_DOWN) {
                val call = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "prev" else "next"
                webView?.evaluateJavascript("window.__nlPager && window.__nlPager.$call();", null)
            }
            return true
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.enableVolumeScroll) {
                    val anim = ObjectAnimator.ofInt(
                        webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) - dataCenter.volumeScrollLength * VOLUME_SCROLL_LENGTH_STEP
                    )
                    anim.setDuration(500).start()
                }
                dataCenter.enableVolumeScroll
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN && dataCenter.enableVolumeScroll) {
                    val anim = ObjectAnimator.ofInt(
                        webView, "scrollY", webView?.scrollY
                            ?: 0, (webView?.scrollY ?: 0) + dataCenter.volumeScrollLength * VOLUME_SCROLL_LENGTH_STEP
                    )
                    anim.setDuration(500).start()
                }
                dataCenter.enableVolumeScroll
            }

            else -> super.dispatchKeyEvent(event)
        }
    }

    fun checkUrl(url: String): Boolean {
        val webPageSettings = dbHelper.getWebPageSettingsByRedirectedUrl(url) ?: return false
        val webPage = dbHelper.getWebPage(webPageSettings.url) ?: return false
        val index = dbHelper.getAllWebPages(novel.id, translatorSourceName).indexOf(webPage)
        return if (index == -1)
            false
        else {
            // A link points at the start of a chapter, not at wherever it was last left.
            if (isPageModeActive) fragmentAt(index)?.startAtPage(0)
            binding.viewPager.currentItem = index
            updateBookmark(webPage)
            true
        }
    }

    private fun createTypeface(path: String = dataCenter.fontPath): Typeface {
        return if (path.startsWith("/android_asset"))
            Typeface.createFromAsset(assets, path.substringAfter('/').substringAfter('/'))
        else
            Typeface.createFromFile(path)
    }

    @SuppressLint("CheckResult")
    private fun changeFontStyle() {
        if (AVAILABLE_FONTS.isEmpty())
            getAvailableFonts()

        var selectedFont = dataCenter.getFontPathForNovel(novel.id).substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')

        var typeFace = createTypeface(dataCenter.getFontPathForNovel(novel.id))

        MaterialDialog(this).show {
            title(R.string.title_fonts)

            val exampleText = TextView(this@ReaderDBPagerActivity)
            exampleText.textAlignment = View.TEXT_ALIGNMENT_CENTER
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

                        launchUI {
                            val it = getCustomView() as TextView
                            it.setTypeface(typeFace, Typeface.NORMAL)
                        }

                        setActionButtonEnabled(WhichButton.POSITIVE, true)
                    } else {
                        dialog.checkItem(AVAILABLE_FONTS.keys.indexOf(selectedFont))
                    }
                }
            }
            positiveButton(R.string.okay) { _ ->
                readerViewModel.onFontChanged(AVAILABLE_FONTS[selectedFont] ?: "")
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
        addFontActivityContract.launch(intent)
    }

    private val addFontActivityContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        try {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val uri = it.data?.data ?: return@registerForActivityResult
            val document = DocumentFile.fromSingleUri(baseContext, uri) ?: return@registerForActivityResult
            if (!document.isFile) return@registerForActivityResult

            val fontsDir = File(getExternalFilesDir(null) ?: filesDir, "Fonts/")
            if (!fontsDir.exists()) fontsDir.mkdir()
            val file = File(fontsDir, document.name ?: "RandomFontName${Random().nextInt()}")
            Utils.copyFile(contentResolver, document, file)
            AVAILABLE_FONTS[file.nameWithoutExtension.replace('_', ' ')] = file.path
            readerViewModel.onFontChanged(file.path)
        } catch (e: Exception) {
            Logs.error(TAG, "Unable to copy font", e)
        }
    }

    private val readerSettingsActivityContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Handler(Looper.getMainLooper()).post {
            readerViewModel.initialize(novel.id) // Refresh state from DataCenter
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
        }
    }

    override fun onResume() {
        super.onResume()
        updateNovelLastRead(novel)
    }
}
