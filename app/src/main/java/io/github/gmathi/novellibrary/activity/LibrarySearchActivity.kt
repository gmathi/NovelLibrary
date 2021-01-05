package io.github.gmathi.novellibrary.activity

import android.animation.Animator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.databinding.ActivityLibrarySearchBinding
import io.github.gmathi.novellibrary.databinding.ListitemLibraryBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.system.hideSoftKeyboard
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.*
import io.github.gmathi.novellibrary.util.view.SimpleAnimationListener
import io.github.gmathi.novellibrary.util.view.SuggestionsBuilder
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem

class LibrarySearchActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    private val allNovelsList: List<Novel> = dbHelper.getAllNovels()

    private var isDateSorted = false
    private var isTitleSorted = false
    
    private lateinit var binding: ActivityLibrarySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityLibrarySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSearchView()
        setRecyclerView()
    }

    private fun setSearchView() {
        //searchView.setHomeButtonVisibility(View.GONE)
        binding.searchView.setHomeButtonListener(object : PersistentSearchView.HomeButtonListener {
            override fun onHomeButtonClick() {
                hideSoftKeyboard()
                finish()
            }
        })

        binding.searchView.setSuggestionBuilder(SuggestionsBuilder(dataCenter.loadLibrarySearchHistory()))
        binding.searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(query: String?) {
                query?.addToLibrarySearchHistory()
            }

            override fun onSearchEditOpened() {
                binding.searchViewBgTint.visibility = View.VISIBLE
                binding.searchViewBgTint
                    .animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(SimpleAnimationListener())
                    .start()
            }

            override fun onSearchEditClosed() {
                binding.searchViewBgTint
                    .animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            binding.searchViewBgTint.visibility = View.GONE
                        }
                    })
                    .start()
            }

            override fun onSearchExit() {

            }

            override fun onSearchCleared() {

            }

            override fun onSearchTermChanged(term: String?) {
                searchNovels(term)
            }

            override fun onSuggestion(searchItem: SearchItem?): Boolean {
                return true
            }

            override fun onSearchEditBackPressed(): Boolean {
                //Toast.makeText(context, "onSearchEditBackPressed", Toast.LENGTH_SHORT).show()
                if (binding.searchView.searchOpen) {
                    binding.searchView.closeSearch()
                    return true
                }
                return false
            }
        })
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(allNovelsList), layoutResId = R.layout.listitem_library, listener = this, loadMoreListener = null)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
    }

    private fun searchNovels(searchTerm: String?) {
        searchTerm?.let {
            adapter.items.clear()
            adapter.addItems(allNovelsList.filter { novel -> novel.name.contains(it, ignoreCase = true) })
        }
    }

    override fun onItemClick(item: Novel, position: Int) {
        startChaptersActivity(item)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        val itemBinding = ListitemLibraryBinding.bind(itemView)
        itemBinding.novelImageView.setImageResource(android.R.color.transparent)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(item.imageUrl?.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(itemBinding.novelImageView)
        }

        itemBinding.novelTitleTextView.text = item.name
        itemBinding.novelTitleTextView.isSelected = dataCenter.enableScrollingText

        val lastRead = item.metadata[Constants.MetaDataKeys.LAST_READ_DATE] ?: "N/A"
        val lastUpdated = item.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] ?: "N/A"

        itemBinding.lastOpenedDate.text = getString(R.string.last_read_n_updated, lastRead, lastUpdated)

        itemBinding.popMenu.setOnClickListener {
            val popup = PopupMenu(this, it)
            popup.menuInflater.inflate(R.menu.menu_popup_novel, popup.menu)
            popup.menu.findItem(R.id.action_novel_remove).isVisible = false
            popup.menu.findItem(R.id.action_novel_assign_novel_section).isVisible = false

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_novel_details -> {
                        startNovelDetailsActivity(item, false)
                        true
                    }
                    else -> {
                        true
                    }
                }
            }
            popup.show()
        }

        itemBinding.readChapterImage.setOnClickListener {
            startReader(item)
        }
    }

    private fun startReader(novel: Novel) {
        if (novel.currentChapterUrl != null) {
            startReaderDBPagerActivity(novel)
        } else {
           this.let {
                MaterialDialog(this).show {
                    title(R.string.no_bookmark_found_dialog_title)
                    message(text = getString(R.string.no_bookmark_found_dialog_description, novel.name))
                    positiveButton(R.string.okay) { dialog ->
                        it.startChaptersActivity(novel, false)
                        dialog.dismiss()
                    }
                    negativeButton(R.string.cancel)

                    lifecycleOwner(this@LibrarySearchActivity)
                }
            }
        }
    }

    private fun sortNovelsByTitle() {
        if (adapter.items.isNotEmpty()) {
            val items = adapter.items
            if (!isTitleSorted)
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name })))
            else
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name }).reversed()))
            isTitleSorted = !isTitleSorted
        }
    }

    private fun sortNovelsByDate() {
        if (adapter.items.isNotEmpty()) {
            val items = adapter.items
            if (!isDateSorted)
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name })))
            else
                adapter.updateData(ArrayList(items.sortedWith(compareBy { it.name }).reversed()))
            isDateSorted = !isDateSorted
        }
    }


}
