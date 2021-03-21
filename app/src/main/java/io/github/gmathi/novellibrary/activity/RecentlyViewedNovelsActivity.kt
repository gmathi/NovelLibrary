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
import io.github.gmathi.novellibrary.databinding.ActivityRecentlyViewedNovelsBinding
import io.github.gmathi.novellibrary.databinding.ListitemNovelBinding
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.setDefaults
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity

class RecentlyViewedNovelsActivity : BaseActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    companion object {
        private const val TAG = "RecentlyViewedNovelsActivity"
    }

    private lateinit var binding: ActivityRecentlyViewedNovelsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecentlyViewedNovelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
    }

    override fun bind(item: Novel, itemView: View, position: Int) {
        val itemBinding = ListitemNovelBinding.bind(itemView)
        itemBinding.novelImageView.setImageResource(android.R.color.transparent)
        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(item.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(itemBinding.novelImageView)
        }

        //Other Data Fields
        itemBinding.novelTitleTextView.text = item.name
        itemBinding.novelTitleTextView.isSelected = dataCenter.enableScrollingText

        if (item.rating != null) {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.toFloat()
                itemBinding.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning(TAG, "Rating: " + item.rating, e)
            }
            itemBinding.novelRatingText.text = ratingText
        }
    }

    override fun onItemClick(item: Novel, position: Int) {
        startNovelDetailsActivity(item)
    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recently_viewed_novels, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == R.id.action_clear)
            MaterialDialog(this).show {
                message(text = "Are you sure you want to clear all the recently viewed novels list?")
                positiveButton(text = "Yes") { dialog ->
                    dbHelper.createOrUpdateLargePreference(
                        Constants.LargePreferenceKeys.RVN_HISTORY,
                        "[]"
                    )
                    adapter.updateData(ArrayList())
                    dialog.dismiss()
                }
                negativeButton(R.string.cancel) {
                    it.dismiss()
                }
            }
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
