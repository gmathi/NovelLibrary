package io.github.gmathi.novellibrary.adapter

import androidx.fragment.app.Fragment
import io.github.gmathi.novellibrary.fragment.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.util.Constants

//region Fragment Page Listeners

//endregion

//region Fragment State Page Listeners

class NavPageListener : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        return when (position) {
            0 -> SearchUrlFragment.newInstance("popmonth", null)
            1 -> SearchUrlFragment.newInstance("popular", null)
            else -> SearchUrlFragment.newInstance("sixmonths", null)
        }
    }
}

class SearchResultsListener(private val searchTerms: String, private val sources: List<HttpSource>) : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {

        //This should not be triggered, but just in case if there is an edge case bug
        if (position >= sources.size) return SearchTermFragment.newInstance(searchTerms, Constants.SourceId.WLN_UPDATES)

        return SearchTermFragment.newInstance(searchTerms, sources[position].id)
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

class RecentNovelsPageListener() : GenericFragmentStatePagerAdapter.Listener {
    override fun getFragmentForItem(position: Int): Fragment {
        if (position == 0)
            return RecentlyUpdatedNovelsFragment.newInstance()
        return RecentlyViewedNovelsFragment.newInstance()
    }
}
//endregion



