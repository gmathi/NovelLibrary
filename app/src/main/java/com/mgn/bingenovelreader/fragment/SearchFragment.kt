package com.mgn.bingenovelreader.fragment

import android.animation.Animator
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapter.GenericAdapter
import com.mgn.bingenovelreader.adapter.GenericFragmentStatePagerAdapter
import com.mgn.bingenovelreader.extension.NavPageListener
import com.mgn.bingenovelreader.extension.SearchResultsListener
import com.mgn.bingenovelreader.extension.addToSearchHistory
import com.mgn.bingenovelreader.extension.hideSoftKeyboard
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.util.SimpleAnimationListener
import com.mgn.bingenovelreader.util.SuggestionsBuilder
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import kotlinx.android.synthetic.main.fragment_search.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class SearchFragment : BaseFragment() {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_search, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setViewPager()
        setSearchView()
    }

    private fun setViewPager() {
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
            activity.drawerLayout.openDrawer(GravityCompat.START)
        }
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
//                if (!Utils.checkNetwork(activity)) {
//                    //toast("No Active Internet! (⋋▂⋌)")
//                } else {
                searchTerm?.addToSearchHistory()
                if (searchTerm != null) {
                    searchNovels(searchTerm)
                } else {
                    // Throw a empty search
                }
//                }
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
                if (viewPager.offscreenPageLimit == 2) {
                    setViewPager()
                }
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
        val titles = resources.getStringArray(R.array.search_results_tab_titles)
        val searchPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, SearchResultsListener(searchTerm))
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = searchPageAdapter
        tabStrip.setViewPager(viewPager)
    }


}
