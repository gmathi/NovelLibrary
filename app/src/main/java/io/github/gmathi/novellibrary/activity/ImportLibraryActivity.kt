package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
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
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.insertNovel
import io.github.gmathi.novellibrary.databinding.ActivityImportLibraryBinding
import io.github.gmathi.novellibrary.databinding.ListitemImportListBinding
import io.github.gmathi.novellibrary.extensions.showEmpty
import io.github.gmathi.novellibrary.extensions.showError
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.ImportListItem
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions.NETWORK_ERROR
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean


class ImportLibraryActivity : BaseActivity(), GenericAdapter.Listener<ImportListItem>, ActionMode.Callback {

    lateinit var adapter: GenericAdapter<ImportListItem>

    private val network: NetworkHelper by injectLazy()
    private val client: OkHttpClient
        get() = network.cloudflareClient

    private var updateSet: HashSet<ImportListItem> = HashSet()
    private var actionMode: ActionMode? = null
    private var job: Job? = null
    private val isImporting: AtomicBoolean = AtomicBoolean(false)

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
        }

// Test Only
//        binding.contentImportLibrary.readingListUrlEditText.setText("https://www.novelupdates.com/user/87290/goa_naidu2010/?rl=1")
//        getNovelsFromUrl()
//        adapter.notifyDataSetChanged()

        binding.contentImportLibrary.importCardButton.setOnClickListener {
            getNovelsFromUrl()
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
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_import_list, listener = this)
        binding.contentImportLibrary.recyclerView.setDefaultsNoAnimation(adapter)
        binding.contentImportLibrary.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentImportLibrary.progressLayout.showEmpty(resId = R.raw.no_data_blob, emptyText = "Add a URL to see your reading list here", isLottieAnimation = true)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun getNovelsFromUrl() {
        lifecycleScope.launch {
            binding.contentImportLibrary.progressLayout.showLoading()
            try {
                var responseString = withContext(Dispatchers.IO) { getNovelListResponse() } ?: throw Exception(NETWORK_ERROR)
                responseString = responseString.replace("\\\"", "\"")
                    .replace("\\n", "")
                    .replace("\\t", "")
                    .replace("\\/", "/")

                val doc: Document = Jsoup.parse(responseString)
                val novels = doc.body().select("a.mb-box-btn") ?: throw Exception(NETWORK_ERROR)
                if (novels.isNotEmpty()) {
                    adapter.removeAllItems()
                    val importListItems = ArrayList<ImportListItem>()
                    novels.mapTo(importListItems) {
                        val importItem = ImportListItem(it.attr("href"))
                        importItem.novelName = it.getElementsByClass("title")?.firstOrNull()?.text()
                        val styleAttr = it.getElementsByClass("icon-thumb")?.firstOrNull()?.attr("style")
                        if (styleAttr != null && styleAttr.length > 26)
                            importItem.novelImageUrl = styleAttr.substring(22, styleAttr.length - 3)
                        importItem.currentlyReadingChapterName = it.getElementsByClass("cr_status")?.firstOrNull()?.text()
                        importItem.currentlyReading = it.getElementsByClass("cr_status")?.firstOrNull()?.parent()?.text()
                        importItem.isAlreadyInLibrary = dbHelper.getNovelByUrl(importItem.novelUrl) != null
                        importItem
                    }
                    adapter.addItems(importListItems)
                    binding.contentImportLibrary.progressLayout.showContent()
                    binding.contentImportLibrary.headerLayout.visibility = View.VISIBLE
                } else {
                    binding.contentImportLibrary.progressLayout.showError(errorText = "No Novels found!", buttonText = getString(R.string.try_again)) {
                        getNovelsFromUrl()
                    }
                }
            } catch (e: Exception) {
                //Do Nothing
                toast(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    private fun getNovelListResponse(): String? {
        val url = getUrl() ?: return null
        val userId = getUserIdFromUrl(url)
        val adminUrl = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nu_prevew")
            .add("pagenum", "0")
            .add("intUserID", userId)
            .add("isMobile", "yes")
            .build()
        val request = POST(adminUrl, body = formBody)
        return client.newCall(request).safeExecute().body?.string()
    }

    private fun getUserIdFromUrl(urlString: String): String {
        val url = URL(urlString)
        return url.path.split("/")[2]
    }

    private fun getUrl(): String? {
        val url = binding.contentImportLibrary.readingListUrlEditText.text?.toString() ?: return null
        return try {
            val uri = Uri.parse(url) ?: return null
            if (uri.scheme!!.startsWith("http") && uri.host!!.contains(HostNames.NOVEL_UPDATES))
                url
            else
                null
        } catch (e: Exception) {
            toast(e.localizedMessage ?: "Error parsing url!")
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
        adapter.notifyItemRangeChanged(0, adapter.items.size)
    }

    private fun clearSelection() {
        adapter.items.forEach {
            removeFromUpdateSet(it)
        }
        adapter.notifyItemRangeChanged(0, adapter.items.size)
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onDestroyActionMode(mode: ActionMode?) {
        updateSet.clear()
        actionMode = null
        adapter.notifyDataSetChanged()
    }

//endregion


    @SuppressLint("NotifyDataSetChanged")
    private fun startImport() {
        if (isImporting.get()) {
            return
        }

        isImporting.set(true)

        val snackProgressBarManager = Utils.createSnackProgressBarManager(findViewById(android.R.id.content), this)
        val snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, "Importing… - " + getString(R.string.please_wait))
            .setAction(getString(R.string.cancel), object : SnackProgressBar.OnActionClickListener {
                @SuppressLint("NotifyDataSetChanged")
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
            isImporting.set(false)
        }
    }

    private suspend fun importNovelToLibrary(importListItem: ImportListItem) {
        val novel = Novel(importListItem.novelName, importListItem.novelUrl, Constants.SourceId.NOVEL_UPDATES)
        sourceManager.get(Constants.SourceId.NOVEL_UPDATES)?.getNovelDetails(novel)?.let { dbHelper.insertNovel(it) }
    }

    override fun onBackPressed() {
        if (isImporting.get()) {
            return
        }

        super.onBackPressed()
    }
}


