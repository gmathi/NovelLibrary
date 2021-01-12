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
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.google.firebase.analytics.ktx.logEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityNovelDetailsBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getNovelDetails
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityNovelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        novel = intent.getSerializableExtra("novel") as Novel
        getNovelInfoDB()
        if (intent.hasExtra(Constants.JUMP))
            startChaptersActivity(novel, true)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = novel.name

        if (novel.id != -1L) {
            if (!Utils.isConnectedToNetwork(this))
                setupViews()
            else
                binding.contentNovelDetails.progressLayout.showLoading()
        } else {
            binding.contentNovelDetails.progressLayout.showLoading()
        }
        getNovelInfo()
        binding.contentNovelDetails.swipeRefreshLayout.setOnRefreshListener { getNovelInfoDB(); getNovelInfo() }
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
                binding.contentNovelDetails.swipeRefreshLayout.isRefreshing = false
                binding.contentNovelDetails.progressLayout.noInternetError {
                    binding.contentNovelDetails.progressLayout.showLoading()
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
                binding.contentNovelDetails.progressLayout.showContent()
                binding.contentNovelDetails.swipeRefreshLayout.isRefreshing = false
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = e.localizedMessage ?: "Unknown Error" + "\n" + e.stackTrace.joinToString(separator = "\n") { it.toString() }
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("Error Message", errorMessage)
                clipboard.setPrimaryClip(clip)
                MaterialDialog(this@NovelDetailsActivity).show {
                    title(text = "Error!")
                    message(text = "The error message has been copied to clipboard. Please paste it and send it the developer in discord.")
                    
                    lifecycleOwner(this@NovelDetailsActivity)
                }
                if (novel.id == -1L)
                    binding.contentNovelDetails.progressLayout.showError(errorText = getString(R.string.failed_to_load_url), buttonText = getString(R.string.try_again)) {
                        binding.contentNovelDetails.progressLayout.showLoading()
                        getNovelInfo()
                    }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupViews() {
        setNovelImage()

        binding.contentNovelDetails.novelDetailsName.applyFont(assets).text = novel.name
        binding.contentNovelDetails.novelDetailsName.isSelected = dataCenter.enableScrollingText

        val listener: View.OnClickListener = View.OnClickListener {
            MaterialDialog(this).show {
                title(text = "Novel Name")
                message(text = novel.name)

                lifecycleOwner(this@NovelDetailsActivity)
            }
        }
        binding.contentNovelDetails.novelDetailsName.setOnClickListener(listener)
        binding.contentNovelDetails.novelDetailsNameInfo.setOnClickListener(listener)

        setNovelAuthor()

        binding.contentNovelDetails.novelDetailsStatus.applyFont(assets).text = "N/A"
        if (novel.metadata["Year"] != null)
            binding.contentNovelDetails.novelDetailsStatus.applyFont(assets).text = novel.metadata["Year"]

        setLicensingInfo()
        setNovelRating()
        setNovelAddToLibraryButton()
        setNovelGenre()
        setNovelDescription()

        binding.contentNovelDetails.novelDetailsChapters.text = getString(R.string.chapters) + " (${novel.chaptersCount})"
        binding.contentNovelDetails.novelDetailsChaptersLayout.setOnClickListener {
            if (novel.chaptersCount != 0L) startChaptersActivity(novel, false)
        }
        binding.contentNovelDetails.novelDetailsMetadataLayout.setOnClickListener { startMetadataActivity(novel) }
        binding.contentNovelDetails.openInBrowserButton.setOnClickListener { openInBrowser(novel.url) }
    }

    private fun setNovelImage() {
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(novel.imageUrl?.getGlideUrl())
                .into(binding.contentNovelDetails.novelDetailsImage)
            binding.contentNovelDetails.novelDetailsImage.setOnClickListener { startImagePreviewActivity(novel.imageUrl, novel.imageFilePath, binding.contentNovelDetails.novelDetailsImage) }
        }
    }

    private fun setNovelAuthor() {
        val author = novel.metadata["Author(s)"]
        if (author != null) {
            binding.contentNovelDetails.novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
            @Suppress("DEPRECATION")
            binding.contentNovelDetails.novelDetailsAuthor.applyFont(assets).text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(author, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(author)
        }
    }

    @Suppress("DEPRECATION")
    private fun setLicensingInfo() {
        if (novel.metadata["English Publisher"] ?: "" != "" || novel.metadata["Licensed (in English)"] == "Yes") {
            val publisher = if (novel.metadata["English Publisher"] == null || novel.metadata["English Publisher"] == "") "an unknown publisher" else novel.metadata["English Publisher"]
            val warningLabel = getString(R.string.licensed_warning, publisher)
            binding.contentNovelDetails.novelDetailsLicensedAlert.movementMethod = TextViewLinkHandler(this)
            binding.contentNovelDetails.novelDetailsLicensedAlert.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(warningLabel, Html.FROM_HTML_MODE_LEGACY) else Html.fromHtml(warningLabel)
            binding.contentNovelDetails.novelDetailsLicensedLayout.visibility = View.VISIBLE
        } else {
            binding.contentNovelDetails.novelDetailsLicensedLayout.visibility = View.GONE
        }
    }

    private fun setNovelRating() {
        if (novel.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = novel.rating!!.toFloat()
                binding.contentNovelDetails.novelDetailsRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("Library Activity", "Rating: " + novel.rating, e)
            }
            binding.contentNovelDetails.novelDetailsRatingText.text = ratingText
        }
    }

    private fun setNovelAddToLibraryButton() {
        if (novel.id == -1L) {
            resetAddToLibraryButton()
            binding.contentNovelDetails.novelDetailAddToLibraryButton.setOnClickListener {
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
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setText(getString(R.string.add_to_library))
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_library_add_white_vector)
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, android.R.color.transparent))
        binding.contentNovelDetails.novelDetailAddToLibraryButton.isClickable = true
    }

    private fun disableAddToLibraryButton() {
        invalidateOptionsMenu()
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setText(getString(R.string.in_library))
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_local_library_white_vector)
        binding.contentNovelDetails.novelDetailAddToLibraryButton.setBackgroundColor(ContextCompat.getColor(this@NovelDetailsActivity, R.color.Green))
        binding.contentNovelDetails.novelDetailAddToLibraryButton.isClickable = false
    }


    private fun setNovelGenre() {
        binding.contentNovelDetails.novelDetailsGenresLayout.removeAllViews()
        if (novel.genres != null && novel.genres!!.isNotEmpty()) {
            novel.genres!!.forEach {
                binding.contentNovelDetails.novelDetailsGenresLayout.addView(getGenreTextView(it))
            }
        } else binding.contentNovelDetails.novelDetailsGenresLayout.addView(getGenreTextView("N/A"))
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
                    binding.contentNovelDetails.novelDetailsDescription.applyFont(assets).text = novel.longDescription
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            val novelDescription = "${novel.longDescription?.subSequence(0, min(300, novel.longDescription?.length ?: 0))}… Expand"
            val ss2 = SpannableString(novelDescription)
            ss2.setSpan(expandClickable, min(300, novel.longDescription?.length ?: 0) + 2, novelDescription.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.contentNovelDetails.novelDetailsDescription.applyFont(assets).text = ss2
            binding.contentNovelDetails.novelDetailsDescription.movementMethod = LinkMovementMethod.getInstance()
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
