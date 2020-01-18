package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.createWebPage
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.insertNovel
import io.github.gmathi.novellibrary.database.updateBookmarkCurrentWebPageUrl
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.ImportListItem
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.network.getNUNovelDetails
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_import_library.*
import kotlinx.android.synthetic.main.content_import_library.*
import kotlinx.android.synthetic.main.listitem_import_list.view.*


class ImportLibraryActivity : BaseActivity(), GenericAdapter.Listener<ImportListItem>, ActionMode.Callback {

    lateinit var adapter: GenericAdapter<ImportListItem>

    private var importList = ArrayList<ImportListItem>()
    private var updateSet: HashSet<ImportListItem> = HashSet()
    private var actionMode: ActionMode? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_library)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()

        //From Browser or any other Application which is sending the url for reading list
        if (Intent.ACTION_VIEW == intent.action) {
            val url = intent.data!!.toString()
            readingListUrlEditText.setText(url)
            getNovelsFromUrl()
            adapter.notifyDataSetChanged()
        } else

            if (Intent.ACTION_SEND == intent.action) {
                val url = intent.getStringExtra(Intent.EXTRA_TEXT)
                readingListUrlEditText.setText(url)
                getNovelsFromUrl()
                adapter.notifyDataSetChanged()
            }


//        readingListUrlEditText.setText("http://www.novelupdates.com/readlist/?uname=swordman009")
//        getNovelsFromUrl()
//        adapter.notifyDataSetChanged()

        importCardButton.setOnClickListener {
            getNovelsFromUrl()
            adapter.notifyDataSetChanged()
        }

        headerLayout.visibility = View.GONE

        headerLayout.setOnClickListener {
            if (importUrlLayout.visibility == View.GONE) {
                importUrlLayout.visibility = View.VISIBLE
                upButton.setImageDrawable(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_arrow_drop_up_white_vector))
            } else {
                importUrlLayout.visibility = View.GONE
                upButton.setImageDrawable(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_arrow_drop_down_white_vector))
            }
        }

    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = importList, layoutResId = R.layout.listitem_import_list, listener = this)
        recyclerView.setDefaultsNoAnimation(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        progressLayout.showEmpty(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_arrow_upward_white_vector), "Add a URL to see your reading list here")
    }

    private fun getNovelsFromUrl() {
        async {
            try {
                val url = getUrl() ?: return@async
                progressLayout.showLoading()
                val doc = await { NovelApi.getDocumentWithUserAgent(url) }
                val novels = doc.body()?.getElementsByClass("mb-box-btn")?.filter { it.tagName() == "a" }
                if (novels != null && novels.isNotEmpty()) {
                    importList.clear()
                    novels.mapTo(importList) {
                        val importItem = ImportListItem()
                        importItem.novelName = it.getElementsByClass("title")?.firstOrNull()?.text()
                        importItem.novelUrl = it.attr("href")
                        val styleAttr = it.getElementsByClass("icon-thumb")?.firstOrNull()?.attr("style")
                        if (styleAttr != null && styleAttr.length > 26)
                            importItem.novelImageUrl = styleAttr.substring(22, styleAttr.length - 3)
                        importItem.currentlyReadingChapterName = it.getElementsByClass("cr_status")?.firstOrNull()?.text()
                        importItem.currentlyReading = it.getElementsByClass("cr_status")?.firstOrNull()?.parent()?.text()

                        importItem
                    }
                    progressLayout.showContent()
                    headerLayout.visibility = View.VISIBLE
                } else {
                    progressLayout.showError(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_warning_white_vector), "No Novels found!", getString(R.string.try_again)) { getNovelsFromUrl() }
                }
            } catch (e: Exception) {

            }
        }
    }

    private fun getUrl(): String? {
        val url = readingListUrlEditText?.text?.toString() ?: return null
        return try {
            val uri = Uri.parse(url) ?: return null
            if (uri.scheme!!.startsWith("http") && uri.host!!.contains(HostNames.NOVEL_UPDATES))
                url
            else
                null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override fun bind(item: ImportListItem, itemView: View, position: Int) {
        itemView.title.applyFont(assets).text = item.novelName
        itemView.subtitle.applyFont(assets).text = item.currentlyReading

        itemView.checkbox.setOnCheckedChangeListener(null)
        itemView.checkbox.isChecked = updateSet.contains(item)
        itemView.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                addToUpdateSet(item)
            else
                removeFromUpdateSet(item)
        }

        if (dbHelper.getNovelByUrl(item.novelUrl!!) != null) {
            itemView.checkbox.visibility = View.GONE
            itemView.title.setTextColor(ContextCompat.getColor(this@ImportLibraryActivity, R.color.Lime))
            itemView.subtitle.applyFont(assets).text = getString(R.string.already_in_library)
        } else {
            itemView.title.setTextColor(ContextCompat.getColor(this@ImportLibraryActivity, R.color.White))
            itemView.checkbox.visibility = View.VISIBLE
        }

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: ImportListItem) {
        if (updateSet.contains(item))
            removeFromUpdateSet(item)
        else
            addToUpdateSet(item)
        adapter.updateItem(item)
    }

    //region ActionMode Callback

    private fun selectAll() {
        adapter.items.forEach {
            addToUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun clearSelection() {
        adapter.items.forEach {
            removeFromUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    private fun addToUpdateSet(importListItem: ImportListItem) {
        updateSet.add(importListItem)
        if (updateSet.isNotEmpty() && actionMode == null) {
            actionMode = startSupportActionMode(this)
        }
        actionMode?.title = getString(R.string.chapters_selected, updateSet.size)
    }

    private fun removeFromUpdateSet(importListItem: ImportListItem) {
        updateSet.remove(importListItem)
        actionMode?.title = getString(R.string.chapters_selected, updateSet.size)
        if (updateSet.isEmpty()) {
            actionMode?.finish()
        }
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {

            R.id.action_add_to_library -> {
                startImport()
            }
            R.id.action_select_all -> {
                selectAll()
            }
            R.id.action_clear_selection -> {
                clearSelection()
            }

        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.menu_import_library_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        updateSet.clear()
        actionMode = null
        adapter.notifyDataSetChanged()
    }

    //endregion


    private fun startImport() {
        val dialog = MaterialDialog.Builder(this)
                .title("Importing…")
                .content(R.string.please_wait)
                .progress(false, updateSet.size, true)
                .negativeText(R.string.cancel)
                .cancelable(false)
                .autoDismiss(false)
                .onNegative { dialog, _ ->
                    run {
                        async.cancelAll()
                        actionMode?.finish()
                        dialog.dismiss()
                    }
                }
                .show()
        async {
            updateSet.asSequence().forEach {
                dialog.setContent("Importing: ${it.novelName}")
                await { importNovelToLibrary(it) }
                dialog.incrementProgress(1)
            }
            actionMode?.finish()
            dialog.dismiss()
        }
    }

    private fun importNovelToLibrary(importListItem: ImportListItem) {
        val novel = NovelApi.getNUNovelDetails(importListItem.novelUrl!!) ?: return
        novel.id = dbHelper.insertNovel(novel)
        val webPages = NovelApi.getChapterUrls(novel)
        webPages?.forEach {
            dbHelper.createWebPage(it)
        }
        val currentReaderWebPage = webPages?.firstOrNull { it.chapter == importListItem.currentlyReadingChapterName }
        currentReaderWebPage?.let {
            dbHelper.updateBookmarkCurrentWebPageUrl(novel.id, it.url)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }
}
