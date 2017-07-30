package io.github.gmathi.novellibrary.extension

import android.support.v4.app.Fragment
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.fragment.SearchTermFragment
import io.github.gmathi.novellibrary.fragment.SearchUrlFragment
import io.github.gmathi.novellibrary.network.HostNames

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=popmonth")
            1 -> return SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=popular")
            2 -> return SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=sixmonths")
            3 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> return null
        }
    }
}

class NavDetailsPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/active-popular")
            1 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/best-rated")
            2 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            3 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> return SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> return null
        }
    }
}

class SearchResultsListener(val searchTerms: String) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        when (position) {
            0 -> return SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            1 -> return SearchTermFragment.newInstance(searchTerms, HostNames.ROYAL_ROAD)
            2 -> return SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
            else -> return null
        }
    }
}

//endregion



