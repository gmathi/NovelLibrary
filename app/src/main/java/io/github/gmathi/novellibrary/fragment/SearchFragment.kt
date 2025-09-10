package io.github.gmathi.novellibrary.fragment

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.logEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.NavPageListener
import io.github.gmathi.novellibrary.adapter.SearchResultsListener
import io.github.gmathi.novellibrary.databinding.FragmentSearchBinding
import io.github.gmathi.novellibrary.model.SearchUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.view.SimpleAnimationListener
import io.github.gmathi.novellibrary.util.view.SuggestionsBuilder
import io.github.gmathi.novellibrary.viewmodel.SearchViewModel
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem


@AndroidEntryPoint
class SearchFragment : BaseFragment() {

    private val viewModel: SearchViewModel by viewModels()
    
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private var searchMode: Boolean = false
    private var searchTerm: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun getLayoutId() = R.layout.fragment_search

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setSearchView()
        observeViewModel()
        
        // Restore state if needed
        if (savedInstanceState != null) {
            searchTerm = savedInstanceState.getString("searchTerm")
            searchMode = savedInstanceState.getBoolean("searchMode")
            viewModel.restoreSearchState(searchTerm, searchMode)
        } else {
            viewModel.initializeSearch()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SearchUiState.Initial -> {
                    // Initialize the search view
                    setViewPager()
                }
                is SearchUiState.Loading -> {
                    // Show loading state if needed
                }
                is SearchUiState.Success -> {
                    handleSearchSuccess(state)
                }
                is SearchUiState.Error -> {
                    handleSearchError(state.message)
                }
            }
        }
    }

    private fun handleSearchSuccess(state: SearchUiState.Success) {
        // Update search suggestions
        binding.searchView.setSuggestionBuilder(SuggestionsBuilder(ArrayList(state.searchHistory)))
        
        if (state.isSearchMode && !state.searchTerm.isNullOrBlank()) {
            searchMode = true
            searchTerm = state.searchTerm
            displaySearchResults(state.searchTerm, state.sources)
        } else {
            searchMode = false
            searchTerm = null
            setViewPager()
        }
    }

    private fun handleSearchError(message: String) {
        // Show error message to user
        activity?.let { activity ->
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Search Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchTerm = null
        searchMode = false
        val titles = resources.getStringArray(R.array.search_tab_titles)
        val navPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, NavPageListener())
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = navPageAdapter
        binding.tabStrip.setViewPager(binding.viewPager)
    }

    private fun setSearchView() {
        binding.searchView.setHomeButtonListener {
            hideSoftKeyboard()
            // In single activity architecture, we don't need to access NavDrawerActivity
            // The drawer is handled by MainActivity
            try {
                // Navigate back or open drawer through Navigation Component
                if (!findNavController().navigateUp()) {
                    activity?.onBackPressed()
                }
            } catch (e: Exception) {
                // Fallback to activity back press
                activity?.onBackPressed()
            }
        }

        binding.searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(query: String?) {
                if (!query.isNullOrBlank()) {
                    viewModel.searchNovels(query)
                    firebaseAnalytics.logEvent(FAC.Event.SEARCH_NOVEL) {
                        param(FAC.Param.SEARCH_TERM, query)
                    }
                }
            }

            override fun onSearchEditOpened() {
                binding.searchViewBgTint.visibility = View.VISIBLE
                binding.searchViewBgTint
                    .animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(SimpleAnimationListener())
                    .start()
            }

            override fun onSearchEditClosed() {
                binding.searchViewBgTint
                    .animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            binding.searchViewBgTint.visibility = View.GONE
                        }
                    })
                    .start()
            }

            override fun onSearchExit() {
                if (searchMode) {
                    viewModel.exitSearchMode()
                }
            }

            override fun onSearchCleared() {
                // Handle search cleared if needed
            }

            override fun onSearchTermChanged(term: String?) {
                // Handle search term changes if needed
            }

            override fun onSuggestion(searchItem: SearchItem?): Boolean {
                return true
            }

            override fun onSearchEditBackPressed(): Boolean {
                if (binding.searchView.searchOpen) {
                    binding.searchView.closeSearch()
                    return true
                }
                return false
            }
        })
    }


    private fun displaySearchResults(searchTerm: String, sources: List<io.github.gmathi.novellibrary.model.source.Source>) {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()

        val sourceNames = ArrayList(sources.map { it.name })

        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.adapter = GenericFragmentStatePagerAdapter(
            childFragmentManager, 
            sourceNames.toTypedArray(), 
            sourceNames.size, 
            SearchResultsListener(searchTerm, sources.filterIsInstance<io.github.gmathi.novellibrary.model.source.online.HttpSource>())
        )
        binding.tabStrip.setViewPager(binding.viewPager)
    }

    fun closeSearch() {
        binding.searchView.closeSearch()
        viewModel.exitSearchMode()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("searchMode", searchMode)
        if (searchTerm != null) outState.putString("searchTerm", searchTerm)
    }

}
