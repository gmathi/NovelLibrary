package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.insertNovel
import io.github.gmathi.novellibrary.databinding.ActivityImportLibraryBinding
import io.github.gmathi.novellibrary.databinding.ListitemImportListBinding
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.showEmpty
import io.github.gmathi.novellibrary.extensions.showError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.other.ImportListItem
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getNUNovelDetails
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URL


class ImportLibraryActivity : BaseActivity(), GenericAdapter.Listener<ImportListItem>, ActionMode.Callback {

    lateinit var adapter: GenericAdapter<ImportListItem>
    private var importList = ArrayList<ImportListItem>()
    private var updateSet: HashSet<ImportListItem> = HashSet()
    private var actionMode: ActionMode? = null
    private var job: Job? = null

    private lateinit var binding: ActivityImportLibraryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityImportLibraryBinding.inflate(layoutInflater)
        
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()


        //From Browser or any other Application which is sending the url for reading list
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
            val url = if (intent.action == Intent.ACTION_VIEW) intent.data!!.toString()
            else intent.getStringExtra(Intent.EXTRA_TEXT)
            binding.contentImportLibrary.readingListUrlEditText.setText(url)
            getNovelsFromUrl()
            adapter.notifyDataSetChanged()
        }

//        readingListUrlEditText.setText("https://www.novelupdates.com/user/87290/goa_naidu2010/?rl=1")
//        getNovelsFromUrl()
//        adapter.notifyDataSetChanged()

        binding.contentImportLibrary.importCardButton.setOnClickListener {
            getNovelsFromUrl()
            adapter.notifyDataSetChanged()
        }

        binding.contentImportLibrary.headerLayout.visibility = View.GONE

        binding.contentImportLibrary.headerLayout.setOnClickListener {
            if (binding.contentImportLibrary.importUrlLayout.visibility == View.GONE) {
                binding.contentImportLibrary.importUrlLayout.visibility = View.VISIBLE
                binding.contentImportLibrary.upButton.setImageDrawable(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_arrow_drop_up_white_vector))
            } else {
                binding.contentImportLibrary.importUrlLayout.visibility = View.GONE
                binding.contentImportLibrary.upButton.setImageDrawable(ContextCompat.getDrawable(this@ImportLibraryActivity, R.drawable.ic_arrow_drop_down_white_vector))
            }
        }
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = importList, layoutResId = R.layout.listitem_import_list, listener = this)
        binding.contentImportLibrary.recyclerView.setDefaultsNoAnimation(adapter)
        binding.contentImportLibrary.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentImportLibrary.progressLayout.showEmpty(resId = R.raw.no_data_blob, emptyText = "Add a URL to see your reading list here", isLottieAnimation = true)
    }

    private fun getNovelsFromUrl() {
        lifecycleScope.launch {
            try {
                binding.contentImportLibrary.progressLayout.showLoading()
                val url = getUrl() ?: return@launch
                val userId = getUserIdFromUrl(url)
                val adminUrl = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
                val formData: HashMap<String, String> = hashMapOf(
                    "action" to "nu_prevew",
                    "pagenum" to "0",
                    "intUserID" to userId,
                    "isMobile" to "yes"
                )
                var body = withContext(Dispatchers.IO) { NovelApi.getStringWithFormData(adminUrl, formData) }
                body = body.replace("\\\"", "\"")
                    .replace("\\n", "")
                    .replace("\\t", "")
                    .replace("\\/", "/")

                val doc: Document = Jsoup.parse(body)
                val novels = doc.body().select("a.mb-box-btn")
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
                        importItem.isAlreadyInLibrary = dbHelper.getNovelByUrl(importItem.novelUrl!!) != null
                        importItem
                    }
                    binding.contentImportLibrary.progressLayout.showContent()
                    binding.contentImportLibrary.headerLayout.visibility = View.VISIBLE
                } else {
                    binding.contentImportLibrary.progressLayout.showError(errorText = "No Novels found!", buttonText = getString(R.string.try_again), onClickListener = {
                        getNovelsFromUrl()
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getUserIdFromUrl(urlString: String): String {
        val url = URL(urlString)
        return url.path.split("/")[2]
    }

    private fun getUrl(): String? {
        val url = binding.contentImportLibrary.readingListUrlEditText?.text?.toString() ?: return null
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
        val binding = ListitemImportListBinding.bind(itemView)
        binding.title.applyFont(assets).text = item.novelName
        binding.subtitle.applyFont(assets).text = item.currentlyReading

        binding.checkbox.setOnCheckedChangeListener(null)
        binding.checkbox.isChecked = updateSet.contains(item)
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                addToUpdateSet(item)
            else
                removeFromUpdateSet(item)
        }

        if (item.isAlreadyInLibrary) {
            binding.checkbox.visibility = View.GONE
            binding.title.setTextColor(ContextCompat.getColor(this@ImportLibraryActivity, R.color.Lime))
            binding.subtitle.applyFont(assets).text = getString(R.string.already_in_library)
        } else {
            binding.title.setTextColor(ContextCompat.getColor(this@ImportLibraryActivity, R.color.White))
            binding.checkbox.visibility = View.VISIBLE
        }

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: ImportListItem, position: Int) {
        if (updateSet.contains(item))
            removeFromUpdateSet(item)
        else
            addToUpdateSet(item)
        adapter.updateItem(item)
    }

//region ActionMode Callback

    private fun selectAll() {
        adapter.items.forEach {
            if (!it.isAlreadyInLibrary)
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
        val snackProgressBarManager = Utils.createSnackProgressBarManager(findViewById(android.R.id.content), this)
        val snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "Importing… - " + getString(R.string.please_wait))
            .setAction(getString(R.string.cancel), object : SnackProgressBar.OnActionClickListener {
                override fun onActionClick() {
                    run {
                        this@ImportLibraryActivity.job?.cancel()
                        this@ImportLibraryActivity.actionMode?.finish()
                        this@ImportLibraryActivity.adapter.notifyDataSetChanged()
                        snackProgressBarManager.disable()
                    }
                }
            })
            .setProgressMax(updateSet.count())
        snackProgressBarManager.show(snackProgressBar, SnackProgressBarManager.LENGTH_INDEFINITE)

        job = lifecycleScope.launch {
            var progressCnt: Int = 0
            updateSet.asSequence().forEach {
                snackProgressBarManager.updateTo(snackProgressBar.setMessage("Importing: ${it.novelName}"))
                withContext(Dispatchers.IO) { importNovelToLibrary(it) }
                it.isAlreadyInLibrary = true
                snackProgressBarManager.setProgress(++progressCnt)
            }
            actionMode?.finish()
            adapter.notifyDataSetChanged()
            snackProgressBarManager.disable()
        }
    }

    private fun importNovelToLibrary(importListItem: ImportListItem) {
        val novel = NovelApi.getNUNovelDetails(importListItem.novelUrl!!) ?: return
        novel.id = dbHelper.insertNovel(novel)
    }

}


