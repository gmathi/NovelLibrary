package io.github.gmathi.novellibrary.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.activity.NovelSectionsActivity
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.LibraryPageListener
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.databinding.FragmentLibraryPagerBinding
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.util.Constants


class LibraryPagerFragment : BaseFragment() {

    private val novelSections: ArrayList<NovelSection> = ArrayList()

    private lateinit var binding: FragmentLibraryPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_library_pager, container, false) ?: return null
        binding = FragmentLibraryPagerBinding.bind(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.toolbar.title = getString(R.string.title_library)
        (activity as NavDrawerActivity).setToolbar(binding.toolbar)

        setViewPager()

        binding.contentLibraryPager.novelSectionSettings.setOnClickListener {
            startActivityForResult(Intent(activity, NovelSectionsActivity::class.java), Constants.NOVEL_SECTIONS_ACT_REQ_CODE)
        }
    }

    private fun setViewPager() {
        while (childFragmentManager.backStackEntryCount > 0)
            childFragmentManager.popBackStack()

        //We Manually add this because we want it to be static and the name to be change in different languages
        novelSections.clear()
        novelSections.add(NovelSection(-1L, getString(R.string.default_novel_section_name)))
        novelSections.addAll(dbHelper.getAllNovelSections())

        val titles = Array(novelSections.size, init = {
            novelSections[it].name!!
        })

        val navPageAdapter = GenericFragmentStatePagerAdapter(childFragmentManager, titles, titles.size, LibraryPageListener(novelSections))
        binding.contentLibraryPager.viewPager.offscreenPageLimit = 3
        binding.contentLibraryPager.viewPager.adapter = navPageAdapter
        binding.contentLibraryPager.tabStrip.setViewPager(binding.contentLibraryPager.viewPager)
    }

    fun getLibraryFragment(): LibraryFragment? {
        val viewPager = (binding.contentLibraryPager.viewPager.adapter as? GenericFragmentStatePagerAdapter) ?: return null
        val listener = (viewPager.listener as? LibraryPageListener) ?: return null
        return listener.getCurrentFragment(binding.contentLibraryPager.viewPager.currentItem) as? LibraryFragment
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.NOVEL_SECTIONS_ACT_REQ_CODE) {
            setViewPager()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}
