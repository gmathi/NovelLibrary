package io.github.gmathi.novellibrary.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.github.gmathi.novellibrary.fragment.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        return when (position) {
            0 -> SearchUrlFragment.newInstance("popmonth")
            1 -> SearchUrlFragment.newInstance("popular")
            else -> SearchUrlFragment.newInstance("sixmonths")
        }
    }
}

class SearchResultsListener(private val searchTerms: String, private val tabNames: ArrayList<String>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {

        //This should not be triggered, but just in case
        if (position >= tabNames.size) return SearchTermFragment.newInstance(searchTerms, Constants.SourceId.WLN_UPDATES)

        //This returns the expected sourceId fragment for the matching titles
        return when (tabNames[position]) {
            "Novel-Updates" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.NOVEL_UPDATES)
            "RoyalRoad" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.ROYAL_ROAD)
            "NovelFull" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.NOVEL_FULL)
            "ScribbleHub" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.SCRIBBLE_HUB)
            "LNMTL" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.LNMTL)
            "Neovel" -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.NEOVEL)
            else -> SearchTermFragment.newInstance(searchTerms, Constants.SourceId.WLN_UPDATES)
        }
    }
}


class WebPageFragmentPageListener(val novel: Novel, private val webPages: List<WebPage>) : GenericFragmentStatePagerAdapter.Listener {

    override fun getFragmentForItem(position: Int): Fragment {
        return WebPageDBFragment.newInstance(novel.id, webPages[position])
    }
}

class LibraryPageListener(private val novelSections: ArrayList<NovelSection>) : GenericFragmentStatePagerAdapter.Listener {
    private var currentFragment = HashMap<Int, Fragment>()
    
    fun getCurrentFragment(position: Int) = currentFragment[position]

    override fun getFragmentForItem(position: Int): Fragment {
        val result = LibraryFragment.newInstance(novelSections[position].id)
        currentFragment[position] = result
        return result
    }
}

class ChaptersPageListener(private val novel: Novel, private val translatorSourceNames: ArrayList<String>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        return ChaptersFragment.newInstance(novel, translatorSourceNames[position])
    }
}
//endregion



