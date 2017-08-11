package io.github.gmathi.novellibrary.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import io.github.gmathi.novellibrary.fragment.WebPageFragment
import io.github.gmathi.novellibrary.model.WebPage
import java.io.Serializable


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class WebPageAdapter(fm: FragmentManager, val chapters: MutableList<WebPage>) : FragmentPagerAdapter(fm), Serializable {

    override fun getItem(position: Int): Fragment {
        val fragment = WebPageFragment.newInstance(chapters[position])
        return fragment
    }

    override fun getCount(): Int {
        return chapters.size
    }

    interface Listener : Serializable {
        fun checkUrl(url: String?): Boolean
    }
}