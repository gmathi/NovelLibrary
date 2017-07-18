package com.mgn.bingenovelreader.extension

import android.support.v4.app.Fragment
import com.mgn.bingenovelreader.adapter.GenericFragmentStatePagerAdapter
import com.mgn.bingenovelreader.fragment.PopularNovelsFragment
import com.mgn.bingenovelreader.fragment.SearchResultsFragment
import com.mgn.bingenovelreader.network.HostNames

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/active-popular")
            1 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/best-rated")
            2 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            3 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> return null
        }
    }
}

class NavDetailsPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/active-popular")
            1 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/best-rated")
            2 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            3 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> return PopularNovelsFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> return null
        }
    }
}

class SearchResultsListener(val searchTerms: String) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return SearchResultsFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            1 -> return SearchResultsFragment.newInstance(searchTerms, HostNames.ROYAL_ROAD)
            else -> return null
        }
    }
}

//endregion



