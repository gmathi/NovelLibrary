package io.github.gmathi.novellibrary.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.*
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.database.updateNewChapterCount
import io.github.gmathi.novellibrary.database.updateOrderId
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterCount
import io.github.gmathi.novellibrary.util.*
import kotlinx.android.synthetic.main.activity_library.*
import kotlinx.android.synthetic.main.content_library.*
import kotlinx.android.synthetic.main.listitem_library.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileOutputStream


class LibraryFragment : BaseFragment(), GenericAdapter.Listener<Novel>, SimpleItemTouchListener {

    lateinit var adapter: GenericAdapter<Novel>
    private lateinit var touchHelper: ItemTouchHelper
    private var lastDeletedId: Long = -1
    private var isSorted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.activity_library, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbar.title = getString(R.string.title_library)
        (activity as NavDrawerActivity).setToolbar(toolbar)
        setRecyclerView()
        progressLayout.showLoading()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(ArrayList(), R.layout.listitem_library, this)
        val callback = SimpleItemTouchHelperCallback(this)
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)
        recyclerView.setDefaults(adapter)
        swipeRefreshLayout.setOnRefreshListener {
            setData()
        }
    }

    private fun setData() {
        updateOrderIds()
        adapter.updateData(ArrayList(dbHelper.getAllNovels()))
        if (swipeRefreshLayout != null && progressLayout != null) {
            swipeRefreshLayout.isRefreshing = false
            progressLayout.showContent()
        }
    }


    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        if (lastDeletedId != item.id)
            startNovelDetailsActivity(item, false)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        itemView.novelImageView.setImageResource(android.R.color.transparent)
        if (item.imageFilePath != null) {
            itemView.novelImageView.setImageDrawable(Drawable.createFromPath(item.imageFilePath))
        }

        if (item.imageUrl != null) {
            val file = File(activity?.filesDir, Constants.IMAGES_DIR_NAME + "/" + Uri.parse(item.imageUrl).getFileName())
            if (file.exists())
                item.imageFilePath = file.path

            if (item.imageFilePath == null) {
                Glide.with(this).asBitmap().load(item.imageUrl).into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(bitmap: Bitmap?, transition: Transition<in Bitmap>?) {
                        //itemView.novelImageView.setImageBitmap(bitmap)
                        Thread(Runnable {
                            try {
                                val os = FileOutputStream(file)
                                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, os)
                                bitmap?.recycle()
                                item.imageFilePath = file.path
                                Handler(Looper.getMainLooper()).post {
                                    itemView.novelImageView.setImageDrawable(Drawable.createFromPath(item.imageFilePath))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }).start()
                    }
                })
            } else {
                itemView.novelImageView.setImageDrawable(Drawable.createFromPath(item.imageFilePath))
            }
        }

        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemView.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Log.w("Library Activity", "Rating: " + item.rating, e)
            }
            itemView.novelRatingText.text = ratingText
        }

        itemView.reorderButton.setOnTouchListener { _, event ->
            @Suppress("DEPRECATION")
            if (MotionEventCompat.getActionMasked(event) ==
                MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(recyclerView.getChildViewHolder(itemView))
            }
            false
        }

        itemView.readChapterImage.setOnClickListener {
            startReader(item)
        }


        if (item.chapterCount < item.newChapterCount) {
            val shape = GradientDrawable()
            shape.cornerRadius = 99f
            activity?.let { ContextCompat.getColor(it, R.color.Black) }?.let { shape.setStroke(1, it) }
            activity?.let { ContextCompat.getColor(it, R.color.DarkRed) }?.let { shape.setColor(it) }
            itemView.newChapterCount.background = shape
            itemView.newChapterCount.applyFont(activity?.assets).text = (item.newChapterCount - item.chapterCount).toString()
            itemView.newChapterCount.visibility = View.VISIBLE
        } else {
            itemView.newChapterCount.visibility = View.GONE
        }

        if (item.currentWebPageId != -1L) {
            val orderId = dbHelper.getWebPage(item.currentWebPageId)?.orderId
            if (orderId != null) {
                val progress = "${orderId + 1} / ${item.newChapterCount}"
                itemView.novelProgressText.text = progress
            }
        } else {
            itemView.novelProgressText.text = getString(R.string.no_bookmark)
        }
    }

    //endregion

    //region Sync Code
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_library, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (activity != null && statusCard != null)
            menu.getItem(0).isVisible = statusCard.visibility == View.GONE
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_sync -> {
                syncNovels()
                return true
            }
            R.id.action_sort -> {
                sortNovelsAlphabetically()
            }
            R.id.action_import_reading_list -> {
                activity?.startImportLibraryActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sortNovelsAlphabetically() {
        if (adapter.items.isNotEmpty()) {
            val items = adapter.items
            if (!isSorted)
                adapter.updateData(ArrayList(items.sortedWith(compareBy({ it.name }))))
            else
                adapter.updateData(ArrayList(items.sortedWith(compareBy({ it.name })).reversed()))
            isSorted = !isSorted
            updateOrderIds()
        }
    }

    private fun syncNovels() {
        //activity.startSyncService()
        async syncing@ {

            if (statusCard == null || activity == null) return@syncing

            statusCard.visibility = View.VISIBLE
            statusCard.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.alpha_animation))
            activity?.invalidateOptionsMenu()
            dbHelper.getAllNovels().forEach {
                try {
                    val totalChapters = await { NovelApi().getChapterCount(it.url) }
                    if (totalChapters != 0 && totalChapters > it.chapterCount.toInt() && totalChapters > it.newChapterCount.toInt()) {
                        dbHelper.updateNewChapterCount(it.id, totalChapters.toLong())
                        adapter.updateItem(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            setData()

            if (statusCard == null || activity == null) return@syncing
            statusCard.animation = null
            statusCard.visibility = View.GONE
            activity?.invalidateOptionsMenu()
        }
    }
    //endregion

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        adapter.updateData(ArrayList(dbHelper.getAllNovels()))
    }

    override fun onPause() {
        super.onPause()
        updateOrderIds()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        print(event.novelId)
    }

    private fun startReader(novel: Novel) {
        if (novel.currentWebPageId != -1L) {
            activity?.startReaderPagerDBActivity(novel)
        } else {
            val confirmDialog = activity?.let {
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

    private fun startNovelDetailsActivity(novel: Novel, jumpToReader: Boolean) {
        val intent = Intent(activity, NovelDetailsActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("novel", novel)
        if (jumpToReader)
            bundle.putBoolean(Constants.JUMP, true)
        intent.putExtras(bundle)
        activity?.startActivityForResult(intent, Constants.NOVEL_DETAILS_REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.NOVEL_DETAILS_RES_CODE) {
            val novelId = data?.extras?.getLong(Constants.NOVEL_ID)
            if (novelId != null) {
                lastDeletedId = novelId
                adapter.removeItemAt(adapter.items.indexOfFirst { it.id == novelId })
                Handler().postDelayed({ lastDeletedId = -1 }, 1200)
            }
            return
        } else if (requestCode == Constants.READER_ACT_REQ_CODE || requestCode == Constants.NOVEL_DETAILS_RES_CODE) {
            setData()
        }
    }

    override fun onItemDismiss(viewHolderPosition: Int) {
        activity?.let {
            MaterialDialog.Builder(it)
                .title(getString(R.string.confirm_remove))
                .content(getString(R.string.confirm_remove_description))
                .positiveText(R.string.remove)
                .negativeText(R.string.cancel)
                .onPositive { dialog, _ ->
                    run {
                        val novel = adapter.items[viewHolderPosition]
                        Utils.deleteNovel(it, novel.id)
                        adapter.onItemDismiss(viewHolderPosition)
                        dialog.dismiss()
                    }
                }
                .onNegative { dialog, _ ->
                    run {
                        adapter.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
                .show()
        }
    }

    override fun onItemMove(source: Int, target: Int) {
        adapter.onItemMove(source, target)
    }

    private fun updateOrderIds() {
        if (adapter.items.isNotEmpty())
            for (i in 0 until adapter.items.size) {
                dbHelper.updateOrderId(adapter.items[i].id, i.toLong())
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

}
