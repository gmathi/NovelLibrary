package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.GravityCompat
import androidx.viewpager.widget.ViewPager
import com.google.firebase.analytics.ktx.logEvent
import com.ogaclejapan.smarttablayout.SmartTabLayout
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.NavPageListener
import io.github.gmathi.novellibrary.adapter.SearchResultsListener
import io.github.gmathi.novellibrary.compose.search.*
import io.github.gmathi.novellibrary.model.source.getPreferenceKey
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.lang.addToNovelSearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard


class SearchFragment : BaseFragment() {

    var searchMode: Boolean = false
    private var searchTerm: String? = null
    private var viewPager: ViewPager? = null
    private var tabStrip: SmartTabLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SearchScreen()
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("searchTerm"))
                searchTerm = savedInstanceState.getString("searchTerm")
            if (savedInstanceState.containsKey("searchMode"))
                searchMode = savedInstanceState.getBoolean("searchMode")
        }

        if (searchMode && searchTerm != null) {
            searchNovels(searchTerm!!)
        } else {
            setViewPager()
        }
    }

    @Composable
    private fun SearchScreen() {
        val searchState = rememberPersistentSearchState(
            initialLogoText = getString(R.string.search_novel)
        )
        val searchHistory = remember { dataCenter.loadNovelSearchHistory() }
        val suggestionBuilder = remember(searchHistory) {
            HistorySearchSuggestionsBuilder(searchHistory)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search View
                PersistentSearchView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    state = searchState,
                    hint = getString(R.string.search_novel),
                    homeButtonMode = HomeButtonMode.Burger,
                    onHomeButtonClick = {
                        hideSoftKeyboard()
                        if (activity is NavDrawerActivity) {
                            (requireActivity() as NavDrawerActivity).binding.drawerLayout.openDrawer(
                                GravityCompat.START
                            )
                        }
                    },
                    onSearch = { query ->
                        query.addToNovelSearchHistory()
                        searchNovels(query)
                        firebaseAnalytics.logEvent(FAC.Event.SEARCH_NOVEL) {
                            param(FAC.Param.SEARCH_TERM, query)
                        }
                    },
                    onSearchExit = {
                        if (searchMode) {
                            setViewPager()
                        }
                    },
                    suggestionBuilder = suggestionBuilder,
                    elevation = 2
                )

                // Tab Strip
                AndroidView(
                    factory = { context ->
                        SmartTabLayout(context).apply {
                            tabStrip = this
                            val typedValue = android.util.TypedValue()
                            val height = if (context.theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
                                android.util.TypedValue.complexToDimensionPixelSize(typedValue.data, context.resources.displayMetrics)
                            } else {
                                // Fallback to 48dp if actionBarSize can't be resolved
                                (48 * context.resources.displayMetrics.density).toInt()
                            }
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                height
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // ViewPager for search results or default tabs
                AndroidView(
                    factory = { context ->
                        ViewPager(context).apply {
                            viewPager = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Background tint overlay
            SearchBackgroundTint(
                visible = searchState.isEditing,
                onClick = { searchState.closeSearch() }
            )
        }
    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchTerm = null
        searchMode = false
        
        viewPager?.let { vp ->
            val titles = resources.getStringArray(R.array.search_tab_titles)
            val navPageAdapter = GenericFragmentStatePagerAdapter(
                childFragmentManager,
                titles,
                titles.size,
                NavPageListener()
            )
            vp.offscreenPageLimit = 3
            vp.adapter = navPageAdapter
            tabStrip?.setViewPager(vp)
        }
    }

    private fun searchNovels(searchTerm: String) {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchMode = true
        this.searchTerm = searchTerm

        val sources = sourceManager.getOnlineSources().filter {
            dataCenter.isSourceEnabled(it.getPreferenceKey())
        }

        val sourceNames = sources.map { it.name }

        viewPager?.let { vp ->
            vp.offscreenPageLimit = 2
            vp.adapter = GenericFragmentStatePagerAdapter(
                childFragmentManager,
                sourceNames.toTypedArray(),
                sourceNames.size,
                SearchResultsListener(searchTerm, sources)
            )
            tabStrip?.setViewPager(vp)
        }
    }

    fun closeSearch() {
        setViewPager()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("searchMode", searchMode)
        if (searchTerm != null) outState.putString("searchTerm", searchTerm)
    }
}
