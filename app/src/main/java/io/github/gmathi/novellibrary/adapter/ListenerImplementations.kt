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
            0 -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=popmonth")
            1 -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=popular")
            2 -> SearchUrlFragment.newInstance("https://www.novelupdates.com/series-ranking/?rank=sixmonths")
            else -> null
        }
    }
}

class SearchResultsListener(private val searchTerms: String, private val tabNames: ArrayList<String>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment? {
        if (position >= tabNames.size) return null
        return when (tabNames[position]) {
            "Novel-Updates" -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_UPDATES)
            "RoyalRoad" -> SearchTermFragment.newInstance(searchTerms, HostNames.ROYAL_ROAD)
            "NovelFull" -> SearchTermFragment.newInstance(searchTerms, HostNames.NOVEL_FULL)
            "ScribbleHub" -> SearchTermFragment.newInstance(searchTerms, HostNames.SCRIBBLE_HUB)
            "WLN-Updates" -> SearchTermFragment.newInstance(searchTerms, HostNames.WLN_UPDATES)
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



