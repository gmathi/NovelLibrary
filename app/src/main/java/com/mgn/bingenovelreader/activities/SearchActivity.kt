package com.mgn.bingenovelreader.activities

import android.animation.Animator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.*
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.network.NovelApi
import com.mgn.bingenovelreader.utils.SimpleAnimationListener
import com.mgn.bingenovelreader.utils.SuggestionsBuilder
import com.mgn.bingenovelreader.utils.addToSearchHistory
import com.mgn.bingenovelreader.utils.toast
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.android.synthetic.main.content_search.*
import kotlinx.android.synthetic.main.listitem_novel.view.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class SearchActivity : AppCompatActivity(), GenericAdapter.Listener<Novel> {

    lateinit var adapter: GenericAdapter<Novel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setRecyclerView()
        setSearchView()

        loadingImageView.visibility = View.INVISIBLE
        Glide.with(this).load("https://media.giphy.com/media/ADyQEh474eu0o/giphy.gif").into(loadingImageView)
        searchNovels("Realms")
    }

    private fun setRecyclerView() {
        val llm = LinearLayoutManager(applicationContext)
        val set = AnimationSet(true)
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = 500
        set.addAnimation(animation)
        animation = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
        )
        animation.setDuration(100)
        set.addAnimation(animation)
        val controller = LayoutAnimationController(set, 0.5f)

        adapter = GenericAdapter(items = ArrayList<Novel>(), layoutResId = R.layout.listitem_novel, listener = this)

        searchRecyclerView.setHasFixedSize(true)
        searchRecyclerView.layoutManager = llm
        searchRecyclerView.layoutAnimation = controller
        searchRecyclerView.adapter = adapter
    }

    private fun setSearchView() {
        searchView.setHomeButtonListener { finish() }
        searchView.setSuggestionBuilder(SuggestionsBuilder())
        searchView.setSearchListener(object : PersistentSearchView.SearchListener {

            override fun onSearch(searchTerm: String?) {
                searchTerm?.addToSearchHistory()
                searchNovels(searchTerm)
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

            override fun onSearchExit() {}
            override fun onSearchCleared() {}
            override fun onSearchTermChanged(searchTerm: String?) {}
            override fun onSearchEditBackPressed(): Boolean = false
        })

    }

    fun searchNovels(searchTerm: String?) {
        searchRecyclerView.visibility = View.INVISIBLE
        loadingImageView.visibility = View.VISIBLE

        Thread(Runnable {
            searchTerm?.let {
                val newList = ArrayList(NovelApi().search(it)["novel-updates"])
                Handler(Looper.getMainLooper()).post {
                    loadingImageView.visibility = View.GONE
                    searchRecyclerView.visibility = View.VISIBLE
                    adapter.updateData(newList)
                }
            }
        }).start()
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: Novel) {
        toast("${item.name} Clicked")
    }

    override fun bind(item: Novel, itemView: View) {
        Glide.with(this).load(item.imageUrl).into(itemView.novelImageView)
        itemView.novelTitleTextView.text = item.name
        itemView.novelRatingBar.rating = item.rating.toFloat()
        itemView.novelRatingTextView.text = "(" + item.rating + ")"
        itemView.novelGenreTextView.text = item.genres?.joinToString { it }
        itemView.novelDescriptionTextView.text = item.shortDescription
    }

    //endregion

    override fun onBackPressed() {
        if (searchView.isEditing) {
            searchView.cancelEditing()
        } else {
            super.onBackPressed()
        }
    }

}
