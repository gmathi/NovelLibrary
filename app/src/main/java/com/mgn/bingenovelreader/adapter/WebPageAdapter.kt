package com.mgn.bingenovelreader.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.mgn.bingenovelreader.fragment.WebPageFragment
import com.mgn.bingenovelreader.model.WebPage


/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class WebPageAdapter(fm: FragmentManager, val chapters: MutableList<WebPage>, val listener: Listener) : FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        val fragment = WebPageFragment.newInstance(chapters[position].filePath)
        fragment.listener = listener
        return fragment
    }

    override fun getCount(): Int {
        return chapters.size
    }

    interface Listener {
        fun checkUrl(url: String?)
    }
}