package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.createOrUpdateLargePreference
import io.github.gmathi.novellibrary.database.getLargePreference
import io.github.gmathi.novellibrary.databinding.ContentRecyclerViewBinding
import io.github.gmathi.novellibrary.databinding.ListitemNovelBinding
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.util.view.setDefaults

class RecentlyViewedNovelsFragment : BaseFragment(), GenericAdapter.Listener<Novel> {

    companion object {
        fun newInstance() = RecentlyViewedNovelsFragment()
    }

    private lateinit var binding: ContentRecyclerViewBinding
    private lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.content_recycler_view, container, false)
        binding = ContentRecyclerViewBinding.bind(view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList(), layoutResId = R.layout.listitem_novel, listener = this)
        binding.recyclerView.setDefaults(adapter)
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

        if (item.metadata.containsKey("OriginMarker")) {
            itemBinding.novelLanguageText.text = item.metadata["OriginMarker"]
            itemBinding.novelLanguageText.visibility = View.VISIBLE
        }

        if (!item.rating.isNullOrBlank()) {
            var ratingText = "(N/A)"
            try {
                val rating = item.rating!!.replace(",",".").toFloat()
                itemBinding.novelRatingBar.rating = rating
                ratingText = "(" + String.format("%.1f", rating) + ")"
            } catch (e: Exception) {
                Logs.warning("NovelDetailsActivity", "Rating: ${item.rating}, Novel: ${item.name}", e)
            }
            itemBinding.novelRatingText.text = ratingText
        }
    }

    override fun onItemClick(item: Novel, position: Int) {
        requireActivity().startNovelDetailsActivity(item)
    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_recently_viewed_novels, menu)
        return super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear)
            MaterialDialog(requireActivity()).show {
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