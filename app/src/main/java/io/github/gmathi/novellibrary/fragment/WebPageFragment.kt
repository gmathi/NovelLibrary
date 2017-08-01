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
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.WebPageAdapter
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_reader_pager.*
import kotlinx.android.synthetic.main.fragment_reader.*
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

    lateinit var webPage: WebPage
    lateinit var doc: Document

    var listener: WebPageAdapter.Listener? = null
    var isCleaned: Boolean = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCleaned = false
        //Get WebPage from Intent
        val intentWebPage = arguments.getSerializable(WEB_PAGE) as WebPage?
        if (intentWebPage == null) activity.finish()
        else webPage = intentWebPage

        //Hide the clean button
        if (dataCenter.cleanChapters) {
            activity.fabClean.hide()
        }

        doc = Jsoup.parse("<html></html>", webPage.url)

        //Setup Scrolling Fab
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            readerWebView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                run {
                    if (scrollY > oldScrollY && scrollY > 0) {
                        activity.floatingToolbar.hide()
                        activity.fab.hide()
                    }
                    if (scrollY < oldScrollY) {
                        activity.fab.show()
                    }
                }
            }
        }

        //Setup WebView
        setWebView()

        loadData()

        swipeRefreshLayout.setOnRefreshListener { loadData() }
    }

    fun loadData() {
        //Load with downloaded HTML File
        if (webPage.filePath != null) {
            val internalFilePath = "file://${webPage.filePath}"
            val input = File(internalFilePath.substring(7))

            var url = webPage.redirectedUrl
            if (url == null) url = internalFilePath

            doc = Jsoup.parse(input, "UTF-8", url)
            if (dataCenter.cleanChapters) cleanPage() else
                loadDocument()
            swipeRefreshLayout.isRefreshing = false
        }
        //Load from Internet
        else {
            if (webPage.url != null)
                downloadWebPage(webPage.url!!)
        }
    }

    fun setWebView() {
        readerWebView.settings.javaScriptEnabled = !dataCenter.javascriptDisabled
        readerWebView.webViewClient = object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                //First page
                if (url == doc.location()) {
                    return true
                }

                //Handle the known links like next and previous chapter if downloaded
                if (listener != null && listener!!.checkUrl(url)) return true

                //Any other urls that are not part of the index
                if (url != null) {
                    downloadWebPage(url)
                    return true
                }

                //If everything else fails, default loading of the WebView
                return false
            }
        }
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
                val cleaner = HtmlHelper.getInstance(doc.location())
                if (dataCenter.cleanChapters) {
                    cleaner.removeJS(doc)
                    cleaner.additionalProcessing(doc)
                    cleaner.toggleTheme(dataCenter.isDarkTheme, doc)
                } else {
                    isCleaned = false
                    activity.fabClean.show()
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

    fun loadDocument() {
        readerWebView.loadDataWithBaseURL(
            doc.location(),
            doc.outerHtml(),
            "text/html", "UTF-8", null)
    }

    fun applyTheme() {
        HtmlHelper.getInstance(doc.location()).toggleTheme(dataCenter.isDarkTheme, doc)
    }

    fun getUrl(): String? {
        if (doc.location() != null) return doc.location()
        return webPage.url
    }


    fun cleanPage() {
        if (!isCleaned) {
            progressLayout.showLoading()
            readerWebView.settings.javaScriptEnabled = false
            HtmlHelper.getInstance(doc.location()).removeJS(doc)
            HtmlHelper.getInstance(doc.location()).additionalProcessing(doc)
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

}
