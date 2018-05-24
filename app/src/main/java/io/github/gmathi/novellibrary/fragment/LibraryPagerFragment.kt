package io.github.gmathi.novellibrary.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.activity.NovelSectionsActivity
import io.github.gmathi.novellibrary.activity.startNovelSectionsActivity
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.LibraryPageListener
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.NovelSection
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.android.synthetic.main.content_library_pager.*
import kotlinx.android.synthetic.main.fragment_library_pager.*


class LibraryPagerFragment : BaseFragment() {

    private val novelSections: ArrayList<NovelSection> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_library_pager, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbar.title = getString(R.string.title_library)
        (activity as NavDrawerActivity).setToolbar(toolbar)

        setViewPager()

        novelSectionSettings.setOnClickListener {
            startActivityForResult(Intent(activity, NovelSectionsActivity::class.java), Constants.NOVEL_SECTIONS_ACT_REQ_CODE)
        }

    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()

        //We Manually add this because we want it to be static and the name to be change in different languages
        novelSections.clear()
        novelSections.add(NovelSection(-1L, "Currently Reading"))
        novelSections.addAll(dbHelper.getAllNovelSections())

        val titles = Array(novelSections.size, init = {
            novelSections[it].name!!
        })

        val navPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, LibraryPageListener(novelSections))
        viewPager.offscreenPageLimit = 3
        viewPager.adapter = navPageAdapter
        tabStrip.setViewPager(viewPager)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
       // super.onActivityResult(requestCode, resultCode, data)
        //TODO: NOT WORKING!!!
        if (requestCode == Constants.NOVEL_SECTIONS_ACT_REQ_CODE) {
            setViewPager()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
