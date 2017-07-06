package com.mgn.bingenovelreader.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.WebPageAdapter
import com.mgn.bingenovelreader.dataCenter
import com.mgn.bingenovelreader.util.Constants
import kotlinx.android.synthetic.main.fragment_reader.*
import org.jsoup.Jsoup
import java.io.File


class WebPageFragment : Fragment() {

    var listener: WebPageAdapter.Listener? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_reader, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val filePath = arguments.getString(FILE_PATH)
        if (filePath == null) {

        } else {
            val internalFilePath = "file://$filePath"

            if (filePath.contains("royalroadl.com")) {
                applyRoyalRoadTheme(internalFilePath)
            } else {
                readerWebView.loadUrl(internalFilePath)
            }

            setWebView()
        }
    }

    fun setWebView() {
        //readerWebView.setOnClickListener { if (!floatingToolbar.isShowing) floatingToolbar.show() else floatingToolbar.hide() }
        readerWebView.webViewClient = object : WebViewClient() {
            @Suppress("OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                listener?.checkUrl(url)
                return true
            }
        }
    }

    companion object {
        private val FILE_PATH = "filePath"
        fun newInstance(filePath: String?): WebPageFragment {
            val fragment = WebPageFragment()
            val args = Bundle()
            args.putString(FILE_PATH, filePath)
            fragment.arguments = args
            return fragment
        }
    }

    private fun applyRoyalRoadTheme(internalFilePath: String) {
        val input = File(internalFilePath.substring(7))
        val doc = Jsoup.parse(input, "UTF-8", internalFilePath)
        val element = doc.head().getElementById(Constants.ROYAL_ROAD_SITE_THEME_ID)
        if (element != null) {
            element.removeAttr("href")
            if (dataCenter.isDarkTheme)
                element.attr("href", "../Site-dark.css")
            else
                element.attr("href", "../Site.css")
        }
        readerWebView.loadDataWithBaseURL(internalFilePath, doc.toString(), "text/html", "UTF-8", null)
    }

}
