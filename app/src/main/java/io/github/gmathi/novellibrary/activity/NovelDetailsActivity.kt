package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.ActivityNovelDetailsBinding
import io.github.gmathi.novellibrary.databinding.ContentNovelDetailsBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.system.getParcelableExtraCompat
import io.github.gmathi.novellibrary.util.view.*
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsEvent
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsUiState
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsViewModel
import kotlinx.coroutines.launch


class NovelDetailsActivity : BaseActivity(), TextViewLinkHandler.OnClickListener {

    companion object {
        const val TAG = "NovelDetailsActivity"
    }

    private val viewModel: NovelDetailsViewModel by viewModels()

    private lateinit var binding: ActivityNovelDetailsBinding
    private lateinit var contentBinding: ContentNovelDetailsBinding

    private val cloudflareResolverLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onCloudflareResolved()
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentBinding = binding.contentNovelDetails

        val novel = intent.getParcelableExtraCompat<Novel>("novel") as Novel

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = novel.name

        viewModel.init(novel)

        contentBinding.swipeRefreshLayout.setOnRefreshListener { viewModel.refresh() }

        observeState()
        observeEvents()
    }

    // ------------------------------------------------------------------
    // State observation
    // ------------------------------------------------------------------

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    contentBinding.swipeRefreshLayout.isRefreshing = false
                    when (state) {
                        is NovelDetailsUiState.Loading -> {
                            contentBinding.progressLayout.showLoading()
                        }

                        is NovelDetailsUiState.Success -> {
                            setupViews(state.novel)
                            contentBinding.progressLayout.showContent()
                        }

                        is NovelDetailsUiState.NoInternet -> {
                            contentBinding.progressLayout.noInternetError(
                                View.OnClickListener { viewModel.refresh() }
                            )
                        }

                        is NovelDetailsUiState.Error -> {
                            contentBinding.progressLayout.showError(
                                errorText = getString(R.string.failed_to_load_url),
                                buttonText = getString(R.string.try_again),
                                onClickListener = View.OnClickListener { viewModel.refresh() }
                            )
                        }

                        is NovelDetailsUiState.MissingSource -> {
                            contentBinding.progressLayout.showError(
                                errorText = getString(R.string.missing_source_id_error),
                                buttonText = getString(R.string.delete_novel),
                                onClickListener = View.OnClickListener {
                                    performDeleteNovel()
                                    finish()
                                }
                            )
                        }

                        is NovelDetailsUiState.CloudflareChallenge -> {
                            showCloudflareResolverDialog()
                        }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is NovelDetailsEvent.ShowToast -> toast(event.messageResId)

                        is NovelDetailsEvent.CopyErrorToClipboard -> {
                            if (!isDestroyed && !isFinishing) {
                                Utils.copyErrorToClipboard(event.exception, this@NovelDetailsActivity)
                            }
                        }

                        is NovelDetailsEvent.NovelAddedToLibrary -> {
                            addNewNovel(viewModel.novel)
                            viewModel.onNovelAddedToLibrary(viewModel.novel)
                            setupAddToLibraryButton(viewModel.novel)
                        }

                        is NovelDetailsEvent.NovelDeleted -> {
                            performDeleteNovel()
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // UI setup — pure view binding, no business logic
    // ------------------------------------------------------------------

    @SuppressLint("SetTextI18n")
    private fun setupViews(novel: Novel) {
        setupNovelImage(novel)

        contentBinding.novelDetailsName.applyFont(assets).text = novel.name
        contentBinding.novelDetailsName.isSelected = dataCenter.enableScrollingText

        val nameClickListener = View.OnClickListener {
            MaterialDialog(this).show {
                title(text = getString(R.string.novel_name))
                message(text = novel.name)
                positiveButton(text = getString(R.string.copy)) {
                    copyToClipboard(getString(R.string.novel_name), novel.name)
                }
                lifecycleOwner(this@NovelDetailsActivity)
            }
        }
        contentBinding.novelDetailsName.setOnClickListener(nameClickListener)
        contentBinding.novelDetailsName.setOnLongClickListener {
            copyToClipboard(getString(R.string.novel_name), novel.name)
            true
        }
        contentBinding.novelDetailsNameInfo.setOnClickListener(nameClickListener)

        setupNovelAuthor(novel)

        contentBinding.novelDetailsStatus.applyFont(assets).text =
            novel.metadata["Year"] ?: "N/A"

        setupLicensingInfo(novel)
        setupNovelRating(novel)
        setupAddToLibraryButton(novel)
        setupNovelGenres(novel)
        setupNovelDescription(novel)

        val chaptersCountText = novel.chaptersCount.takeIf { it > 0L }?.toString()
            ?: novel.metadata["Chapters"]?.takeIf { it.isNotBlank() }
        contentBinding.novelDetailsChapters.text = if (chaptersCountText != null)
            getString(R.string.chapters) + " ($chaptersCountText)"
        else
            getString(R.string.chapters)

        contentBinding.novelDetailsChaptersLayout.setOnClickListener {
            startChaptersActivity(novel, false)
        }
        contentBinding.novelDetailsMetadataLayout.setOnClickListener { startMetadataActivity(novel) }
        contentBinding.openInBrowserButton.setOnClickListener { openInBrowser(novel.url) }
    }

    private fun setupNovelImage(novel: Novel) {
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(novel.imageUrl?.getGlideUrl()).into(contentBinding.novelDetailsImage)
            contentBinding.novelDetailsImage.setOnClickListener {
                startImagePreviewActivity(novel.imageUrl, novel.imageFilePath, contentBinding.novelDetailsImage)
            }
        }
    }

    private fun setupNovelAuthor(novel: Novel) {
        val authorText: CharSequence
        val author = novel.metadata["Author(s)"]
        if (author != null) {
            authorText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(author, Html.FROM_HTML_MODE_LEGACY)
            else
                @Suppress("DEPRECATION") Html.fromHtml(author)
        } else {
            val authors = novel.authors?.joinToString { "," } ?: return
            authorText = authors
        }

        contentBinding.novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
        contentBinding.novelDetailsAuthor.applyFont(assets).text = authorText

        // textIsSelectable is overridden by TextViewLinkHandler's MovementMethod,
        // so we use long-press-to-copy instead for reliable copy support.
        contentBinding.novelDetailsAuthor.setOnLongClickListener {
            copyToClipboard("Author", authorText.toString())
            true
        }
    }

    private fun setupLicensingInfo(novel: Novel) {
        var publisher = novel.metadata["English Publisher"] ?: ""
        val isLicensed = novel.metadata["Licensed (in English)"] == "Yes"
        if (publisher != "" || isLicensed) {
            if (publisher.isEmpty()) publisher = "an unknown publisher"
            val warningLabel = getString(R.string.licensed_warning, publisher)
            contentBinding.novelDetailsLicensedAlert.movementMethod = TextViewLinkHandler(this)
            contentBinding.novelDetailsLicensedAlert.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(warningLabel, Html.FROM_HTML_MODE_LEGACY)
                else
                    @Suppress("DEPRECATION") Html.fromHtml(warningLabel)
            contentBinding.novelDetailsLicensedLayout.visibility = View.VISIBLE
        } else {
            contentBinding.novelDetailsLicensedLayout.visibility = View.GONE
        }
    }

    private fun setupNovelRating(novel: Novel) {
        if (!novel.rating.isNullOrBlank()) {
            var ratingText = "(N/A)"
            try {
                val rating = novel.rating!!.replace(",", ".").toFloat()
                contentBinding.novelDetailsRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                io.github.gmathi.novellibrary.util.logging.Logs.warning(
                    TAG, "Rating: ${novel.rating}, Novel: ${novel.name}", e
                )
            }
            contentBinding.novelDetailsRatingText.text = ratingText
        }
    }

    private fun setupAddToLibraryButton(novel: Novel) {
        if (novel.id == -1L) {
            contentBinding.novelDetailAddToLibraryButton.setText(getString(R.string.add_to_library))
            contentBinding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_library_add_white_vector)
            contentBinding.novelDetailAddToLibraryButton.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.transparent)
            )
            contentBinding.novelDetailAddToLibraryButton.isClickable = true
            contentBinding.novelDetailAddToLibraryButton.setOnClickListener {
                viewModel.addToLibrary()
            }
        } else {
            invalidateOptionsMenu()
            contentBinding.novelDetailAddToLibraryButton.setText(getString(R.string.in_library))
            contentBinding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_local_library_white_vector)
            contentBinding.novelDetailAddToLibraryButton.setBackgroundColor(
                ContextCompat.getColor(this, R.color.Green)
            )
            contentBinding.novelDetailAddToLibraryButton.isClickable = false
        }
    }

    private fun setupNovelGenres(novel: Novel) {
        contentBinding.novelDetailsGenresLayout.removeAllViews()
        val genres = novel.genres
        if (!genres.isNullOrEmpty()) {
            genres.forEach { contentBinding.novelDetailsGenresLayout.addView(buildGenreChip(it)) }
        } else {
            contentBinding.novelDetailsGenresLayout.addView(buildGenreChip("N/A"))
        }
    }

    private fun buildGenreChip(genre: String): TextView {
        val textView = TextView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(4, 8, 20, 4)
        textView.layoutParams = layoutParams
        textView.setPadding(8, 8, 8, 8)
        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.LightGoldenrodYellow))
        textView.applyFont(assets).text = genre
        textView.setTextColor(ContextCompat.getColor(this, R.color.black))
        return textView
    }

    private fun setupNovelDescription(novel: Novel) {
        val description = novel.longDescription ?: return

        if (description.length <= 300) {
            // Short enough to show in full — textIsSelectable from XML handles copy
            contentBinding.novelDetailsDescription.applyFont(assets).text = description
            contentBinding.novelDetailsDescription.movementMethod = null
            return
        }

        val prefix = description.subSequence(0, 300)
        val suffix = "… Expand"
        val truncated = "$prefix$suffix"

        val expandClickable = object : ClickableSpan() {
            override fun onClick(textView: View) {
                contentBinding.novelDetailsDescription.applyFont(assets).text = description
                // Remove LinkMovementMethod so textIsSelectable works for copy after expansion
                contentBinding.novelDetailsDescription.movementMethod = null
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }

        val spannable = SpannableString(truncated)
        spannable.setSpan(
            expandClickable,
            prefix.length + 2, // skip the "… " before "Expand"
            truncated.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        contentBinding.novelDetailsDescription.applyFont(assets).text = spannable
        contentBinding.novelDetailsDescription.movementMethod = LinkMovementMethod.getInstance()
    }

    // ------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------

    private fun showCloudflareResolverDialog() {
        if (isDestroyed || isFinishing) return
        MaterialDialog(this).show {
            lifecycleOwner(this@NovelDetailsActivity)
            title(text = getString(R.string.cloudflare_verification_required))
            message(text = getString(R.string.cloudflare_verification_message))
            positiveButton(text = getString(R.string.resolve_manually)) {
                val url = "https://${HostNames.NOVEL_UPDATES}"
                val intent = CloudflareResolverActivity.createIntent(this@NovelDetailsActivity, url)
                cloudflareResolverLauncher.launch(intent)
            }
            negativeButton(text = getString(R.string.retry)) {
                viewModel.refresh()
            }
        }
    }

    private fun confirmNovelDelete() {
        MaterialDialog(this).show {
            icon(R.drawable.ic_delete_white_vector)
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_description_novel)
            positiveButton(R.string.remove) { performDeleteNovel() }
            negativeButton(R.string.cancel)
            lifecycleOwner(this@NovelDetailsActivity)
        }
    }

    // ------------------------------------------------------------------
    // Actions that need Context (DataAccessor extensions)
    // ------------------------------------------------------------------

    private fun performDeleteNovel() {
        // Capture the id before the delete mutates the novel
        val novelId = viewModel.novel.id
        deleteNovel(viewModel.novel)
        viewModel.onNovelDeleted(novelId)
        setupAddToLibraryButton(viewModel.novel)
        invalidateOptionsMenu()
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_novel_details, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.getItem(0)?.isVisible = viewModel.isInLibrary
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_delete_novel -> confirmNovelDelete()
            R.id.action_share -> shareUrl(viewModel.novel.url)
        }
        return super.onOptionsItemSelected(item)
    }

    // ------------------------------------------------------------------
    // Activity result (legacy — chapters screen)
    // ------------------------------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.CHAPTER_ACT_REQ_CODE) {
            if (resultCode == Constants.OPEN_DOWNLOADS_RES_CODE) {
                setResult(resultCode)
                finish()
                return
            }
            // Chapters screen may have added the novel to the library — refresh from DB on IO
            viewModel.refreshFromDatabase()
            lifecycleScope.launch {
                // Wait for the DB refresh to propagate, then update the button
                // (refreshFromDatabase is fast, but we yield to let it complete)
                kotlinx.coroutines.yield()
                setupAddToLibraryButton(viewModel.novel)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // ------------------------------------------------------------------
    // Link handler callback
    // ------------------------------------------------------------------

    override fun onLinkClicked(title: String, url: String) {
        if (url.contains(HostNames.NOVEL_UPDATES)) {
            startSearchResultsActivity(title, url)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        // Android 13+ shows its own "Copied" confirmation
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            toast(R.string.copy_to_clipboard)
        }
    }
}
