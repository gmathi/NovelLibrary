package io.github.gmathi.novellibrary.activity

import android.animation.Animator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.*
import kotlinx.android.synthetic.main.activity_library_search.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_library.view.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem

class LibrarySearchActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    private val allNovelsList: List<Novel> = dbHelper.getAllNovels()

    private var isDateSorted = false
    private var isTitleSorted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_search)
        setSearchView()
        setRecyclerView()
    }

    private fun setSearchView() {
        //searchView.setHomeButtonVisibility(View.GONE)
        searchView.setHomeButtonListener(object : PersistentSearchView.HomeButtonListener {
            override fun onHomeButtonClick() {
                hideSoftKeyboard()
                finish()
            }
        })

        searchView.setSuggestionBuilder(SuggestionsBuilder(dataCenter.loadLibrarySearchHistory()))
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(query: String?) {
                query?.addToLibrarySearchHistory()
            }

            override fun onSearchEditOpened() {
                searchViewBgTint.visibility = View.VISIBLE
                searchViewBgTint
                    .animate()
                    .alpha(1.0f)
                    .setDuration(300)
                    .setListener(SimpleAnimationListener())
                    .start()
            }

            override fun onSearchEditClosed() {
                searchViewBgTint
                    .animate()
                    .alpha(0.0f)
                    .setDuration(300)
                    .setListener(object : SimpleAnimationListener() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            searchViewBgTint.visibility = View.GONE
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
                if (searchView.searchOpen) {
                    searchView.closeSearch()
                    return true
                }
                return false
            }
        })
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(allNovelsList), layoutResId = R.layout.listitem_library, listener = this, loadMoreListener = null)
        recyclerView.setDefaults(adapter)
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
        itemView.novelImageView.setImageResource(android.R.color.transparent)

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(item.imageUrl?.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.novelImageView)
        }

        itemView.novelTitleTextView.text = item.name
        itemView.novelTitleTextView.isSelected = true

        val lastRead = item.metaData[Constants.MetaDataKeys.LAST_READ_DATE] ?: "N/A"
        val lastUpdated = item.metaData[Constants.MetaDataKeys.LAST_UPDATED_DATE] ?: "N/A"

        itemView.lastOpenedDate.text = getString(R.string.last_read_n_updated, lastRead, lastUpdated)

        itemView.popMenu.setOnClickListener {
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

        itemView.readChapterImage.setOnClickListener {
            startReader(item)
        }
    }

    private fun startReader(novel: Novel) {
        if (novel.currentWebPageUrl != null) {
            startReaderDBPagerActivity(novel)
        } else {
            val confirmDialog = this.let {
                MaterialDialog.Builder(it)
                    .title(getString(R.string.no_bookmark_found_dialog_title))
                    .content(getString(R.string.no_bookmark_found_dialog_description, novel.name))
                    .positiveText(getString(R.string.okay))
                    .negativeText(R.string.cancel)
                    .onPositive { dialog, _ -> it.startChaptersActivity(novel, false); dialog.dismiss() }
            }
            confirmDialog!!.show()
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
