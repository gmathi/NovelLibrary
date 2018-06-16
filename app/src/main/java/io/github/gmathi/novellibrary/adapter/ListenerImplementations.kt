package io.github.gmathi.novellibrary.adapter

import android.support.v4.app.Fragment
import io.github.gmathi.novellibrary.fragment.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelSection
import io.github.gmathi.novellibrary.network.HostNames

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return when (position) {
            0 -> SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=popmonth")
            1 -> SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=popular")
            2 -> SearchUrlFragment.newInstance("http://www.novelupdates.com/series-ranking/?rank=sixmonths")
            3 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> null
        }
    }
}

class NavDetailsPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return when (position) {
            0 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/active-popular")
            1 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/best-rated")
            2 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            3 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            4 -> SearchUrlFragment.newInstance("https://royalroadl.com/fictions/complete")
            else -> null
        }
    }
}

class SearchResultsListener(private val searchTerms: String) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return when (position) {
            0 -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            1 -> SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
            else -> null
        }
    }
}

class SearchResultsUnlockedListener(private val searchTerms: String) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return when (position) {
            0 -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            1 -> SearchTermFragment.newInstance(searchTerms, HostNames.ROYAL_ROAD)
            2 -> SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
            else -> null
        }
    }

}

class WebPageFragmentPageListener(val novel: Novel, val sourceId: Long) : GenericFragmentStatePagerAdapter.Listener {

    override fun getFragmentForItem(position: Int): Fragment? {
        return WebPageDBFragment.newInstance(novel.id, sourceId, position)
    }
}

class LibraryPageListener(private val novelSections: ArrayList<NovelSection>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return LibraryFragment.newInstance(novelSections[position].id)
    }
}

class ChaptersPageListener(private val novel: Novel, private val sources: ArrayList<Pair<Long, String>>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        return ChaptersFragment.newInstance(novel, sources[position].first)
    }
}
//endregion



