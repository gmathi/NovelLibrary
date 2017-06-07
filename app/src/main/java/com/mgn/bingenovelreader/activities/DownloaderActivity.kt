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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


class DownloaderActivity : AppCompatActivity() {

    private val USER_AGENT: String = "Mozilla/5.0 (Linux; Android 4.0.4; Galaxy Nexus Build/IMM76K) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19"

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
        //dataCenter.cacheMap.clear()
        downloaderTitleEditText.setText("Novel AC" + (dataCenter.cacheMap.size + 1))
        downloaderPageCountEditText.setText("1")
        downloaderSearchTextEditText.setText("Next Chapter")
        downloaderUrlEditText.setText("http://www.wuxiaworld.com/absolute-choice-index/ac-chapter-351/")
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
        processUrlNew(url)
    }

    private fun processUrl(url: String) {
        Thread(Runnable {
            try {
                addMessage("Downloading Url: " + url)
                val cleaner = HtmlCleaner()
                val node = cleaner.clean(URL(url))
                var title: String? = null
                var newUrl: String? = null



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
                    val webPage = WebPage(newUrl!!, title, null)
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
        Handler(Looper.getMainLooper()).post {
            downloaderLog.append("\n" + msg)
            val scrollAmount = downloaderLog.layout.getLineTop(downloaderLog.lineCount) - downloaderLog.height
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                downloaderLog.scrollTo(0, scrollAmount)
            else
                downloaderLog.scrollTo(0, 0)
        }
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
                addMessage("Downloading Url: " + url)
                val doc = Jsoup.connect(url).userAgent(USER_AGENT).get()
                addMessage("Reading Data...")
                val title = doc.head().getElementsByTag("title").text()
                var newUrl: String = doc.getElementsByTag("a")
                        .firstOrNull { it.text().equals("Next Chapter") }
                        ?.attr("href") ?: "#"

                updateCSS(doc)

                if (newUrl != "#" && iterationCounter > 0) {
                    newUrl = cleanUrl(url, newUrl)
                    val webPage = WebPage(url, title, doc.outerHtml())
                    cacheList.add(webPage)
                    iterationCounter--
                    addMessage("Downloaded - " + webPage.title + "\n" + "Url: " + webPage.url)
                    Handler(Looper.getMainLooper()).post {
                        processUrlNew(newUrl)
                    }
                } else {
                    finishProcessing()
                }


            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    addMessage("Download Failed: " + e.localizedMessage)
                }
                e.printStackTrace()
            }
        }).start()
    }

    private fun updateCSS(doc: Document) {
        val elements = doc.head().getElementsByTag("link").filter { element -> element.hasAttr("rel") && element.attr("rel") == "stylesheet" }
        for (element in elements) {
            val cssFile = downloadCSS(element)
            element.remove()
            doc.head().appendElement("link").attr("rel", "stylesheet").attr("type", "text/css").attr("href", ""+cssFile.parentFile.name +"/"+ cssFile.name)
        }
    }

    private fun downloadCSS(element: Element): File {
        val uri = Uri.parse(element.absUrl("href"))
        val path = filesDir

        val dirName = uri.host.replace(Regex.fromLiteral("[^a-zA-Z0-9.-]"), "_")
        val hostDir = File(path, dirName)
        if (!hostDir.exists()) hostDir.mkdir()

        val fileName = (uri.lastPathSegment + uri.toString().substringAfter("?", "")).replace(Regex.fromLiteral("[^a-zA-Z0-9.-]"), "_")
        val cssFile = File(hostDir, fileName)
        if (cssFile.exists()) return cssFile else addMessage("Downloading CSS: " + cssFile.name)

        val doc = Jsoup.connect(uri.toString()).userAgent(USER_AGENT).ignoreContentType(true).get()
        val stream = FileOutputStream(cssFile)
        val content = doc.body().html()
        stream.use { stream ->
            stream.write(content.toByteArray())
        }
        return cssFile
    }

    private fun finishProcessing() {
        Handler(Looper.getMainLooper()).post {
            dataCenter.cacheMap.put(name!!, cacheList)
            dataCenter.saveCacheMap()
            downloaderFab.setImageResource(R.drawable.ic_check_black_24dp)
            downloaderFab.tag = "downloadComplete"
            downloaderFab.show()
            downloaderProgressBar.visibility = View.GONE
        }
        addMessage("Download Complete.")
        addMessage("Data has been saved.")
        addMessage("Click on check or hit back to close!")
    }

}
