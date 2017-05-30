package com.mgn.bingenovelreader.activities

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.dataCenter
import com.mgn.bingenovelreader.models.WebPage
import kotlinx.android.synthetic.main.activity_downloader.*
import kotlinx.android.synthetic.main.content_downloader.*
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.SimpleHtmlSerializer
import org.htmlcleaner.TagNode
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL


class DownloaderActivity : AppCompatActivity() {

    private var isDownoading = false
    var name: String? = null
    val cacheList: ArrayList<WebPage> = ArrayList()
    var pageCount: String? = null
    var searchText: String? = null
    var iterationCounter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloader)
        setSupportActionBar(toolbar)
        addTestCode()
        addListeners()
    }

    private fun addTestCode() {
        //Test Code
        downloaderTitleEditText.setText("Novel " + (dataCenter.cacheMap.size + 1))
        downloaderPageCountEditText.setText("30")
        downloaderSearchTextEditText.setText("Next Chapter")
        downloaderUrlEditText.setText("https://royalroadl.com/fiction/chapter/127131")
    }

    private fun addListeners() {
        downloaderLog.typeface = Typeface.createFromAsset(assets, "font/roboto_light.ttf")
        downloaderLog.movementMethod = ScrollingMovementMethod()
        downloaderFab.setOnClickListener {
            if (downloaderFab.tag == "pristine")
                checkFieldsNStart()
            else finish()
        }
    }

    private fun checkFieldsNStart() {
        val url = downloaderUrlEditText.text.toString()
        name = downloaderTitleEditText.text.toString()
        pageCount = downloaderPageCountEditText.text.toString()
        searchText = downloaderSearchTextEditText.text.toString()

        if (url == "" || name == "" || pageCount == "" || searchText == "") {
            alert("Fields cannot be empty!")
            return
        }

        iterationCounter = pageCount!!.toInt()
        downloaderFab.hide()
        downloaderProgressBar.visibility = View.VISIBLE
        hideKeyboard()
        addMessage("Started Downloading...")
        processUrl(url)
    }

    private fun processUrl(url: String) {
        Thread(Runnable {
            try {

                Handler(Looper.getMainLooper()).post {
                    addMessage("Downloading Url: " + url)
                }
                val cleaner = HtmlCleaner()
                val node = cleaner.clean(URL(url))
                var title: String? = null
                var newUrl: String? = null

                Handler(Looper.getMainLooper()).post {
                    addMessage("Reading its data...")
                }

                node.traverse({ _, htmlNode ->
                    if (htmlNode is TagNode) {
                        if ("title" == htmlNode.name) {
                            title = htmlNode.text.toString()
                        } else if ("a" == htmlNode.name && searchText == htmlNode.text.toString()) {
                            newUrl = htmlNode.getAttributeByName("href")
                        }
                    }
                    (title == null || newUrl == null) //return 'true' to continue & 'false' to stop traverse
                })

                if (newUrl != null && title != null && iterationCounter > 0) {
                    newUrl = cleanUrl(url, newUrl!!)
                    val webPage = WebPage(newUrl!!, title)
                    val serializer = SimpleHtmlSerializer(cleaner.properties)
                    webPage.pageData = serializer.getAsString(node)
                    cacheList.add(webPage)
                    iterationCounter--
                    Handler(Looper.getMainLooper()).post {
                        addMessage("Downloaded - " + webPage.title + "\n" + "Url: " + webPage.url)
                        processUrl(newUrl!!)
                    }
                    Log.d("process", title + " " + newUrl)

                } else {
                    Handler(Looper.getMainLooper()).post {
                        dataCenter.cacheMap.put(name!!, cacheList)
                        dataCenter.saveCacheMap()
                        addMessage("Download Complete.")
                        addMessage("Data has been saved.")
                        addMessage("Click on check or hit back to close!")
                        downloaderFab.setImageResource(R.drawable.ic_check_black_24dp)
                        downloaderFab.tag = "downloadComplete"
                        downloaderFab.show()
                        downloaderProgressBar.visibility = View.GONE
                    }
                }

            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    addMessage("Download Failed: " + e.localizedMessage)
                }
                e.printStackTrace()
            }
        }).start()
    }

    fun cleanUrl(oldUrl: String, newUrl: String): String {
        val uri = Uri.parse(newUrl)
        if (uri.scheme == null || uri.host == null) {
            val oldUri = Uri.parse(oldUrl)
            return oldUri.scheme + "://" + oldUri.host + newUrl
        } else return newUrl
    }

    fun alert(message: String) {
        val alertBox: AlertDialog.Builder = AlertDialog.Builder(this)
        alertBox.setCancelable(true)
        alertBox.setMessage(message)
        alertBox.setPositiveButton("ok", null)
        alertBox.create().show()
    }


    private fun addMessage(msg: String) {
        downloaderLog.append("\n" + msg)
        val scrollAmount = downloaderLog.layout.getLineTop(downloaderLog.lineCount) - downloaderLog.height
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            downloaderLog.scrollTo(0, scrollAmount)
        else
            downloaderLog.scrollTo(0, 0)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    fun processUrlNew(url: String) {
        Thread(Runnable {
            try {
                Handler(Looper.getMainLooper()).post {
                    addMessage("Downloading Url: " + url)
                }
                var doc = Jsoup.connect(url).get()
                val title = doc.head().getElementsByTag("title").`val`()
                val newUrl: String? = doc.getElementsByTag("a")
                        .firstOrNull { it.`val`().equals("Next Chapter") }
                        ?.attr("href")


            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    addMessage("Download Failed: " + e.localizedMessage)
                }
                e.printStackTrace()
            }
        }).start()
    }
}
