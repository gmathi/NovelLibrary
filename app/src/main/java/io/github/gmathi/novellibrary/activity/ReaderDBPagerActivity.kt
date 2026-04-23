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
import io.github.gmathi.novellibrary.util.system.intentOf
import io.github.gmathi.novellibrary.util.system.logNovelEvent
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.util.system.showAlertDialog
import io.github.gmathi.novellibrary.util.system.startAiTtsActivity
import io.github.gmathi.novellibrary.util.system.startAiTtsService
import io.github.gmathi.novellibrary.util.system.startTTSActivity
import io.github.gmathi.novellibrary.util.system.startTTSService
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.system.updateNovelLastRead
import io.github.gmathi.novellibrary.util.system.getParcelableExtraCompat
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

    /** Compose-observable menu icon visibility state (shown on scroll down, hidden on scroll up) */
    private val menuIconVisible = mutableStateOf(false)

    lateinit var binding: ActivityReaderPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityReaderPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        readerViewModel = ViewModelProvider(this)[ReaderViewModel::class.java]
        readerViewModel.initialize()

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

        // Get all WebPages & set view pager
        webPages = dbHelper.getAllWebPages(novel.id, translatorSourceName)
        if (dataCenter.japSwipe)
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
        val initialDisplayIndex = if (dataCenter.japSwipe) (webPages.size - 1 - initialPosition) else initialPosition
        readerViewModel.updateChapterInfo(
            index = initialDisplayIndex,
            total = webPages.size,
            title = webPages.getOrNull(initialPosition)?.chapterName ?: ""
        )

        // Setup Compose overlay (once — state changes drive recomposition)
        setupComposeOverlay()

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
                    isMenuIconVisible = menuIconVisible.value,
                    novelName = novel.name ?: "",
                    onBackPress = { finish() },
                    onPreviousChapter = {
                        val current = binding.viewPager.currentItem
                        if (current > 0) binding.viewPager.currentItem = current - 1
                    },
                    onNextChapter = {
                        val current = binding.viewPager.currentItem
                        if (current < webPages.size - 1) binding.viewPager.currentItem = current + 1
                    },
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
        if (overlayVisible.value) {
            menuIconVisible.value = false // Hide icon when overlay opens
        }
    }

    fun showOverlay() {
        overlayVisible.value = true
        menuIconVisible.value = false // Hide icon when overlay is shown
    }

    fun hideOverlay() {
        overlayVisible.value = false
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

    private fun updateBookmark(webPage: WebPage) {
        updateNovelBookmark(novel, webPage)
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
        val displayIndex = if (dataCenter.japSwipe) (webPages.size - 1 - position) else position
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
        if (dataCenter.readerMode) {
            val webPageDBFragment = (binding.viewPager.adapter?.instantiateItem(binding.viewPager, binding.viewPager.currentItem) as? WebPageDBFragment)
            val audioText = webPageDBFragment?.doc?.getFormattedText() ?: return
            val title = webPageDBFragment.doc?.title() ?: ""
            val chapterIndex = (if (dataCenter.japSwipe) webPages.reversed() else webPages).indexOf(webPages[binding.viewPager.currentItem])

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

        var selectedFont = dataCenter.fontPath.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')

        var typeFace = createTypeface()

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
                dataCenter.fontPath = AVAILABLE_FONTS[selectedFont] ?: ""
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
            readerViewModel.initialize() // Refresh state from DataCenter
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
        }
    }

    override fun onResume() {
        super.onResume()
        updateNovelLastRead(novel)
    }
}
