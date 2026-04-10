//TODO: DUPLICATE - REMOVAL NEEDED
// This file duplicates fragment/SearchFragment.kt which also uses Compose UI.
// Both implement the same search flow (search history, novel search, ViewPager tabs).
// SearchFragment.kt is the canonical version (used by NavDrawerActivity).
// Remove this file once SearchFragment.kt is confirmed as the sole search entry point.

package io.github.gmathi.novellibrary.compose.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.GravityCompat
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.fragment.BaseFragment
import io.github.gmathi.novellibrary.util.analytics.FAC
import io.github.gmathi.novellibrary.util.lang.addToNovelSearchHistory
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard

/**
 * Search fragment using Compose UI
 * Delegates all UI rendering to SearchScreenUI
 */
class SearchFragmentCompose : BaseFragment() {

    private var searchMode: Boolean = false
    private var searchTerm: String? = null

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
                NovelLibraryTheme {
                    SearchScreen(
                        searchHistory = dataCenter.loadNovelSearchHistory(),
                        onHomeClick = {
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
                        viewModelStoreOwner = this@SearchFragmentCompose
                    )
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
    }

    private fun setViewPager() {
        searchTerm = null
        searchMode = false
    }

    private fun searchNovels(searchTerm: String) {
        searchMode = true
        this.searchTerm = searchTerm
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
