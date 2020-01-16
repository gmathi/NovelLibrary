package io.github.gmathi.novellibrary.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.util.*


class GenericFragmentStatePagerAdapter(manager: FragmentManager, private val titles: Array<String>?, private val pagerCount: Int, val listener: Listener) : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment = listener.getFragmentForItem(position)

    override fun getCount(): Int = pagerCount

    override fun getPageTitle(position: Int): CharSequence? = if (titles != null) titles[position].toUpperCase(Locale.getDefault()) else super.getPageTitle(position)

    interface Listener {
        fun getFragmentForItem(position: Int): Fragment
    }

}