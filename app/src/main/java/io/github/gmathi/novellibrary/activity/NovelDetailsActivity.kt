package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.bumptech.glide.Glide
import com.google.firebase.analytics.ktx.logEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getNovelDetails
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.view.TextViewLinkHandler
import kotlinx.android.synthetic.main.activity_novel_details.*
import kotlinx.android.synthetic.main.content_novel_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class NovelDetailsActivity : BaseActivity(), TextViewLinkHandler.OnClickListener {

    companion object {
        const val TAG = "NovelDetailsActivity"
    }

    lateinit var novel: Novel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novel_details)

        novel = intent.getSerializableExtra("novel") as Novel
        getNovelInfoDB()
        if (intent.hasExtra(Constants.JUMP))
            startChaptersActivity(novel, true)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = novel.name

        if (novel.id != -1L) {
            if (!Utils.isConnectedToNetwork(this))
                setupViews()
            else
                progressLayout.showLoading()
        } else {
            progressLayout.showLoading()
        }
        getNovelInfo()
        swipeRefreshLayout.setOnRefreshListener { getNovelInfoDB(); getNovelInfo() }
    }

    private fun getNovelInfoDB() {
        val dbNovel = dbHelper.getNovel(novel.name)
        if (dbNovel != null) {
            novel.copyFrom(dbNovel)
        }
    }

    private fun getNovelInfo() {
        if (!Utils.isConnectedToNetwork(this)) {
            if (novel.id == -1L) {
                swipeRefreshLayout.isRefreshing = false
                progressLayout.noInternetError {
                    progressLayout.showLoading()
                    getNovelInfo()
                }
            }
            return
        }

        lifecycleScope.launch {
            try {
                val downloadedNovel = withContext(Dispatchers.IO) { NovelApi.getNovelDetails(novel.url) }
                novel.copyFrom(downloadedNovel)
                addNovelToHistory()
                if (novel.id != -1L) withContext(Dispatchers.IO) { dbHelper.updateNovel(novel) }
                setupViews()
                progressLayout.showContent()
                swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = e.localizedMessage ?: "Unknown Error" + "\n" + e.stackTrace.joinToString(separator = "\n") { it.toString() }
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("Error Message", errorMessage)
                clipboard.setPrimaryClip(clip)
                MaterialDialog.Builder(this@NovelDetailsActivity).title("Error!").content("The error message has been copied to clipboard. Please paste it and send it the developer in discord.")
                    .show()
                if (novel.id == -1L)
                    progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again)) {
                        progressLayout.showLoading()
                        getNovelInfo()
                    }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        setNovelImage()

        novelDetailsName.applyFont(assets).text = novel.name
        novelDetailsName.isSelected = dataCenter.enableScrollingText

        val listener: View.OnClickListener = View.OnClickListener {
            MaterialDialog.Builder(this)
                .title("Novel Name")
                .content(novel.name)
                .build().show()
        }
        novelDetailsName.setOnClickListener(listener)
        novelDetailsNameInfo.setOnClickListener(listener)

        setNovelAuthor()

        novelDetailsStatus.applyFont(assets).text = "N/A"
        if (novel.metadata["Year"] != null)
            novelDetailsStatus.applyFont(assets).text = novel.metadata["Year"]

        setLicensingInfo()
        setNovelRating()
        setNovelAddToLibraryButton()
        setNovelGenre()
        setNovelDescription()

        novelDetailsChapters.text = getString(R.string.chapters) + " (${novel.chaptersCount})"
        novelDetailsChaptersLayout.setOnClickListener {
            if (novel.chaptersCount != 0L) startChaptersActivity(novel, false)
        }
        novelDetailsMetadataLayout.setOnClickListener { startMetadataActivity(novel) }
        openInBrowserButton.setOnClickListener { openInBrowser(novel.url) }
    }

    private fun setNovelImage() {
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(novel.imageUrl?.getGlideUrl())
                .into(novelDetailsImage)
            novelDetailsImage.setOnClickListener { startImagePreviewActivity(novel.imageUrl, novel.imageFilePath, novelDetailsImage) }
        }
    }

    private fun setNovelAuthor() {
        val author = novel.metadata["Author(s)"]
        if (author != null) {
            novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
            @Suppress("DEPRECATION")
            novelDetailsAuthor.applyFont(assets).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(author, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(author)
        }
    }

    @Suppress("DEPRECATION")
    private fun setLicensingInfo() {
        if (novel.metadata["English Publisher"] ?: "" != "" || novel.metadata["Licensed (in English)"] == "Yes") {
            val publisher = if (novel.metadata["English Publisher"] == null || novel.metadata["English Publisher"] == "") "an unknown publisher" else novel.metadata["English Publisher"]
            val warningLabel = getString(R.string.licensed_warning, publisher)
            novelDetailsLicensedAlert.movementMethod = TextViewLinkHandler(this)
            novelDetailsLicensedAlert.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(warningLabel, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(warningLabel)
            novelDetailsLicensedLayout.visibility = View.VISIBLE
        } else {
            novelDetailsLicensedLayout.visibility = View.GONE
        }
    }

    private fun setNovelRating() {
        if (novel.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = novel.rating!!.toFloat()
                novelDetailsRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("Library Activity", "Rating: " + novel.rating, e)
            }
            novelDetailsRatingText.text = ratingText
        }
    }

    private fun setNovelAddToLibraryButton() {
        if (novel.id == -1L) {
            resetAddToLibraryButton()
            novelDetailAddToLibraryButton.setOnClickListener {
                disableAddToLibraryButton()
                addNovelToDB()
            }
        } else disableAddToLibraryButton()
    }

    private fun addNovelToDB() {
        if (novel.id == -1L) {
            novel.id = dbHelper.insertNovel(novel)
            NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncAddNovels(it.host)) it.addNovel(novel, null) }
            firebaseAnalytics.logEvent(FAC.Event.ADD_NOVEL) {
                param(FAC.Param.NOVEL_NAME, novel.name)
                param(FAC.Param.NOVEL_URL, novel.url)
            }
        }
    }

    private fun resetAddToLibraryButton() {
        novelDetailAddToLibraryButton.setText(getString(R.string.add_to_library))
        novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_library_add_white_vector)
        novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, android.R.color.transparent))
        novelDetailAddToLibraryButton.isClickable = true
    }

    private fun disableAddToLibraryButton() {
        invalidateOptionsMenu()
        novelDetailAddToLibraryButton.setText(getString(R.string.in_library))
        novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_local_library_white_vector)
        novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, R.color.Green))
        novelDetailAddToLibraryButton.isClickable = false
    }


    private fun setNovelGenre() {
        novelDetailsGenresLayout.removeAllViews()
        if (novel.genres != null && novel.genres!!.isNotEmpty()) {
            novel.genres!!.forEach {
                novelDetailsGenresLayout.addView(getGenreTextView(it))
            }
        } else novelDetailsGenresLayout.addView(getGenreTextView("N/A"))
    }

    private fun getGenreTextView(genre: String): TextView {
        val textView = TextView(this)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(4, 8, 20, 4)
        textView.layoutParams = layoutParams
        textView.setPadding(8, 8, 8, 8)
        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.LightGoldenrodYellow))
        textView.applyFont(assets).text = genre
        textView.setTextColor(ContextCompat.getColor(this, R.color.black))
        return textView
    }

    private fun setNovelDescription() {
        if (novel.longDescription != null) {
            val expandClickable = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    novelDetailsDescription.applyFont(assets).text = novel.longDescription
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            val novelDescription = "${novel.longDescription?.subSequence(0, min(300, novel.longDescription?.length ?: 0))}… Expand"
            val ss2 = SpannableString(novelDescription)
            ss2.setSpan(expandClickable, min(300, novel.longDescription?.length ?: 0) + 2, novelDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            novelDetailsDescription.applyFont(assets).text = ss2
            novelDetailsDescription.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_novel_details, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.isVisible = novel.id != -1L
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_delete_novel -> confirmNovelDelete()
            R.id.action_share -> shareUrl(novel.url)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmNovelDelete() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.confirm_remove))
            .content(getString(R.string.confirm_remove_description_novel))
            .positiveText(getString(R.string.remove))
            .negativeText(getString(R.string.cancel))
            .icon(ContextCompat.getDrawable(this, R.drawable.ic_delete_white_vector)!!)
            .typeface("source_sans_pro_regular.ttf", "source_sans_pro_regular.ttf")
            .theme(Theme.DARK)
            .onPositive { _, _ -> deleteNovel() }
            .show()
    }

    private fun deleteNovel() {
        Utils.deleteNovel(this, novel.id)
        novel.id = -1L
        setNovelAddToLibraryButton()
        invalidateOptionsMenu()
        firebaseAnalytics.logEvent(FAC.Event.REMOVE_NOVEL) {
            param(FAC.Param.NOVEL_NAME, novel.name)
            param(FAC.Param.NOVEL_URL, novel.url)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.CHAPTER_ACT_REQ_CODE) {
            if (resultCode == Constants.OPEN_DOWNLOADS_RES_CODE) {
                setResult(resultCode)
                finish()
                return
            }
            getNovelInfoDB()
            setNovelAddToLibraryButton()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onLinkClicked(title: String, url: String) {
        startSearchResultsActivity(title, url)
    }

    private fun addNovelToHistory() {
        try {
            var history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
                ?: "[]"
            var historyList: ArrayList<Novel> = Gson().fromJson(history, object : TypeToken<ArrayList<Novel>>() {}.type)
            Logs.info(TAG, "Novel Search History Size: ${historyList.size}")
            historyList.removeAll { novel.name == it.name }
            if (historyList.size > 99)
                historyList = ArrayList(historyList.take(99))
            historyList.add(novel)
            history = Gson().toJson(historyList)
            dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, history)
        } catch (e: Exception) {
            Logs.error(TAG, "Error adding novel to history. Resetting the history", e)
            dbHelper.deleteLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
        }
    }
}
