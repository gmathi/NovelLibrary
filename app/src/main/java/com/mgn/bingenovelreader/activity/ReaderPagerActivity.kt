package com.mgn.bingenovelreader.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.database.getAllReadableWebPages
import com.mgn.bingenovelreader.database.updateCurrentWebPageId
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.model.WebPage
import com.mgn.bingenovelreader.util.Constants
import kotlinx.android.synthetic.main.activity_reader_pager.*
import kotlinx.android.synthetic.main.fragment_reader.*

class ReaderPagerActivity : AppCompatActivity(), ViewPager.OnPageChangeListener {

    private var adapter: WebPageAdapter? = null
    private var chapters = ArrayList<WebPage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_pager)

        val novelId = intent.getLongExtra(Constants.NOVEL_ID, -1L)
        if (novelId == -1L) finish()

        chapters = ArrayList<WebPage>(dbHelper.getAllReadableWebPages(novelId))
        adapter = WebPageAdapter(supportFragmentManager, chapters, object : WebPageAdapter.Listener {
            override fun checkUrl(url: String?) {
                if (url != null) {
                    val index = chapters.indexOfFirst { it.redirectedUrl!!.contains(url) }
                    if (index != -1) {
                        viewPager.currentItem = index
                        dbHelper.updateCurrentWebPageId(chapters[index].novelId, chapters[index].id)
                    }
                }
            }
        })

        viewPager.addOnPageChangeListener(this)
        viewPager.adapter = adapter

        val webPageId = intent.getLongExtra(Constants.WEB_PAGE_ID, -1L)
        if (webPageId != -1L) {
            viewPager.currentItem = chapters.indexOfFirst { it.id == webPageId }
            dbHelper.updateCurrentWebPageId(novelId, webPageId)
        }
    }

    override fun onPageSelected(position: Int) {
        dbHelper.updateCurrentWebPageId(chapters[position].novelId, chapters[position].id)
    }

    override fun onPageScrollStateChanged(position: Int) {
        //Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        //Do Nothing
    }


    /**
     * A WebView fragment containing a simple WebView.
     */
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
                readerWebView.loadUrl(internalFilePath)
                setWebView()
            }
        }

        fun setWebView() {
            readerWebView.setWebViewClient(object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    listener?.checkUrl(url)
                    return true
                }
            })
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
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class WebPageAdapter(fm: FragmentManager, val chapters: MutableList<WebPage>, val listener: Listener) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            val fragment = WebPageFragment.newInstance(chapters[position].filePath)
            fragment.listener = listener
            return fragment
        }

        override fun getCount(): Int {
            return chapters.size
        }

        interface Listener {
            fun checkUrl(url: String?)
        }
    }

}