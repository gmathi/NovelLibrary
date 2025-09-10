package io.github.gmathi.novellibrary.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.LibraryPageListener
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.databinding.FragmentLibraryPagerBinding
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.util.Constants


@AndroidEntryPoint
class LibraryPagerFragment : BaseFragment() {

    private val novelSections: ArrayList<NovelSection> = ArrayList()

    private var binding: FragmentLibraryPagerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_library_pager, container, false) ?: return null
        binding = FragmentLibraryPagerBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            // Remove toolbar setup as MainActivity handles the toolbar in single activity architecture
            toolbar.visibility = View.GONE
            setViewPager()
            contentLibraryPager.novelSectionSettings.setOnClickListener {
                // Navigate to novel sections using Navigation Component
                navigateToNovelSections()
            }
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
        binding?.apply {
            contentLibraryPager.apply {
                viewPager.apply {
                    offscreenPageLimit = 3
                    adapter = navPageAdapter
                }
                tabStrip.setViewPager(viewPager)
            }
        }
    }

    fun getLibraryFragment(): LibraryFragment? {
        val viewPager = (binding?.contentLibraryPager?.viewPager?.adapter as? GenericFragmentStatePagerAdapter) ?: return null
        val listener = (viewPager.listener as? LibraryPageListener) ?: return null
        val currentItem = binding?.contentLibraryPager?.viewPager?.currentItem ?: return null
        return listener.getCurrentFragment(currentItem) as? LibraryFragment
    }

    /**
     * Navigate to novel sections using Navigation Component
     */
    private fun navigateToNovelSections() {
        try {
            // For now, show a dialog indicating this will be implemented as a fragment
            // In the future, this should navigate to a NovelSectionsFragment
            activity?.let { activity ->
                androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Novel Sections")
                    .setMessage("Novel sections management will be available in a future update as part of the single activity architecture.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        } catch (e: Exception) {
            // Log error but don't crash
            activity?.let { activity ->
                androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle("Error")
                    .setMessage("Navigation error: ${e.message}")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    /**
     * Refresh the view pager when novel sections change
     * This method can be called from other parts of the app when sections are updated
     */
    fun refreshViewPager() {
        setViewPager()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the view pager in case novel sections were changed
        // This replaces the onActivityResult functionality
        refreshViewPager()
    }

}
