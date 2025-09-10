package io.github.gmathi.novellibrary.fragment

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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.FragmentNovelDetailsBinding
import io.github.gmathi.novellibrary.extensions.showError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.NovelDetailsUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.TextViewLinkHandler
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsViewModel
import kotlin.math.min

/**
 * Fragment for displaying novel details following the established architecture patterns.
 * Uses ViewBinding, Hilt injection, and Navigation Component.
 */
@AndroidEntryPoint
class NovelDetailsFragment : BaseFragment(), TextViewLinkHandler.OnClickListener {

    companion object {
        private const val TAG = "NovelDetailsFragment"
    }

    private var _binding: FragmentNovelDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NovelDetailsViewModel by viewModels()
    private val args: NovelDetailsFragmentArgs by navArgs()

    private lateinit var currentNovel: Novel

    override fun getLayoutId(): Int = R.layout.fragment_novel_details

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNovelDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get novel ID from arguments
        val novelId = args.novelId
        
        setupViews()
        observeViewModel()
        
        // Load novel details
        viewModel.loadNovelDetails(novelId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        // Setup swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshNovelDetails()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is NovelDetailsUiState.Loading -> showLoading()
                is NovelDetailsUiState.Success -> showContent(state)
                is NovelDetailsUiState.Error -> showError(state)
            }
        }
    }

    private fun showLoading() {
        binding.progressLayout.showLoading()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showContent(state: NovelDetailsUiState.Success) {
        currentNovel = state.novel
        binding.progressLayout.showContent()
        binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
        
        setupNovelDetails(state.novel, state.isInLibrary)
        activity?.invalidateOptionsMenu()
    }

    private fun showError(state: NovelDetailsUiState.Error) {
        binding.swipeRefreshLayout.isRefreshing = false
        
        if (state.canRetry) {
            binding.progressLayout.showError(
                errorText = state.message,
                buttonText = getString(R.string.try_again)
            ) {
                viewModel.retryLoading()
            }
        } else {
            binding.progressLayout.showError(
                errorText = state.message,
                buttonText = "Delete Novel"
            ) {
                viewModel.removeFromLibrary(currentNovel)
                findNavController().navigateUp()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupNovelDetails(novel: Novel, isInLibrary: Boolean) {
        setNovelImage(novel)
        setNovelName(novel)
        setNovelAuthor(novel)
        setNovelStatus(novel)
        setLicensingInfo(novel)
        setNovelRating(novel)
        setNovelAddToLibraryButton(novel, isInLibrary)
        setNovelGenre(novel)
        setNovelDescription(novel)
        setNovelChapters(novel)
        setClickListeners(novel)
    }

    private fun setNovelImage(novel: Novel) {
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(novel.imageUrl?.getGlideUrl())
                .into(binding.novelDetailsImage)
            
            binding.novelDetailsImage.setOnClickListener {
                (requireActivity() as AppCompatActivity).startImagePreviewActivity(
                    novel.imageUrl,
                    novel.imageFilePath,
                    binding.novelDetailsImage
                )
            }
        }
    }

    private fun setNovelName(novel: Novel) {
        binding.novelDetailsName.applyFont(requireActivity().assets).text = novel.name
        binding.novelDetailsName.isSelected = dataCenter.enableScrollingText

        val listener = View.OnClickListener {
            MaterialDialog(requireContext()).show {
                title(text = "Novel Name")
                message(text = novel.name)
                lifecycleOwner(this@NovelDetailsFragment)
            }
        }
        binding.novelDetailsName.setOnClickListener(listener)
        binding.novelDetailsNameInfo.setOnClickListener(listener)
    }

    private fun setNovelAuthor(novel: Novel) {
        val author = novel.metadata["Author(s)"]
        if (author != null) {
            binding.novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
            binding.novelDetailsAuthor.applyFont(requireActivity().assets).text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(author, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(author)
                }
            return
        }
        
        val authors = novel.authors?.joinToString(", ") ?: return
        binding.novelDetailsAuthor.movementMethod = TextViewLinkHandler(this)
        binding.novelDetailsAuthor.applyFont(requireActivity().assets).text = authors
    }

    private fun setNovelStatus(novel: Novel) {
        binding.novelDetailsStatus.applyFont(requireActivity().assets).text = "N/A"
        if (novel.metadata["Year"] != null) {
            binding.novelDetailsStatus.applyFont(requireActivity().assets).text = novel.metadata["Year"]
        }
    }

    private fun setLicensingInfo(novel: Novel) {
        var publisher = novel.metadata["English Publisher"] ?: ""
        val isLicensed = novel.metadata["Licensed (in English)"] == "Yes"
        
        if (publisher.isNotEmpty() || isLicensed) {
            if (publisher.isEmpty()) publisher = "an unknown publisher"
            val warningLabel = getString(R.string.licensed_warning, publisher)
            binding.novelDetailsLicensedAlert.movementMethod = TextViewLinkHandler(this)
            binding.novelDetailsLicensedAlert.text =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(warningLabel, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    Html.fromHtml(warningLabel)
                }
            binding.novelDetailsLicensedLayout.visibility = View.VISIBLE
        } else {
            binding.novelDetailsLicensedLayout.visibility = View.GONE
        }
    }

    private fun setNovelRating(novel: Novel) {
        if (!novel.rating.isNullOrBlank()) {
            var ratingText = "(N/A)"
            try {
                val rating = novel.rating!!.replace(",", ".").toFloat()
                binding.novelDetailsRatingBar.rating = rating
                ratingText = "(${String.format("%.1f", rating)})"
            } catch (e: Exception) {
                Logs.warning(TAG, "Rating: ${novel.rating}, Novel: ${novel.name}", e)
            }
            binding.novelDetailsRatingText.text = ratingText
        }
    }

    private fun setNovelAddToLibraryButton(novel: Novel, isInLibrary: Boolean) {
        if (isInLibrary) {
            disableAddToLibraryButton()
        } else {
            resetAddToLibraryButton()
            binding.novelDetailAddToLibraryButton.setOnClickListener {
                disableAddToLibraryButton()
                viewModel.addToLibrary(novel)
            }
        }
    }

    private fun resetAddToLibraryButton() {
        binding.novelDetailAddToLibraryButton.setText(getString(R.string.add_to_library))
        binding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_library_add_white_vector)
        binding.novelDetailAddToLibraryButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
        binding.novelDetailAddToLibraryButton.isClickable = true
    }

    private fun disableAddToLibraryButton() {
        binding.novelDetailAddToLibraryButton.setText(getString(R.string.in_library))
        binding.novelDetailAddToLibraryButton.setIconResource(R.drawable.ic_local_library_white_vector)
        binding.novelDetailAddToLibraryButton.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.Green)
        )
        binding.novelDetailAddToLibraryButton.isClickable = false
    }

    private fun setNovelGenre(novel: Novel) {
        binding.novelDetailsGenresLayout.removeAllViews()
        if (novel.genres != null && novel.genres!!.isNotEmpty()) {
            novel.genres!!.forEach { genre ->
                binding.novelDetailsGenresLayout.addView(getGenreTextView(genre))
            }
        } else {
            binding.novelDetailsGenresLayout.addView(getGenreTextView("N/A"))
        }
    }

    private fun getGenreTextView(genre: String): TextView {
        val textView = TextView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(4, 8, 20, 4)
        textView.layoutParams = layoutParams
        textView.setPadding(8, 8, 8, 8)
        textView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.LightGoldenrodYellow))
        textView.applyFont(requireActivity().assets).text = genre
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        return textView
    }

    private fun setNovelDescription(novel: Novel) {
        if (novel.longDescription != null) {
            val expandClickable = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    binding.novelDetailsDescription.applyFont(requireActivity().assets).text = novel.longDescription
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            val maxLength = min(300, novel.longDescription?.length ?: 0)
            val novelDescription = "${novel.longDescription?.subSequence(0, maxLength)}… Expand"
            val ss2 = SpannableString(novelDescription)
            ss2.setSpan(
                expandClickable,
                maxLength + 2,
                novelDescription.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.novelDetailsDescription.applyFont(requireActivity().assets).text = ss2
            binding.novelDetailsDescription.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun setNovelChapters(novel: Novel) {
        binding.novelDetailsChapters.text = getString(R.string.chapters) + " (${novel.chaptersCount})"
    }

    private fun setClickListeners(novel: Novel) {
        binding.novelDetailsChaptersLayout.setOnClickListener {
            if (novel.chaptersCount != 0L) {
                // Navigate to chapters using Navigation Component
                try {
                    val action = NovelDetailsFragmentDirections.actionNovelDetailsToChapters(novel.id)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Logs.error(TAG, "Error navigating to chapters", e)
                    activity?.showAlertDialog(message = "Navigation error: ${e.message}")
                }
            }
        }

        binding.novelDetailsMetadataLayout.setOnClickListener {
            // Navigate to metadata using Navigation Component
            try {
                val action = NovelDetailsFragmentDirections.actionNovelDetailsToMetadata(novel)
                findNavController().navigate(action)
            } catch (e: Exception) {
                Logs.error(TAG, "Error navigating to metadata", e)
                activity?.showAlertDialog(message = "Navigation error: ${e.message}")
            }
        }

        binding.openInBrowserButton.setOnClickListener {
            requireContext().openInBrowser(novel.url)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_novel_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_delete_novel)?.isVisible = currentNovel.id != -1L
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_novel -> {
                confirmNovelDelete()
                true
            }
            R.id.action_share -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, currentNovel.url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Novel"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmNovelDelete() {
        MaterialDialog(requireContext()).show {
            icon(R.drawable.ic_delete_white_vector)
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_description_novel)
            positiveButton(R.string.remove) {
                viewModel.removeFromLibrary(currentNovel)
                findNavController().navigateUp()
            }
            negativeButton(R.string.cancel)
            lifecycleOwner(this@NovelDetailsFragment)
        }
    }

    override fun onLinkClicked(title: String, url: String) {
        (requireActivity() as AppCompatActivity).startSearchResultsActivity(title, url)
    }
}