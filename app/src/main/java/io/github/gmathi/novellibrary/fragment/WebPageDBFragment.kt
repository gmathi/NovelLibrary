package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import co.metalab.asyncawait.async
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getWebPageSettings
import io.github.gmathi.novellibrary.database.updateWebPageSettings
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.dataFetchError
import io.github.gmathi.novellibrary.extensions.noInternetError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.ReaderSettingsEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.model.WebPageSettings
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_reader_pager.*
import kotlinx.android.synthetic.main.fragment_reader.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URL


class WebPageDBFragment : BaseFragment() {

    private lateinit var webPage: WebPage
    private lateinit var webPageSettings: WebPageSettings

    var doc: Document? = null
    var history: ArrayList<WebPageSettings> = ArrayList()

    companion object {
        private const val NOVEL_ID = "novelId"
        private const val WEB_PAGE = "webPage"

        fun newInstance(novelId: Long, webPage: WebPage): WebPageDBFragment {
            val fragment = WebPageDBFragment()
            val args = Bundle()
            args.putLong(NOVEL_ID, novelId)
            args.putSerializable(WEB_PAGE, webPage)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_reader, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState == null)
            EventBus.getDefault().register(this)

        //Verify activity is still loaded in
        val activity = activity as? ReaderDBPagerActivity ?: return

        setOnScrollVisibleButtons()
        setWebView()

        // Get data from args or savedInstance in case of device rotation
        @Suppress("UNCHECKED_CAST")
        if (savedInstanceState != null && savedInstanceState.containsKey("webPage")) {
            webPage = savedInstanceState.getSerializable("webPage") as WebPage
            webPageSettings = savedInstanceState.getSerializable("webPageSettings") as WebPageSettings
            history = savedInstanceState.getSerializable("history") as ArrayList<WebPageSettings>
        } else {

            val argWebPage = requireArguments().getSerializable(WEB_PAGE) as? WebPage
            val argWebPageSettings: WebPageSettings? = argWebPage?.let { dbHelper.getWebPageSettings(argWebPage.url) }
            if (argWebPage == null || argWebPageSettings == null) {
                activity.finish()
                return
            }
            webPage = argWebPage
            webPageSettings = argWebPageSettings
        }

        // Load data from webPage into webView
        loadData()
        swipeRefreshLayout.setOnRefreshListener { loadData(true) }

    }

    private fun setOnScrollVisibleButtons() {
        // Show the menu button on scroll
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            readerWebView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (dataCenter.isReaderModeButtonVisible) {
                    if (scrollY > oldScrollY && scrollY > 0) requireActivity().menuNav.visibility = View.GONE
                    if (oldScrollY - scrollY > Constants.SCROLL_LENGTH) requireActivity().menuNav.visibility = View.VISIBLE
                }
                if (dataCenter.enableImmersiveMode && dataCenter.showNavbarAtChapterEnd) {
                    // Using deprecated WebView.scale due to WebViewClient.onScaleChanged being completely unreliable.
                    // New approach sometimes simply does not trigger, causing anything but online reader mode to break.
                    @Suppress("DEPRECATION") val height = readerWebView.contentHeight * readerWebView.scale - readerWebView.height - 10
                    requireActivity().window.decorView.systemUiVisibility =
                        if (height > 0 && scrollY > height) Constants.IMMERSIVE_MODE_W_NAVBAR_FLAGS
                        else Constants.IMMERSIVE_MODE_FLAGS
                }
            }
        }
    }

    private fun checkForCloudFlare() {
        if (activity != null)
            CloudFlareByPasser.check(requireActivity(), "novelupdates.com") { state ->
                if (activity != null) {
                    if (state == CloudFlareByPasser.State.CREATED || state == CloudFlareByPasser.State.UNNEEDED) {
                        Toast.makeText(activity, "Cloud Flare Bypassed", Toast.LENGTH_SHORT).show()
                        readerWebView.loadUrl("about:blank")
                        readerWebView.clearHistory()
                        loadData()
                    }
                }
            }
    }

    @SuppressLint("JavascriptInterface", "AddJavascriptInterface")
    private fun setWebView() {
        readerWebView.isVerticalScrollBarEnabled = dataCenter.showReaderScroll
        readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled
        readerWebView.settings.userAgentString = HostNames.USER_AGENT
        readerWebView.setBackgroundColor(Color.argb(1, 0, 0, 0))
        readerWebView.addJavascriptInterface(this, "HTMLOUT")
        readerWebView.webViewClient = object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                //First page
                if (url == doc?.location()) {
                    return true
                }

                if (url == "abc://retry_internal")
                    checkForCloudFlare()

                //Add current page to history, if it was not already added or if the history is empty
//                if (history.isEmpty()) history.add(webPage!!)
//                else if (history.last() != webPage) history.add(webPage!!)

                //Handle the known links like next and previous chapter if downloaded
                if (checkUrl(url)) return true

                if (dataCenter.readerMode)
                    url?.let {

                        //If url is an image
                        if (url.endsWith(".jpg", true) || url.endsWith(".jpeg", true) || url.endsWith(".png"))
                            return false //default loading

                        downloadWebPage(url)
                        return true
                    }

                //If everything else fails, default loading of the WebView
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                Logs.debug("WebViewDBFragment", "${Uri.parse(url).host}: All the cookiesMap in a string: $cookies")

                if (url != "about:blank" && cookies != null && cookies.contains("cfduid") && cookies.contains("cf_clearance")) {
                    CloudFlareByPasser.saveCookies(URL(url))
                }

                webPageSettings.let {
                    if (it.metaData.containsKey(Constants.MetaDataKeys.SCROLL_POSITION)) {
                        view?.scrollTo(
                            0, (it.metaData[Constants.MetaDataKeys.SCROLL_POSITION]
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

        readerWebView.apply {
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

        swipeRefreshLayout.apply {
            isRefreshing = false
        }

        val url = webPageSettings.redirectedUrl ?: internalFilePath

        doc = Jsoup.parse(input, "UTF-8", url)
        doc?.let { doc ->
            if (dataCenter.readerMode) {
                cleanDocument(doc)
            }
            loadCreatedDocument()
        }
    }

    private fun loadFromWeb() {
        swipeRefreshLayout.isEnabled = true

        //Check Reader Mode
        if (!dataCenter.readerMode) {
            swipeRefreshLayout.isRefreshing = false
            readerWebView.loadUrl(webPage.url)

        } else {
            //Download the page and clean it to make it readable!
            async.cancelAll()
            downloadWebPage(webPage.url)
        }
    }

    private fun loadCreatedDocument() {
        doc?.body()?.append("<p><a href=\"#\">*** Go to top of page ***</a></p>")
        webPageSettings.let {
            readerWebView.loadDataWithBaseURL(
                if (it.filePath != null) "$FILE_PROTOCOL${it.filePath}" else doc?.location(),
                doc?.outerHtml(),
                "text/html", "UTF-8", null
            )
        }
    }


    private fun downloadWebPage(url: String?) {
        if (url == null) return

        progressLayout.showLoading()

        //If no network
        if (!Utils.isConnectedToNetwork(activity)) {
            progressLayout.noInternetError(View.OnClickListener {
                downloadWebPage(url)
            })
            return
        }

        async download@{
            try {

                doc = await { NovelApi.getDocument(url) }

                if (doc != null) {

                    if (doc!!.location().contains("rssbook") && doc!!.location().contains(HostNames.QIDIAN)) {
                        doc = await { NovelApi.getDocument(doc!!.location().replace("rssbook", "book")) }
                    }
//                    if (doc!!.location().contains("/nu/") && doc!!.location().contains(HostNames.FLYING_LINES)) {
//                        doc = await { NovelApi.getDocumentWithUserAgent(doc!!.location().replace("/nu/", "/chapter/")) }
//                    }
                }

                //If document fails to load and the fragment is still alive
                if (doc == null) {
                    if (isResumed && !isRemoving && !isDetached)
                        progressLayout.dataFetchError(View.OnClickListener {
                            downloadWebPage(url)
                        })
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
                    val htmlHelper = HtmlHelper.getInstance(doc)
                    htmlHelper.removeJS(doc)
                    htmlHelper.additionalProcessing(doc)
                    htmlHelper.setProperHrefUrls(doc)
                    htmlHelper.toggleTheme(dataCenter.isDarkTheme, doc)

                    if (dataCenter.enableClusterPages) {
                        // Get URL domain name of the chapter provider
                        val baseUrlDomain = getUrlDomain(doc.location())

                        // Add the content of the links to the doc
                        val alreadyDownloadedLinks = ArrayList<String>()
                        alreadyDownloadedLinks.add(doc.location())
                        val hrefElements = doc.body().select("a[href]")
                        hrefElements?.forEach {

                            // Other Share links
                            if (it.hasAttr("title") && it.attr("title").contains("Click to share", true)) {
                                return@forEach
                            }

                            val linkedUrl = it.attr("href").split("#").first()
                            if (alreadyDownloadedLinks.contains(linkedUrl)) return@forEach

                            try {
                                // Check if URL is from chapter provider, only download from same domain
                                val urlDomain = getUrlDomain(linkedUrl)
                                if (urlDomain == baseUrlDomain) {
                                    val otherDoc = await { NovelApi.getDocument(linkedUrl) }
                                    val helper = HtmlHelper.getInstance(otherDoc)
                                    helper.removeJS(otherDoc)
                                    helper.additionalProcessing(otherDoc)
                                    doc.body().append(otherDoc.body().html())
                                    alreadyDownloadedLinks.add(otherDoc.location())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    loadCreatedDocument()
                }

                progressLayout.showContent()
                swipeRefreshLayout.isRefreshing = false

            } catch (e: Exception) {
                e.printStackTrace()
                if (isResumed && !isRemoving && !isDetached)
                    progressLayout.dataFetchError(View.OnClickListener {
                        downloadWebPage(url)
                    })
            }
        }
    }

    private fun changeTextSize() {
        val settings = readerWebView.settings
        settings.textZoom = (dataCenter.textSize + 50) * 2
    }

    fun getUrl() = webPage.url

    private fun getUrlDomain(url: String? = getUrl()): String? {
        return url?.let { url.toHttpUrlOrNull()?.topPrivateDomain() }
    }

    fun goBack() {
        webPageSettings = history.last()
        history.remove(webPageSettings)
        loadData()
    }

    private fun cleanDocument(doc: Document) {
        try {
            progressLayout.showLoading()
            readerWebView.settings.javaScriptEnabled = false
            val htmlHelper = HtmlHelper.getInstance(doc)
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            htmlHelper.setProperHrefUrls(doc)
            htmlHelper.toggleTheme(dataCenter.isDarkTheme, doc)

            if (dataCenter.enableClusterPages) {
                // Add the content of the links to the doc
                if (webPageSettings.metaData.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                    val links: ArrayList<String> =
                        Gson().fromJson(webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES_SETTINGS], object : TypeToken<java.util.ArrayList<String>>() {}.type)
                    links.forEach {
                        val tempWebPageSettings = dbHelper.getWebPageSettings(it)!!
                        val internalFilePath = "$FILE_PROTOCOL${tempWebPageSettings.filePath}"
                        val input = File(internalFilePath.substring(7))

                        var url = tempWebPageSettings.redirectedUrl
                        if (url == null) url = internalFilePath
                        val otherDoc = Jsoup.parse(input, "UTF-8", url)
                        if (otherDoc != null) {
                            val helper = HtmlHelper.getInstance(otherDoc)
                            helper.removeJS(otherDoc)
                            helper.additionalProcessing(otherDoc)
                            doc.body().append(otherDoc.body().html())
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            progressLayout.showContent()
        }
    }

    private fun applyTheme() {
        doc?.let {
            HtmlHelper.getInstance(it).toggleTheme(dataCenter.isDarkTheme, it)
            loadCreatedDocument()
        }
    }

    fun checkUrl(url: String?): Boolean {
        if (url == null) return false
        if (webPageSettings.metaData.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
            val links: ArrayList<String> = Gson().fromJson(webPageSettings.metaData[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES], object : TypeToken<java.util.ArrayList<String>>() {}.type)
            links.forEach {
                val tempWebPageSettings = dbHelper.getWebPageSettings(it) ?: return@forEach
                if (it == url || (tempWebPageSettings.redirectedUrl != null && tempWebPageSettings.redirectedUrl == url)) {
                    history.add(tempWebPageSettings)
                    webPageSettings = tempWebPageSettings
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
        when (event.setting) {
            ReaderSettingsEvent.NIGHT_MODE -> {
                applyTheme()
            }
            ReaderSettingsEvent.READER_MODE -> {
                readerWebView.loadUrl("about:blank")
                readerWebView.clearHistory()
                loadData()
            }
            ReaderSettingsEvent.TEXT_SIZE -> {
                changeTextSize()
            }
            ReaderSettingsEvent.JAVA_SCRIPT -> {
                readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled
                loadData()
            }
            ReaderSettingsEvent.FONT -> {
                loadData()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        webPageSettings.let {
            it.metaData[Constants.MetaDataKeys.SCROLL_POSITION] = readerWebView.scrollY.toString()
            dbHelper.updateWebPageSettings(it)
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        async.cancelAll()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("webPage", webPage)
        outState.putSerializable("webPageSettings", webPageSettings)
        outState.putSerializable("history", history)
    }
}
