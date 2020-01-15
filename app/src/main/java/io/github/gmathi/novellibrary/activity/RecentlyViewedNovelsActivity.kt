package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.createOrUpdateLargePreference
import io.github.gmathi.novellibrary.database.getLargePreference
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.startNovelDetailsActivity
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_recently_viewed_novels.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_novel.view.*

class RecentlyViewedNovelsActivity : BaseActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recently_viewed_novels)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this)
        recyclerView.setDefaults(adapter)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        itemView.novelImageView.setImageResource(android.R.color.transparent)
        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                    .load(item.imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .into(itemView.novelImageView)
        }

        //Other Data Fields
        itemView.novelTitleTextView.text = item.name
        if (item.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemView.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("Library Activity", "Rating: " + item.rating, e)
            }
            itemView.novelRatingText.text = ratingText
        }
    }

    override fun onItemClick(item: Novel) {
        startNovelDetailsActivity(item)
    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recently_viewed_novels, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        if (item?.itemId == R.id.action_clear)
            MaterialDialog.Builder(this)
                    .content("Are you sure you want to clear all the recently viewed novels list?")
                    .positiveText("Yes")
                    .onPositive { dialog, _ ->
                        dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, "[]")
                        adapter.updateData(ArrayList())
                        dialog.dismiss()
                    }
                    .negativeText("No")
                    .onNegative { dialog, _ -> dialog.dismiss() }
                    .show()
        return super.onOptionsItemSelected(item)
    }
    //endregion

    override fun onResume() {
        super.onResume()
        val history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY) ?: "[]"
        val historyList: java.util.ArrayList<Novel> = Gson().fromJson(history, object : TypeToken<java.util.ArrayList<Novel>>() {}.type)
        adapter.updateData(ArrayList(historyList.asReversed()))
    }


}
