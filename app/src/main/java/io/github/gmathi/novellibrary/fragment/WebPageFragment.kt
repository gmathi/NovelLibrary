package io.github.gmathi.novellibrary.fragment

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import co.metalab.asyncawait.async
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderPagerActivity
import io.github.gmathi.novellibrary.activity.startChaptersActivity
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.model.NightModeChangeEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_reader_pager.*
import kotlinx.android.synthetic.main.fragment_reader.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File


class WebPageFragment : Fragment() {

    companion object {
        private val WEB_PAGE = "webPage"
        fun newInstance(webPage: WebPage): WebPageFragment {
            val fragment = WebPageFragment()
            val args = Bundle()
            args.putSerializable(WEB_PAGE, webPage)
            fragment.arguments = args
            return fragment
        }
    }

    var webPage: WebPage? = null
    lateinit var doc: Document

    private var isCleaned: Boolean = false
    var history: ArrayList<WebPage> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            readerWebView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                if (scrollY > oldScrollY && scrollY > 0) {
                    activity.floatingToolbar.hide()
                    activity.fab.hide()
                }
                if (oldScrollY - scrollY > Constants.SCROLL_LENGTH) activity.fab.show()

                //if (scrollY < oldScrollY) activity.fab.show()
            }

        swipeRefreshLayout.setOnRefreshListener { loadData() }
        setWebView()

        @Suppress("UNCHECKED_CAST")
        if (savedInstanceState != null && savedInstanceState.containsKey("webPage")) {
            webPage = savedInstanceState.getSerializable("webPage") as WebPage
            isCleaned = savedInstanceState.getBoolean("isCleaned")
            history = savedInstanceState.getSerializable("history") as ArrayList<WebPage>
        } else {
            isCleaned = false
            val intentWebPage = arguments.getSerializable(WEB_PAGE) as WebPage?
            if (intentWebPage == null) activity.finish()
            else webPage = intentWebPage
        }

        if (webPage == null || webPage?.url == null) {
            val activity = (activity as ReaderPagerActivity?)
            activity?.startChaptersActivity(activity.novel!!, false)
            activity?.finish()
        }

        doc = Jsoup.parse("<html></html>", webPage!!.url)
        loadData()
    }

    private fun loadData() {
        //Load with downloaded HTML File
        readerWebView.isVerticalScrollBarEnabled = dataCenter.showReaderScroll
        isCleaned = false
        activity.fabClean.visibility = if (isCleaned || dataCenter.cleanChapters) View.INVISIBLE else View.VISIBLE

        if (webPage!!.filePath != null) {
            val internalFilePath = "file://${webPage!!.filePath}"
            val input = File(internalFilePath.substring(7))

            var url = webPage!!.redirectedUrl
            if (url == null) url = internalFilePath

            doc = Jsoup.parse(input, "UTF-8", url)
            if (dataCenter.cleanChapters) cleanPage() else
                loadDocument()
            swipeRefreshLayout.isRefreshing = false
        }
        //Load from Internet
        else {
            if (webPage!!.url != null)
                downloadWebPage(webPage!!.url!!)
        }
    }

    private fun setWebView() {
        readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled
        readerWebView.webViewClient = object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                //First page
                if (url == doc.location()) {
                    return true
                }

                //Handle the known links like next and previous chapter if downloaded
                if (checkUrl(url)) return true

                //Any other urls that are not part of the index
                if (url != null) {
                    downloadWebPage(url)
                    return true
                }

                //If everything else fails, default loading of the WebView
                return false
            }
        }
        //readerWebView.setOnScrollChangeListener { webView, i, i, i, i ->  }
        changeTextSize(dataCenter.textSize)
    }

    private fun downloadWebPage(url: String) {
        async download@ {

            try {
                progressLayout.showLoading()

                //If no network
                if (!Utils.checkNetwork(activity)) {
                    progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                        downloadWebPage(url)
                    })
                    return@download
                }

                doc = await { NovelApi().getDocumentWithUserAgent(url) }
                val cleaner = HtmlHelper.getInstance(doc)
                doc.getElementsByTag("img")?.forEach {
                    if (it.hasAttr("src")) {
                        it.attr("src", it.absUrl("src"))
                    }
                }
                doc.getElementsByTag("a")?.forEach {
                    if (it.hasAttr("href")) {
                        it.attr("href", it.absUrl("href"))
                    }
                }
                if (dataCenter.cleanChapters) {
                    cleaner.removeJS(doc)
                    cleaner.additionalProcessing(doc)
                    cleaner.toggleTheme(dataCenter.isDarkTheme, doc)
                } else {
                    isCleaned = false
                    activity.fabClean.visibility = View.VISIBLE
                }

                loadDocument()
                swipeRefreshLayout.isRefreshing = false
                progressLayout.showContent()

            } catch (e: Exception) {
                e.printStackTrace()
                if (isResumed && !isRemoving && !isDetached)
                    progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                        downloadWebPage(url)
                    })
            }
        }
    }

    fun changeTextSize(size: Int) {
        val settings = readerWebView.settings
        settings.textZoom = (size + 50) * 2
    }

    private fun loadDocument() {
        readerWebView.loadDataWithBaseURL(
            if (webPage!!.filePath != null) "file://${webPage!!.filePath}" else doc.location(),
            doc.outerHtml(),
            "text/html", "UTF-8", null)
        if (webPage!!.metaData.containsKey("scrollY"))
            readerWebView.scrollTo(0, webPage!!.metaData["scrollY"]!!.toInt())
    }

    private fun applyTheme() {
        HtmlHelper.getInstance(doc).toggleTheme(dataCenter.isDarkTheme, doc)
    }

    fun getUrl(): String? {
        if (doc.location() != null) return doc.location()
        return webPage!!.url
    }


    fun cleanPage() {
        if (!isCleaned) {
            progressLayout.showLoading()
            readerWebView.settings.javaScriptEnabled = false
            val htmlHelper = HtmlHelper.getInstance(doc)
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            applyTheme()
            loadDocument()
            progressLayout.showContent()
            isCleaned = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

    private fun loadNewWebPage(otherWebPage: WebPage) {
        history.add(webPage!!)
        webPage = otherWebPage
        loadData()
    }

    fun goBack() {
        webPage = history.last()
        history.remove(webPage!!)
        loadData()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        //webPage.metaData.put("scrollY", readerWebView.scrollY.toString())
        if (webPage != null)
            outState?.putSerializable("webPage", webPage)
        outState?.putBoolean("isCleaned", isCleaned)
        outState?.putSerializable("history", history)
    }

    fun checkUrl(url: String?): Boolean {
        if (url == null) return false

        if (webPage!!.metaData.containsKey(Constants.MD_OTHER_LINKED_WEB_PAGES)) {
            val links: ArrayList<WebPage> = Gson().fromJson(webPage!!.metaData[Constants.MD_OTHER_LINKED_WEB_PAGES], object : TypeToken<java.util.ArrayList<WebPage>>() {}.type)
            links.forEach {
                if (it.url == url || (it.redirectedUrl != null && it.redirectedUrl == url)) {
                    loadNewWebPage(it)
                    return@checkUrl true
                }
            }
        }

        val readerActivity = (activity as ReaderPagerActivity?) ?: return false
        return readerActivity.checkUrl(url)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNightModeChanged(@Suppress("UNUSED_PARAMETER") event: NightModeChangeEvent) {
        cleanPage()
        applyTheme()
        loadDocument()
    }


}
