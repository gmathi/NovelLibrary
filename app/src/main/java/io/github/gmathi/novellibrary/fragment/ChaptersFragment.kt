package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.databinding.FragmentSourceChaptersBinding
import io.github.gmathi.novellibrary.databinding.ListitemChapterBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startWebViewActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import io.github.gmathi.novellibrary.viewmodel.ChaptersFragmentViewModel

class ChaptersFragment : BaseFragment(),
    GenericAdapterSelectTitleProvider.Listener<WebPage>, CompoundButton.OnCheckedChangeListener {

    companion object {
        private const val NOVEL = "novel"
        private const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"

        fun newInstance(novel: Novel, translatorSourceName: String): ChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(NOVEL, novel)
            bundle.putString(TRANSLATOR_SOURCE_NAME, translatorSourceName)
            val fragment = ChaptersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var novel: Novel
    private lateinit var translatorSourceName: String
    private lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>
    private lateinit var binding: FragmentSourceChaptersBinding

    // ViewModel
    private val viewModel: ChaptersFragmentViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = inflater.inflate(R.layout.fragment_source_chapters, container, false)
            ?: return null
        binding = FragmentSourceChaptersBinding.bind(result)
        return result
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        
        // Get arguments
        novel = requireArguments().getParcelable<Novel>(NOVEL) as Novel
        translatorSourceName = requireArguments().getString(TRANSLATOR_SOURCE_NAME)
            ?: ALL_TRANSLATOR_SOURCES

        // Get parent ViewModel
        val parentViewModel = (activity as? ChaptersPagerActivity)?.vm

        // Initialize ViewModel
        viewModel.init(novel, translatorSourceName, this, requireContext(), parentViewModel)
        
        // Setup UI
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = GenericAdapterSelectTitleProvider(
            items = ArrayList(), 
            layoutResId = R.layout.listitem_chapter, 
            listener = this
        )
        
        binding.recyclerView.apply {
            isVerticalScrollBarEnabled = true
            setDefaultsNoAnimation(adapter)
            context?.let { 
                addItemDecoration(CustomDividerItemDecoration(it, DividerItemDecoration.VERTICAL)) 
            }
        }
        
        binding.swipeRefreshLayout.isEnabled = false
    }

    private fun setupObservers() {
        // Observe UI state changes
        viewModel.uiState.observe(viewLifecycleOwner, Observer { uiState ->
            when (uiState) {
                is ChaptersFragmentViewModel.ChaptersUiState.Loading -> {
                    binding.progressLayout.showLoading()
                }
                is ChaptersFragmentViewModel.ChaptersUiState.Success -> {
                    binding.progressLayout.showContent()
                }
                is ChaptersFragmentViewModel.ChaptersUiState.Empty -> {
                    binding.progressLayout.showEmpty(emptyText = "No Chapters Found!")
                }
                is ChaptersFragmentViewModel.ChaptersUiState.NoInternet -> {
                    binding.progressLayout.noInternetError {
                        viewModel.loadChapters()
                    }
                }
                is ChaptersFragmentViewModel.ChaptersUiState.Error -> {
                    binding.progressLayout.showError(
                        errorText = uiState.message,
                        buttonText = getString(R.string.try_again),
                        onClickListener = View.OnClickListener {
                            viewModel.loadChapters()
                        }
                    )
                }
            }
        })

        // Observe chapters data
        viewModel.chapters.observe(viewLifecycleOwner, Observer { chapters ->
            if (chapters.isNotEmpty()) {
                adapter.updateData(ArrayList(chapters))
            }
        })

        // Observe scroll position
        viewModel.scrollToPosition.observe(viewLifecycleOwner, Observer { position ->
            if (position != -1) {
                binding.recyclerView.scrollToPosition(position)
            }
        })

        // Observe selected chapters
        viewModel.selectedChapters.observe(viewLifecycleOwner, Observer { selectedChapters ->
            // Update adapter to reflect selection changes
            adapter.notifyDataSetChanged()
        })

        // Observe action mode progress
        viewModel.actionModeProgress.observe(viewLifecycleOwner, Observer { progress ->
            // Handle progress updates if needed
        })
    }

    //region Adapter Listener Methods

    override fun getSectionTitle(position: Int): String {
        return adapter.items[position].chapterName
    }

    override fun onItemClick(item: WebPage, position: Int) {
        if (novel.id != -1L) {
            updateNovelBookmark(novel, item)
            startReaderDBPagerActivity(novel, translatorSourceName)
        } else {
            startWebViewActivity(item.url)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {
        val itemBinding = ListitemChapterBinding.bind(itemView)

        // Get chapter settings from ViewModel
        val webPageSettings = viewModel.getChapterSettings(item)

        // Set offline availability indicator
        if (webPageSettings?.filePath != null) {
            itemBinding.availableOfflineImageView.visibility = View.VISIBLE
            itemBinding.availableOfflineImageView.animation = null
        } else {
            itemBinding.availableOfflineImageView.visibility = View.GONE
        }

        // Set read status indicator
        itemBinding.isReadView.visibility = if (webPageSettings?.isRead == true) View.VISIBLE else View.GONE
        
        // Set bookmark indicator
        itemBinding.bookmarkView.visibility = if (item.url == novel.currentChapterUrl) View.VISIBLE else View.GONE

        // Set chapter title
        itemBinding.chapterTitle.text = item.chapterName

        // Update title with custom title if available
        webPageSettings?.title?.let { customTitle ->
            if (customTitle.contains(item.chapterName)) {
                itemBinding.chapterTitle.text = customTitle
            } else {
                itemBinding.chapterTitle.text = "${item.chapterName}: $customTitle"
            }
        }

        // Setup checkbox
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(null)
        itemBinding.chapterCheckBox.isChecked = viewModel.isChapterSelected(item)
        itemBinding.chapterCheckBox.tag = item
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(this@ChaptersFragment)

        // Setup long click listener
        itemView.setOnLongClickListener {
            viewModel.onChapterLongClicked(item)
            true
        }

        // Set favorite indicator
        if (webPageSettings?.metadata?.get(Constants.MetaDataKeys.IS_FAVORITE)?.toBoolean() == true) {
            itemBinding.favoriteView.visibility = View.VISIBLE
        } else {
            itemBinding.favoriteView.visibility = View.GONE
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return
        val chaptersPagerActivity = (activity as? ChaptersPagerActivity) ?: return

        // If Novel is not in Library
        if (novel.id == -1L) {
            if (isChecked) {
                chaptersPagerActivity.confirmDialog(
                    getString(R.string.add_to_library_dialog_content, novel.name),
                    { dialog ->
                        chaptersPagerActivity.vm.addNovelToLibrary()
                        chaptersPagerActivity.invalidateOptionsMenu()
                        chaptersPagerActivity.addToDataSet(webPage)
                        dialog.dismiss()
                    },
                    { dialog ->
                        chaptersPagerActivity.removeFromDataSet(webPage)
                        adapter.notifyItemChanged(adapter.items.indexOf(webPage))
                        dialog.dismiss()
                    }
                )
            }
        } else {
            // If Novel is already in library
            if (isChecked) {
                chaptersPagerActivity.addToDataSet(webPage)
            } else {
                chaptersPagerActivity.removeFromDataSet(webPage)
            }
        }
    }

    //endregion

    override fun onResume() {
        super.onResume()
        // Save recycler state before any updates
        viewModel.saveRecyclerState(binding.recyclerView.layoutManager?.onSaveInstanceState())
    }

    override fun onPause() {
        super.onPause()
        // Save recycler state
        viewModel.saveRecyclerState(binding.recyclerView.layoutManager?.onSaveInstanceState())
    }
}