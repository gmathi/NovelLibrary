package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.webkit.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.cleaner.HtmlCleaner
import io.github.gmathi.novellibrary.database.getWebPageSettings
import io.github.gmathi.novellibrary.database.updateWebPageSettings
import io.github.gmathi.novellibrary.databinding.FragmentReaderBinding
import io.github.gmathi.novellibrary.extensions.dataFetchError
import io.github.gmathi.novellibrary.extensions.noInternetError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.lang.getLinkedPagesCompat
import io.github.gmathi.novellibrary.util.view.ReaderPagerScript
import io.github.gmathi.novellibrary.util.view.extensions.setDefaultSettings
import kotlinx.coroutines.*
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup
import org.jsoup.nodes.DataNode
import org.jsoup.nodes.Document
import java.io.File


class WebPageDBFragment : BaseFragment() {

    private lateinit var webPage: WebPage
    private lateinit var webPageSettings: WebPageSettings
    private var novelId: Long = -1L

    var doc: Document? = null
    var linkedPages: ArrayList<LinkedPage> = ArrayList()
    var history: ArrayList<WebPageSettings> = ArrayList()
    var job: Job? = null

    /**
     * Page mode position: the page the pager last reported, or the remembered one until it
     * reports. Persisted on pause and used to reopen the chapter after a reload. Written from the
     * WebView's JavaBridge thread.
     */
    @Volatile
    private var currentPageIndex = 0

    /** Page count reported by the pager; 0 until the chapter has been laid out. */
    @Volatile
    private var totalPageCount = 0

    // Page mode pager bookkeeping (see the "Page mode" region). Each chapter document carries a
    // generation number; calls from a document that has since been replaced are ignored.
    private val pagerLock = Any()

    @Volatile
    private var pagerGeneration = 0

    /** Generation whose pager has started, or -1 while the current document has none yet. Guarded by [pagerLock]. */
    private var pagerReadyGeneration = -1

    /**
     * Page to open at instead of the remembered one, set when the reader navigates here from
     * another chapter (0 = first page, -1 = last page) before this chapter's pager has started.
     * Guarded by [pagerLock].
     */
    private var pendingStartPage: Int? = null

    /** Room in CSS px the pages leave for the display cutout / status bar and the navigation bar. */
    @Volatile
    private var safeInsetTopCss = 0

    @Volatile
    private var safeInsetBottomCss = 0

    /** Page mode: the current touch gesture started while this chapter was not the one on screen. */
    private var ignoreGesture = false

    /** The WebView's renderer died while the activity was stopped; rebuild the view on resume. */
    private var rebuildOnResume = false

    private lateinit var binding: FragmentReaderBinding

    companion object {
        private const val NOVEL_ID = "novelId"
        private const val WEB_PAGE = "webPage"
        private const val RESET_PAGE_ELEMENT_ID = "nl-reset-page"

        fun newInstance(novelId: Long, webPage: WebPage): WebPageDBFragment {
            val fragment = WebPageDBFragment()
            val args = Bundle()
            args.putLong(NOVEL_ID, novelId)
            args.putSerializable(WEB_PAGE, webPage)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_reader, container, false) ?: return null
        binding = FragmentReaderBinding.bind(view)
        return view
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null)
            EventBus.getDefault().register(this)

        //Verify activity is still loaded in
        val activity = activity as? ReaderDBPagerActivity ?: return

        // Get data from args or savedInstance in case of device rotation
        @Suppress("UNCHECKED_CAST")
        if (savedInstanceState != null && savedInstanceState.containsKey("webPage")) {
            webPage = savedInstanceState.getSerializable("webPage") as WebPage
            webPageSettings = savedInstanceState.getSerializable("webPageSettings") as WebPageSettings
            history = savedInstanceState.getSerializable("history") as ArrayList<WebPageSettings>
            novelId = savedInstanceState.getLong("novelId")
        } else {

            val argWebPage = requireArguments().getSerializable(WEB_PAGE) as? WebPage
            val argWebPageSettings: WebPageSettings? = argWebPage?.let { dbHelper.getWebPageSettings(argWebPage.url) }
            if (argWebPage == null || argWebPageSettings == null) {
                activity.finish()
                return
            }
            webPage = argWebPage
            webPageSettings = argWebPageSettings
            novelId = requireArguments().getLong(NOVEL_ID)
        }
        restorePagePosition()

        // Page mode: keep the pages clear of the system bars as the layout settles or changes.
        view?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> refreshSafeInsets() }

        setOnScrollVisibleButtons()
        setWebView()

        // Load data from webPage into webView
        loadData()
        binding.swipeRefreshLayout.setOnRefreshListener { loadData(true) }

    }

    @Suppress("DEPRECATION")
    private fun setOnScrollVisibleButtons() {
        // Show/hide overlay on scroll
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        binding.readerWebView.setOnScrollChangeListener listener@{ _, _, scrollY, _, oldScrollY ->
            val activity: FragmentActivity
            try {
                activity = requireActivity() //Check if activity is available.
            } catch (e: Exception) {
                return@listener
            }

            if (activity is ReaderDBPagerActivity) {
                // Auto-hide while scrolling down, reveal while scrolling up.
                when {
                    scrollY > oldScrollY && scrollY - oldScrollY > Constants.SCROLL_LENGTH -> activity.hideMenuIcon()
                    oldScrollY > scrollY -> activity.showMenuIcon()
                }
            }
            if (dataCenter.enableImmersiveMode && dataCenter.showNavbarAtChapterEnd) {
                // Using deprecated WebView.scale due to WebViewClient.onScaleChanged being completely unreliable.
                // New approach sometimes simply does not trigger, causing anything but online reader mode to break.

                val height = binding.readerWebView.contentHeight * binding.readerWebView.scale - binding.readerWebView.height - 10
                activity.window.decorView.systemUiVisibility =
                    if (height > 0 && scrollY > height) Constants.IMMERSIVE_MODE_W_NAVBAR_FLAGS
                    else Constants.IMMERSIVE_MODE_FLAGS
            }
        }
    }

//    private fun checkForCloudFlare() {
//        if (activity != null)
//            CloudFlareByPasser.check(requireActivity(), "novelupdates.com") { state ->
//                if (activity != null) {
//                    if (state == CloudFlareByPasser.State.CREATED || state == CloudFlareByPasser.State.UNNEEDED) {
//                        Toast.makeText(activity, "Cloud Flare Bypassed", Toast.LENGTH_SHORT).show()
//                        binding.readerWebView.loadUrl("about:blank")
//                        binding.readerWebView.clearHistory()
//                        loadData()
//                    }
//                }
//            }
//    }

    @SuppressLint("JavascriptInterface", "AddJavascriptInterface")
    private fun setWebView() {
        binding.readerWebView.setDefaultSettings()
        binding.readerWebView.isVerticalScrollBarEnabled = dataCenter.showReaderScroll
        binding.readerWebView.settings.javaScriptEnabled = isJavascriptRequired()
        binding.readerWebView.settings.userAgentString = HostNames.USER_AGENT
        binding.readerWebView.setOnTouchListener { view, event ->
            if (!dataCenter.pageMode) return@setOnTouchListener false
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                // Only the chapter on screen takes input. While the pager slides from one chapter
                // to the next the outgoing one is still under the finger, and a swipe landing on it
                // would turn its pages or hand off to yet another chapter.
                ignoreGesture = (activity as? ReaderDBPagerActivity)?.isCurrentChapter(this) == false
                // Page mode turns pages with horizontal gestures inside the WebView, so the
                // chapter ViewPager must not intercept them.
                if (!ignoreGesture) view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            ignoreGesture
        }
        binding.readerWebView.setBackgroundColor(Color.argb(1, 0, 0, 0))
        binding.readerWebView.addJavascriptInterface(this, "HTMLOUT")

        binding.readerWebView.webViewClient = object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                //First page
                if (url == doc?.location()) {
                    return true
                }

                if (url == "abc://reset_page") {
                    if (dataCenter.pageMode) view?.evaluateJavascript("window.__nlPager && window.__nlPager.goTo(0);", null)
                    else view?.scrollTo(0, 0)
                    return true
                }

//                if (url == "abc://retry_internal")
//                    checkForCloudFlare()

                //Add current page to history, if it was not already added or if the history is empty
//                if (history.isEmpty()) history.add(webPage!!)
//                else if (history.last() != webPage) history.add(webPage!!)

                //Handle the known links like next and previous chapter if downloaded
                if (checkUrl(url)) return true

                if (dataCenter.getReaderModeForNovel(novelId))
                    url?.let {

                        //If url is an image
                        if (url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) || url.endsWith(".png") || url.startsWith("file"))
                            return false //default loading

                        downloadWebPage(url)
                        return true

                    }

                //If everything else fails, default loading of the WebView
                return false
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                // The WebView's renderer died (crash or low memory); if unhandled, Android kills
                // the whole app. The WebView is unusable now, so rebuild this fragment's view,
                // which creates a fresh WebView and reloads the chapter.
                Logs.error("WebPageDBFragment", "WebView renderer gone (crashed=${detail?.didCrash()}, priority=${detail?.rendererPriorityAtExit()}); rebuilding chapter view")
                // Posted so the fragment transactions never run inside another callback.
                Handler(Looper.getMainLooper()).post { rebuildView() }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                // The pager destroys off-screen chapters during fast navigation; a load can finish
                // after that, when this fragment is detached and its resources are unreachable.
                if (!isAdded || getView() == null) return
                val cookies = CookieManager.getInstance().getCookie(url)
                Logs.debug("WebViewDBFragment", "${Uri.parse(url).host}: All the cookiesMap in a string: $cookies")

                if (!dataCenter.getReaderModeForNovel(novelId) && url != null && url != "about:blank" && cookies?.contains("cf_clearance") == true) {
                    url.toHttpUrlOrNull()?.let { hurl ->
                        val list = cookies.split(";").mapNotNull { if (it.startsWith("cf_")) Cookie.parse(hurl, it) else null }
                        networkHelper.cookieManager.saveFromResponse(hurl, list)
                    }
                }

                // Page mode: the pager is part of the chapter document and starts on its own.
                if (dataCenter.pageMode) return

                webPageSettings.let {
                    if (it.metadata.containsKey(Constants.MetaDataKeys.SCROLL_POSITION)) {
                        view?.scrollTo(
                            0, (it.metadata[Constants.MetaDataKeys.SCROLL_POSITION]
                                ?: "0").toInt()
                        )
                    }
                }

            }
        }
        //readerWebView.setOnScrollChangeListener { webView, i, i, i, i ->  }
        changeTextSize()
    }

    private fun loadData(liveFromWeb: Boolean = false) {
        doc = null
        // Whatever the outgoing document still reports is stale from here on.
        invalidatePager()
        // Pull-to-refresh intercepts any gesture with downward drift, which breaks page-mode
        // swipes; keep it off in page mode on every load path (file and web).
        binding.swipeRefreshLayout.isEnabled = !dataCenter.pageMode

        binding.readerWebView.apply {
            stopLoading()
            loadUrl("about:blank")
        }

        if (webPageSettings.filePath != null && !liveFromWeb) {
            loadFromFile()
        } else {
            loadFromWeb()
        }
    }

    private fun loadFromFile() {

        val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
        val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
        if (!input.exists()) {
            loadFromWeb()
            return
        }

        binding.swipeRefreshLayout.apply {
            isRefreshing = false
        }

        val url = webPageSettings.redirectedUrl ?: internalFilePath

        doc = Jsoup.parse(input, "UTF-8", url)
        doc?.let { doc ->
            if (dataCenter.getReaderModeForNovel(novelId)) {
                cleanDocument(doc)
            } else {
                // cleanDocument() already applies the theme/font internally when reader mode is
                // on. When reader mode is off, apply it here so a freshly opened chapter reflects
                // the user's chosen font/colors immediately, instead of only after the user next
                // changes a display setting (which is what triggers applyTheme() reactively).
                HtmlCleaner.getInstance(doc).toggleTheme(
                    dataCenter.getIsDarkThemeForNovel(novelId),
                    doc,
                    fontPath = dataCenter.getFontPathForNovel(novelId),
                    dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                    dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                    nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                    nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
                    limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
                )
            }
            loadCreatedDocument()
        }
    }

    private fun loadFromWeb() {
        // Pull-to-refresh conflicts with page-mode gestures.
        binding.swipeRefreshLayout.isEnabled = !dataCenter.pageMode

        //Check Reader Mode
        if (!dataCenter.getReaderModeForNovel(novelId)) {
            binding.swipeRefreshLayout.isRefreshing = false
            binding.readerWebView.loadUrl(webPage.url)

        } else {
            //Download the page and clean it to make it readable!
            if (job != null && job!!.isActive) job!!.cancel()
            downloadWebPage(webPage.url)
        }
    }

    private fun loadCreatedDocument() {
        val doc = doc ?: return
        // The document is rebuilt on every theme change; replace what earlier builds appended.
        doc.getElementById(RESET_PAGE_ELEMENT_ID)?.remove()
        doc.body().append("<p id=\"$RESET_PAGE_ELEMENT_ID\"><a tts-disable=\"true\" href=\"abc://reset_page\">*** Go to top of page ***</a></p>")
        doc.getElementById(ReaderPagerScript.ELEMENT_ID)?.remove()
        if (dataCenter.pageMode) {
            // The pager travels inside the document, so it starts as soon as the chapter is parsed
            // whichever way it was loaded (see ReaderPagerScript).
            updateSafeInsets()
            doc.body().appendElement("script")
                .attr("id", ReaderPagerScript.ELEMENT_ID)
                .attr("tts-disable", "true")
                .appendChild(DataNode(ReaderPagerScript.build(invalidatePager())))
        }
        webPageSettings.let {
            binding.readerWebView.loadDataWithBaseURL(
                if (it.filePath != null) "$FILE_PROTOCOL${it.filePath}" else doc.location(),
                doc.outerHtml(),
                "text/html", "UTF-8", null
            )
            if (!dataCenter.pageMode && it.metadata.containsKey(Constants.MetaDataKeys.SCROLL_POSITION)) {
                binding.readerWebView.scrollTo(
                    0, (it.metadata[Constants.MetaDataKeys.SCROLL_POSITION]
                            )!!.toInt()
                )
            }
        }
    }


    private fun downloadWebPage(url: String?) {
        if (url == null) return

        binding.progressLayout.showLoading()

        //If no network
        if (!networkHelper.isConnectedToNetwork()) {
            binding.progressLayout.noInternetError {
                downloadWebPage(url)
            }
            return
        }

        job = lifecycleScope.launch download@{
            try {

                doc = withContext(Dispatchers.IO) { WebPageDocumentFetcher.document(url) }

                if (doc != null) {

                    if (doc!!.location().contains("rssbook") && doc!!.location().contains(HostNames.QIDIAN)) {
                        doc = withContext(Dispatchers.IO) { WebPageDocumentFetcher.document(doc!!.location().replace("rssbook", "book")) }
                    }
//                    if (doc!!.location().contains("/nu/") && doc!!.location().contains(HostNames.FLYING_LINES)) {
//                        doc = withContext(Dispatchers.IO) { NovelApi.getDocumentWithUserAgent(doc!!.location().replace("/nu/", "/chapter/")) }
//                    }
                }

                //If document fails to load and the fragment is still alive
                if (doc == null) {
                    if (isResumed && !isRemoving && !isDetached)
                        binding.progressLayout.dataFetchError {
                            downloadWebPage(url)
                        }
                    return@download
                }


                //Update the relative urls with the absolute urls for the images and links
                doc?.getElementsByTag("img")?.forEach {
                    if (it.hasAttr("src")) {
                        it.attr("src", it.absUrl("src"))
                    }
                }
                doc?.getElementsByTag("a")?.forEach {
                    if (it.hasAttr("href")) {
                        it.attr("href", it.absUrl("href"))
                    }
                }

                // Process the document and load it onto the webView
                doc?.let { doc ->
                    val htmlHelper = HtmlCleaner.getInstance(doc)
                    htmlHelper.removeJS(doc)
                    htmlHelper.additionalProcessing(doc)
                    htmlHelper.setProperHrefUrls(doc)
                    htmlHelper.toggleTheme(
                        dataCenter.getIsDarkThemeForNovel(novelId),
                        doc,
                        fontPath = dataCenter.getFontPathForNovel(novelId),
                        dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                        dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                        nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                        nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
                        limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
                    )

                    if (dataCenter.getEnableClusterPagesForNovel(novelId)) {
                        val alreadyDownloadedLinks = ArrayList<String>()
                        alreadyDownloadedLinks.add(doc.location())
                        htmlHelper.getLinkedChapters(doc).forEach { linkedUrl ->
                            if (alreadyDownloadedLinks.contains(linkedUrl.href)) return@forEach
                            try {
                                val otherDoc = withContext(Dispatchers.IO) { WebPageDocumentFetcher.document(linkedUrl.href) }
                                val helper = HtmlCleaner.getInstance(otherDoc)
                                helper.removeJS(otherDoc)
                                helper.additionalProcessing(otherDoc)
                                helper.setProperHrefUrls(otherDoc)
                                doc.body().append(otherDoc.body().html())
                                alreadyDownloadedLinks.add(otherDoc.location())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        linkedPages = ArrayList()
                    } else {
                        linkedPages = htmlHelper.getLinkedChapters(doc)
                    }
                    if (this.isActive)
                        loadCreatedDocument()
                    else return@download
                }

                binding.progressLayout.showContent()
                binding.swipeRefreshLayout.isRefreshing = false

            } catch (e: Exception) {

                e.printStackTrace()
                if (isResumed && !isRemoving && !isDetached)
                    binding.progressLayout.dataFetchError {
                        downloadWebPage(url)
                    }
            }
        }
    }

    private fun changeTextSize() {
        val settings = binding.readerWebView.settings
        settings.textZoom = (dataCenter.getTextSizeForNovel(novelId) + 50) * 2
        if (dataCenter.pageMode) {
            // The zoom change reflows the columns; re-count the pages once layout settles.
            binding.readerWebView.postDelayed({
                if (view != null) binding.readerWebView.evaluateJavascript("window.__nlPager && window.__nlPager.relayout();", null)
            }, 150)
        }
    }

    fun getUrl() = webPage.url

    /** The chapter this fragment shows, or null before its arguments have been read. */
    val chapterUrl: String?
        get() = if (this::webPage.isInitialized) webPage.url else null

    private fun getUrlDomain(url: String? = getUrl()): String? {
        return url?.let { url.toHttpUrlOrNull()?.topPrivateDomain() }
    }

    fun goBack() {
        webPageSettings = history.last()
        history.remove(webPageSettings)
        restorePagePosition()
        loadData()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun cleanDocument(doc: Document) {
        try {
            binding.progressLayout.showLoading()
            binding.readerWebView.settings.javaScriptEnabled = true
            val htmlHelper = HtmlCleaner.getInstance(doc)
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            htmlHelper.setProperHrefUrls(doc)
            htmlHelper.toggleTheme(
                dataCenter.getIsDarkThemeForNovel(novelId),
                doc,
                fontPath = dataCenter.getFontPathForNovel(novelId),
                dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
                limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
            )

            if (dataCenter.getEnableClusterPagesForNovel(novelId)) {
                // Add the content of the links to the doc
                if (webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                    val links = webPageSettings.getLinkedPagesCompat()
                    links.forEach {
                        val tempWebPageSettings = dbHelper.getWebPageSettings(it.href)!!
                        val internalFilePath = "$FILE_PROTOCOL${tempWebPageSettings.filePath}"
                        val input = File(internalFilePath.substring(7))

                        var url = tempWebPageSettings.redirectedUrl
                        if (url == null) url = internalFilePath
                        val otherDoc = Jsoup.parse(input, "UTF-8", url)
                        if (otherDoc != null) {
                            val helper = HtmlCleaner.getInstance(otherDoc)
                            helper.removeJS(otherDoc)
                            helper.additionalProcessing(otherDoc)
                            doc.body().append(otherDoc.body().html())
                        }
                    }
                }
                linkedPages = ArrayList()
            } else {
                linkedPages = htmlHelper.getLinkedChapters(doc)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            binding.progressLayout.showContent()
        }
    }

    private fun applyTheme() {
        doc?.let {
            HtmlCleaner.getInstance(it).toggleTheme(
                dataCenter.getIsDarkThemeForNovel(novelId),
                it,
                fontPath = dataCenter.getFontPathForNovel(novelId),
                dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
                limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
            )
            loadCreatedDocument()
        }
    }

    fun checkUrl(url: String?): Boolean {
        if (url == null) return false
        if (webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
            val links = webPageSettings.getLinkedPagesCompat()
            links.forEach {
                val tempWebPageSettings = dbHelper.getWebPageSettings(it.href) ?: return@forEach
                if (it.href == url || (tempWebPageSettings.redirectedUrl != null && tempWebPageSettings.redirectedUrl == url)) {
                    history.add(tempWebPageSettings)
                    webPageSettings = tempWebPageSettings
                    restorePagePosition()
                    loadData()
                    return@checkUrl true
                }
            }
        }

        val readerActivity = (activity as ReaderDBPagerActivity?) ?: return false
        return readerActivity.checkUrl(url)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onReaderSettingsChanged(event: ReaderSettingsEvent) {
        // Without a view the WebView is gone; the next view loads with the new settings anyway.
        if (view == null) return
        when (event.setting) {
            ReaderSettingsEvent.NIGHT_MODE -> {
                applyTheme()
            }
            ReaderSettingsEvent.READER_MODE -> {
                binding.readerWebView.loadUrl("about:blank")
                binding.readerWebView.clearHistory()
                binding.readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled || dataCenter.getReaderModeForNovel(novelId)
                loadData()
            }
            ReaderSettingsEvent.PAGE_MODE -> {
                binding.readerWebView.settings.javaScriptEnabled = isJavascriptRequired()
                binding.swipeRefreshLayout.isEnabled = !dataCenter.pageMode
                loadData()
            }
            ReaderSettingsEvent.TEXT_SIZE -> {
                changeTextSize()
            }
            ReaderSettingsEvent.JAVA_SCRIPT -> {
                binding.readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled || dataCenter.getReaderModeForNovel(novelId)
                loadData()
            }
            ReaderSettingsEvent.FONT -> {
                applyTheme()
            }
        }
    }

    //region Page mode

    /** JavaScript is needed for reader mode's injected scripts and for the page-mode pager. */
    private fun isJavascriptRequired(): Boolean =
        !dataCenter.javascriptDisabled || dataCenter.readerMode || dataCenter.pageMode

    /** Reopens at the page remembered in the chapter settings now in use. */
    private fun restorePagePosition() {
        currentPageIndex = webPageSettings.metadata[Constants.MetaDataKeys.PAGE_INDEX]?.toIntOrNull() ?: 0
        totalPageCount = 0
    }

    /** Starts a new document generation and returns it; calls from older documents are ignored. */
    private fun invalidatePager(): Int = synchronized(pagerLock) {
        pagerReadyGeneration = -1
        ++pagerGeneration
    }

    /**
     * The reader runs edge-to-edge in immersive mode, so the pages are padded past the display
     * cutout (front camera) and the navigation bar. Only the part of each inset that overlaps this
     * chapter counts: when the bars are visible and the layout already sits between them, nothing
     * is added. Measured on the fragment's root view, which the chapter pager lays out even while
     * the WebView is hidden behind the loading indicator. Insets are device px, pages use CSS px.
     */
    private fun updateSafeInsets() {
        val root = view ?: return
        if (root.height == 0) return
        val insets = ViewCompat.getRootWindowInsets(root) ?: return
        val density = root.resources.displayMetrics.density
        val insetTop = insets.getInsets(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.statusBars()).top
        val insetBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val location = IntArray(2).also { root.getLocationOnScreen(it) }
        val windowHeight = activity?.window?.decorView?.height ?: (location[1] + root.height)
        safeInsetTopCss = ((insetTop - location[1]).coerceAtLeast(0) / density).toInt()
        safeInsetBottomCss = (((location[1] + root.height) - (windowHeight - insetBottom)).coerceAtLeast(0) / density).toInt()
    }

    /** Re-measures the insets after a layout change and moves a running pager's margins with them. */
    private fun refreshSafeInsets() {
        val top = safeInsetTopCss
        val bottom = safeInsetBottomCss
        updateSafeInsets()
        if (!dataCenter.pageMode || view == null || (top == safeInsetTopCss && bottom == safeInsetBottomCss)) return
        binding.readerWebView.evaluateJavascript("window.__nlPager && window.__nlPager.setInsets($safeInsetTopCss, $safeInsetBottomCss);", null)
    }

    /**
     * Asked by the pager when it starts. Returns "page,insetTop,insetBottom": the page to open at
     * (a page the reader asked for while the chapter was loading, else the remembered one; -1 is
     * the last page) and the room in CSS px to leave for the system bars.
     */
    @JavascriptInterface
    fun pagerInit(generation: Int): String {
        val page = synchronized(pagerLock) {
            if (generation != pagerGeneration) return@synchronized currentPageIndex
            pagerReadyGeneration = generation
            (pendingStartPage ?: currentPageIndex).also { pendingStartPage = null }
        }
        return "$page,$safeInsetTopCss,$safeInsetBottomCss"
    }

    @JavascriptInterface
    fun onPageChanged(generation: Int, page: Int, total: Int) {
        if (generation != pagerGeneration) return
        currentPageIndex = page
        totalPageCount = total
        publishPageInfo()
    }

    /** Reports this chapter's page position to the reader (shown in the menu when current). */
    fun publishPageInfo() {
        (activity as? ReaderDBPagerActivity)?.onFragmentPageChanged(this, currentPageIndex, totalPageCount)
    }

    /**
     * Opens this chapter at [page] (0 = first, -1 = last) instead of the remembered page: right
     * away if its pager is running, otherwise as soon as the pager starts.
     */
    fun startAtPage(page: Int) {
        val running = synchronized(pagerLock) {
            (pagerReadyGeneration == pagerGeneration && view != null).also { if (!it) pendingStartPage = page }
        }
        if (running) binding.readerWebView.evaluateJavascript("window.__nlPager && window.__nlPager.goTo($page, true);", null)
    }

    /** The reader turned onto the last page of this chapter. */
    @JavascriptInterface
    fun onLastPageReached(generation: Int) {
        if (generation != pagerGeneration) return
        val readerActivity = activity as? ReaderDBPagerActivity ?: return
        readerActivity.runOnUiThread { readerActivity.onChapterFinished(this) }
    }

    @JavascriptInterface
    fun onChapterBoundary(generation: Int, direction: String) {
        if (generation != pagerGeneration) return
        val readerActivity = activity as? ReaderDBPagerActivity ?: return
        readerActivity.runOnUiThread { readerActivity.onChapterBoundary(this, forward = direction == "next") }
    }

    @JavascriptInterface
    fun onCenterTap(generation: Int) {
        if (generation != pagerGeneration) return
        val readerActivity = activity as? ReaderDBPagerActivity ?: return
        readerActivity.runOnUiThread { if (readerActivity.isCurrentChapter(this)) readerActivity.toggleOverlay() }
    }

    //endregion

    /**
     * Recreates this chapter's view, and with it a fresh WebView that reloads the chapter. Used
     * when the WebView's renderer has died: the old WebView can no longer draw or run the pager,
     * so it would sit frozen on screen ignoring every tap and swipe.
     */
    private fun rebuildView() {
        if (!isAdded) return
        // Renderers are often reclaimed while the app is in the background, when no transaction
        // can run; the rebuild then waits for the reader to come back.
        if (isStateSaved) {
            rebuildOnResume = true
            return
        }
        // Two transactions: a detach and attach in the same one cancel out and keep the dead view.
        parentFragmentManager.beginTransaction().detach(this).commitNowAllowingStateLoss()
        parentFragmentManager.beginTransaction().attach(this).commitNowAllowingStateLoss()
    }

    override fun onResume() {
        super.onResume()
        if (rebuildOnResume) {
            rebuildOnResume = false
            // Not from inside the lifecycle dispatch that is resuming this fragment.
            Handler(Looper.getMainLooper()).post { rebuildView() }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::webPageSettings.isInitialized)
            webPageSettings.let {
                if (dataCenter.pageMode) {
                    it.metadata[Constants.MetaDataKeys.PAGE_INDEX] = currentPageIndex.toString()
                } else {
                    it.metadata[Constants.MetaDataKeys.SCROLL_POSITION] = binding.readerWebView.scrollY.toString()
                }
                dbHelper.updateWebPageSettings(it)
            }
    }

    override fun onDestroyView() {
        // Release the page in the WebView renderer now rather than whenever the WebView is garbage
        // collected: every chapter paged past would otherwise stay loaded in the shared renderer.
        if (job?.isActive == true) job?.cancel()
        binding.readerWebView.apply {
            stopLoading()
            (parent as? ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        if (job != null && job!!.isActive) job!!.cancel()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("webPage", webPage)
        outState.putSerializable("webPageSettings", webPageSettings)
        outState.putSerializable("history", history)
        outState.putLong("novelId", novelId)
    }
}
