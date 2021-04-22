package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
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
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.google.firebase.analytics.ktx.logEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityNovelDetailsBinding
import io.github.gmathi.novellibrary.databinding.ContentNovelDetailsBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min


class NovelDetailsActivity : BaseActivity(), TextViewLinkHandler.OnClickListener {

    companion object {
        const val TAG = "NovelDetailsActivity"
    }

    lateinit var novel: Novel

    private lateinit var binding: ActivityNovelDetailsBinding
    private lateinit var contentBinding: ContentNovelDetailsBinding

    private var retryCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentBinding = binding.contentNovelDetails

        //Get Novel from intent
        novel = intent.getParcelableExtra<Novel>("novel") as Novel

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = novel.name

        //check for novel in database
        dbHelper.getNovelByUrl(novel.url)?.let { novel.id = it.id }

        if (novel.id != -1L && !networkHelper.isConnectedToNetwork()) {
            setupViews()
        } else {
            getNovelInfo()
        }

        contentBinding.swipeRefreshLayout.setOnRefreshListener { getNovelInfo() }
    }

    private fun getNovelInfo() {
        contentBinding.progressLayout.showLoading()

        //Check for network
        if (!networkHelper.isConnectedToNetwork()) {
            contentBinding.swipeRefreshLayout.isRefreshing = false

            if (novel.id == -1L) { //If novel not present in library -> show error screen
                contentBinding.progressLayout.noInternetError {
                    getNovelInfo()
                }
            } else { // else just show a toast
                toast(R.string.no_internet)
            }
            return
        }

        //Download novel details
        lifecycleScope.launch {
            try {
                val source = sourceManager.get(novel.sourceId) ?: throw Exception(Exceptions.MISSING_SOURCE_ID)
                novel = withContext(Dispatchers.IO) { source.getNovelDetails(novel) }

                //Update the novel in library with the new info
                if (novel.id != -1L) withContext(Dispatchers.IO) { dbHelper.updateNovel(novel) }
                addNovelToHistory()
                setupViews()
                contentBinding.swipeRefreshLayout.isRefreshing = false
                contentBinding.progressLayout.showContent()
                retryCounter = 0
            } catch (e: Exception) {
                if (e.message?.contains(Exceptions.MISSING_SOURCE_ID) == true) {
                    toast("Missing Novel Source Id. Please re-add the novel")
                    return@launch
                }

                if (e.message?.contains(getString(R.string.information_cloudflare_bypass_failure)) == true
                    || e.message?.contains("HTTP error 503") == true && retryCounter < 2
                ) {
                    resolveCloudflare(novel.url) { success, _, errorMessage ->
                        if (success) {
                            toast("Cloudflare Success")
                            retryCounter++
                            getNovelInfo()
                        } else {
                            toast("Cloudflare Failed")
                            contentBinding.progressLayout.showError(errorText = errorMessage ?: getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again)) {
                                contentBinding.progressLayout.showLoading()
                                getNovelInfo()
                            }
                            contentBinding.swipeRefreshLayout.isRefreshing = false
                        }
                    }
                    return@launch
                }

                //Copy the error to clipboard
                if (!isDestroyed && !isFinishing)
                    Utils.copyErrorToClipboard(e, this@NovelDetailsActivity)

                if (novel.id == -1L) {
                    contentBinding.progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again)) {
                        contentBinding.progressLayout.showLoading()
                        getNovelInfo()
                    }
                }
                contentBinding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        setNovelImage()

        contentBinding.novelDetailsName.applyFont(assets).text = novel.name
        contentBinding.novelDetailsName.isSelected = dataCenter.enableScrollingText

        val listener: View.OnClickListener = View.OnClickListener {
            MaterialDialog(this).show {
                title(text = "Novel Name")
                message(text = novel.name)

                lifecycleOwner(this@NovelDetailsActivity)
            }
        }
        contentBinding.novelDetailsName.setOnClickListener(listener)
        contentBinding.novelDetailsNameInfo.setOnClickListener(listener)

        setNovelAuthor()

        contentBinding.novelDetailsStatus.applyFont(assets).text = "N/A"
        if (novel.metadata["Year"] != null)
            contentBinding.novelDetailsStatus.applyFont(assets).text = novel.metadata["Year"]

        setLicensingInfo()
        setNovelRating()
        setNovelAddToLibraryButton()
        setNovelGenre()
        setNovelDescription()

        contentBinding.novelDetailsChapters.text = getString(R.string.chapters) + " (${novel.chaptersCount})"
        contentBinding.novelDetailsChaptersLayout.setOnClickListener {
            if (novel.chaptersCount != 0L) startChaptersActivity(novel, false)
        }
        contentBinding.novelDetailsMetadataLayout.setOnClickListener { startMetadataActivity(novel) }
        contentBinding.openInBrowserButton.setOnClickListener { openInBrowser(novel.url) }
    }

    private fun setNovelImage() {
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(novel.imageUrl?.getGlideUrl())
                .into(contentBinding.novelDetailsImage)
            contentBinding.novelDetailsImage.setOnClickListener { startImagePreviewActivity(novel.imageUrl, novel.imageFilePath, contentBinding.novelDetailsImage) }
        }
    }

    private fun setNovelAuthor() {
        val author = novel.metadata["Author(s)"]
        if (author != null) {
            contentBinding.novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
            @Suppress("DEPRECATION")
            contentBinding.novelDetailsAuthor.applyFont(assets).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(author, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(author)
        }
    }

    @Suppress("DEPRECATION")
    private fun setLicensingInfo() {
        if (novel.metadata["English Publisher"] ?: "" != "" || novel.metadata["Licensed (in English)"] == "Yes") {
            val publisher = if (novel.metadata["English Publisher"] == null || novel.metadata["English Publisher"] == "") "an unknown publisher" else novel.metadata["English Publisher"]
            val warningLabel = getString(R.string.licensed_warning, publisher)
            contentBinding.novelDetailsLicensedAlert.movementMethod = TextViewLinkHandler(this)
            contentBinding.novelDetailsLicensedAlert.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(warningLabel, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(warningLabel)
            contentBinding.novelDetailsLicensedLayout.visibility = View.VISIBLE
        } else {
            contentBinding.novelDetailsLicensedLayout.visibility = View.GONE
        }
    }

    private fun setNovelRating() {
        if (!novel.rating.isNullOrBlank()) {
            var ratingText = "(N/A)"
            try {
                val rating = novel.rating!!.replace(",", ".").toFloat()
                contentBinding.novelDetailsRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("NovelDetailsActivity", "Rating: ${novel.rating}, Novel: ${novel.name}", e)
            }
            contentBinding.novelDetailsRatingText.text = ratingText
        }
    }

    private fun setNovelAddToLibraryButton() {
        if (novel.id == -1L) {
            resetAddToLibraryButton()
            contentBinding.novelDetailAddToLibraryButton.setOnClickListener {
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
        contentBinding.novelDetailAddToLibraryButton.setText(getString(R.string.add_to_library))
        contentBinding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_library_add_white_vector)
        contentBinding.novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, android.R.color.transparent))
        contentBinding.novelDetailAddToLibraryButton.isClickable = true
    }

    private fun disableAddToLibraryButton() {
        invalidateOptionsMenu()
        contentBinding.novelDetailAddToLibraryButton.setText(getString(R.string.in_library))
        contentBinding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_local_library_white_vector)
        contentBinding.novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, R.color.Green))
        contentBinding.novelDetailAddToLibraryButton.isClickable = false
    }


    private fun setNovelGenre() {
        contentBinding.novelDetailsGenresLayout.removeAllViews()
        if (novel.genres != null && novel.genres!!.isNotEmpty()) {
            novel.genres!!.forEach {
                contentBinding.novelDetailsGenresLayout.addView(getGenreTextView(it))
            }
        } else contentBinding.novelDetailsGenresLayout.addView(getGenreTextView("N/A"))
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
                    contentBinding.novelDetailsDescription.applyFont(assets).text = novel.longDescription
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            val novelDescription = "${novel.longDescription?.subSequence(0, min(300, novel.longDescription?.length ?: 0))}… Expand"
            val ss2 = SpannableString(novelDescription)
            ss2.setSpan(expandClickable, min(300, novel.longDescription?.length ?: 0) + 2, novelDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            contentBinding.novelDetailsDescription.applyFont(assets).text = ss2
            contentBinding.novelDetailsDescription.movementMethod = LinkMovementMethod.getInstance()
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
        MaterialDialog(this).show {
            icon(R.drawable.ic_delete_white_vector)
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_description_novel)
            positiveButton(R.string.remove) {
                deleteNovel()
            }
            negativeButton(R.string.cancel)

            lifecycleOwner(this@NovelDetailsActivity)
        }
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
            //Check if this novel was added to database in Chapters Screen
            dbHelper.getNovelByUrl(novel.url)?.let { novel = it }
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
            historyList.removeAll { novel.name == it.name }
            if (historyList.size > 99)
                historyList = ArrayList(historyList.take(99))
            historyList.add(novel)
            history = Gson().toJson(historyList)
            dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, history)
        } catch (e: Exception) {
            dbHelper.deleteLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
        }
    }
}
