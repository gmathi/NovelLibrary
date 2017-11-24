package io.github.gmathi.novellibrary.fragment

import android.animation.Animator
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.*
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.SimpleAnimationListener
import io.github.gmathi.novellibrary.util.SuggestionsBuilder
import io.github.gmathi.novellibrary.util.addToSearchHistory
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import kotlinx.android.synthetic.main.fragment_search.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class SearchFragment : BaseFragment() {

    lateinit var adapter: GenericAdapter<Novel>
    var searchMode: Boolean = false
    private var searchTerm: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setSearchView()

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("searchTerm"))
                searchTerm = savedInstanceState.getString("searchTerm")
            if (savedInstanceState.containsKey("searchMode"))
                searchMode = savedInstanceState.getBoolean("searchMode")
        }

        if (searchMode && searchTerm != null)
            searchNovels(searchTerm!!)
        else
            setViewPager()
    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchTerm = null
        searchMode = false
        val titles = resources.getStringArray(R.array.search_tab_titles)
        val navPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, NavPageListener())
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = navPageAdapter
        tabStrip.setViewPager(viewPager)
    }

    private fun setSearchView() {
        //searchView.setHomeButtonVisibility(View.GONE)
        searchView.setHomeButtonListener {
            hideSoftKeyboard()
            activity?.drawerLayout?.openDrawer(GravityCompat.START)
        }
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
                searchTerm?.addToSearchHistory()
                if (searchTerm != null) {
                    searchNovels(searchTerm)
                } else {
                    // Throw a empty search
                }
            }

            override fun onSearchEditOpened() {
                searchViewBgTint.visibility = View.VISIBLE
                searchViewBgTint
                    .animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(SimpleAnimationListener())
                    .start()
            }

            override fun onSearchEditClosed() {
                searchViewBgTint
                    .animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            searchViewBgTint.visibility = View.GONE
                        }
                    })
                    .start()
            }

            override fun onSearchExit() {
                if (searchMode)
                    setViewPager()

            }

            override fun onSearchCleared() {
                //Toast.makeText(context, "onSearchCleared", Toast.LENGTH_SHORT).show()
            }

            override fun onSearchTermChanged(searchTerm: String?) {
                //Toast.makeText(context, "Search Exited", Toast.LENGTH_SHORT).show()
            }

            override fun onSearchEditBackPressed(): Boolean {
                //Toast.makeText(context, "onSearchEditBackPressed", Toast.LENGTH_SHORT).show()
                if (searchView.searchOpen) {
                    searchView.closeSearch()
                    return true
                }
                return false
            }
        })
    }


    private fun searchNovels(searchTerm: String) {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()
        searchMode = true
        this.searchTerm = searchTerm

        val titles: Array<out String>
        val searchPageAdapter:GenericFragmentStatePagerAdapter
        if (dataCenter.lockRoyalRoad) {
             titles = resources.getStringArray(R.array.search_results_tab_titles)
             searchPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, SearchResultsListener(searchTerm))
        } else {
            titles = resources.getStringArray(R.array.search_results_tab_titles_unlocked)
            searchPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, SearchResultsUnlockedListener(searchTerm))
        }

        viewPager.offscreenPageLimit = 2
        viewPager.adapter = searchPageAdapter
        tabStrip.setViewPager(viewPager)
    }

    fun closeSearch() {
        searchView.closeSearch()
        setViewPager()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("searchMode", searchMode)
        if (searchTerm != null) outState.putString("searchTerm", searchTerm)
    }

}
